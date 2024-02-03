(ns scratchpad.partition
  (:require [clojure.string :as str])
  (:import [java.time LocalDate]
           [java.time.format DateTimeFormatter]))

; CREATE TABLE feed_batch_2024_01 PARTITION OF feed_batch_partitioned FOR VALUES FROM ('2024-01-01') TO ('2024-02-01');

(defn create-partitions
  [^LocalDate from months]
  (let [format (DateTimeFormatter/ofPattern "yyyy_MM")]
    (->> (range months)
         (map #(let [from (-> from (.plusMonths %))
                     to (-> from (.plusMonths 1))]
                 (str "CREATE TABLE feed_batch_" (.format format from) " PARTITION OF feed_batch_partitioned FOR VALUES FROM ('" from "') TO ('" to "');")))
         (str/join "\n"))))

(defn copy
  [body]
  (let [body (str body)]
    (-> (java.awt.Toolkit/getDefaultToolkit)
        .getSystemClipboard
        (.setContents (java.awt.datatransfer.StringSelection. body) nil))
    body))

(comment
  (copy (create-partitions (LocalDate/parse "2023-01-01") 12)))
