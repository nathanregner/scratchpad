(ns scratchpad.partition
  (:require
   [clojure.string :as str]
   [next.jdbc :as jdbc]
   [parallel.core :as p]
   [scratchpad.keyring :as keyring])
  (:import
   [java.time LocalDate LocalDateTime]
   [java.time.format DateTimeFormatter]
   [java.time.temporal ChronoUnit]))

(defn create-partitions
  [^LocalDate from months]
  (let [format (DateTimeFormatter/ofPattern "yyyy_MM")]
    (->> (range months)
         (map #(let [from (-> from (.plusMonths %))
                     to (-> from (.plusMonths 1))]
                 (str "CREATE TABLE feed_batch_" (.format format from) " PARTITION OF feed_batch FOR VALUES FROM ('" from "') TO ('" to "');")))
         (str/join "\n"))))

(defn drop-partitions
  [^LocalDate from months]
  (let [format (DateTimeFormatter/ofPattern "yyyy_MM")]
    (->> (range months)
         (map #(let [from (-> from (.plusMonths %))]
                 (str "DROP TABLE feed_batch_" (.format format from) ";")))
         (str/join "\n"))))

(defn copy
  [body]
  (let [body (str body)]
    (-> (java.awt.Toolkit/getDefaultToolkit)
        .getSystemClipboard
        (.setContents (java.awt.datatransfer.StringSelection. body) nil))
    body))

(comment
  (copy (create-partitions (LocalDate/parse "2023-01-01") 12))
  (copy (drop-partitions (LocalDate/parse "2023-01-01") 12)))

(defn hours
  [^LocalDateTime start ^LocalDateTime end]
  (for [start (iterate #(.plus %1 1 ChronoUnit/HOURS) start)
        :while (<= (.compareTo start end) 0)]
    [start (.plus start 1 ChronoUnit/HOURS)]))

(defn days
  [^LocalDate start ^LocalDate end]
  (for [start (iterate #(.plus %1 1 ChronoUnit/DAYS) start)
        :while (< (.compareTo start end) 0)]
    [start (.plus start 1 ChronoUnit/DAYS)]))

(def ds
  (jdbc/get-datasource {:dbtype "postgresql"
                        :dbname "arbitrage_bot"
                        :host "sagittarius"
                        :port 30432
                        :user "postgres"
                        :password (keyring/get "postgres")}))

(defn migrate
  [[from to]]
  (print (str "Migrating " from " to " to "\n"))
  (jdbc/execute! ds ["INSERT INTO feed_batch (feed_id, fetched, processed, index, pages, requested_count, max_published, listing_count)
                      SELECT feed_id, fetched, processed, index, pages, requested_count, max_published, listing_count
                      FROM feed_batch_old
                      WHERE fetched BETWEEN ? AND ?
                      ON CONFLICT DO NOTHING;" from to]))

(defn stripe
  [n coll]
  (mapcat
   #(take-nth n (drop % coll))
   (range n)))

(comment
  ; (set-agent-send-executor! (Executors/newVirtualThreadPerTaskExecutor))
  (jdbc/execute! ds ["SELECT 1"])
  ; (stripe 10 (stripe 2 (range 20)))
  #_(hours (LocalDateTime/parse "2023-12-20T00:00:00")
           (LocalDateTime/parse "2024-01-01T00:00:00"))
  (->> (days (LocalDate/parse "2023-12-01")
             (LocalDate/parse "2024-12-31"))
       ; (stripe 24)
       (#(p/pmap (comp migrate) % 32))
       (count)))
