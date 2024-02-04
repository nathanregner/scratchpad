(ns scratchpad.keyring
  (:import
   [com.github.javakeyring Keyring]))

(def domain
  "net.nregner.scratchpad")

(defn get
  [key]
  (with-open [keyring (Keyring/create)]
    (.getPassword keyring domain key)))

(defn set
  [key password]
  (with-open [keyring (Keyring/create)]
    (.setPassword keyring domain key password)))

(defn delete
  [key]
  (with-open [keyring (Keyring/create)]
    (.deletePassword keyring domain key)))

(comment
  (set "test" "test")
  (get "test")
  (delete "test"))
