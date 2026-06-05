(ns cursor-schema
  (:require [babashka.fs :as fs]
            [babashka.process :as p]
            [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.string :as string]))

(def schema-dir "schemas/cursor")
(def node-modules (str schema-dir "/node_modules"))

(defn- ensure-deps!
  "Installs ajv dependencies when node_modules is missing."
  []
  (when-not (fs/exists? node-modules)
    (println "Installing Cursor schema validator dependencies...")
    (p/shell {:dir schema-dir} "npm" "install")))

(defn validate-json!
  "Validates data (map or path) against a JSON Schema file. Returns error strings."
  [schema-path data]
  (ensure-deps!)
  (let [data-path (if (string? data)
                    data
                    (let [tmp (str (fs/create-temp-file {:prefix "cursor-schema-" :suffix ".json"}))]
                      (spit tmp (json/generate-string data))
                      tmp))
        result (try
                 (p/shell {:dir "."
                           :out :string
                           :err :string}
                          "node"
                          (str schema-dir "/validate.mjs")
                          "-s" schema-path
                          "-d" data-path)
                 (catch Exception e
                   (let [data (:data (ex-data e))]
                     {:out (or (:out data) "")
                      :err (or (:err data) (.getMessage e))
                      :exit (or (:exit data) 1)})))]
    (if (zero? (:exit result))
      []
      (->> (string/split-lines (str (:out result) "\n" (:err result)))
           (remove string/blank?)
           (map string/trim)
           vec))))

(defn validate-plugin-json!
  [data]
  (validate-json! (str schema-dir "/plugin.schema.json") data))

(defn validate-marketplace-json!
  [data]
  (validate-json! (str schema-dir "/marketplace.schema.json") data))

(defn spike-fixtures!
  "Runs fixture smoke tests. Returns error strings."
  []
  (concat
    (when (seq (validate-json! (str schema-dir "/plugin.schema.json")
                               (str schema-dir "/fixtures/valid-plugin.json")))
      ["valid-plugin.json should pass"])
    (when (empty? (validate-json! (str schema-dir "/plugin.schema.json")
                                 (str schema-dir "/fixtures/invalid-extra-key.json")))
      ["invalid-extra-key.json should fail"])
    (when (empty? (validate-json! (str schema-dir "/plugin.schema.json")
                                 (str schema-dir "/fixtures/invalid-name.json")))
      ["invalid-name.json should fail"])
    (when (empty? (validate-json! (str schema-dir "/marketplace.schema.json")
                                 (str schema-dir "/fixtures/invalid-marketplace-version.json")))
      ["invalid-marketplace-version.json should fail"])))

(defn spike!
  []
  (let [errors (spike-fixtures!)]
    (if (seq errors)
      (do
        (println "Cursor schema spike failed:")
        (doseq [e errors] (println (str "  - " e)))
        (System/exit 1))
      (do
        (println "Cursor schema spike passed.")
        (let [clojure-pj "plugins/clojure/.github/plugin/plugin.json"
              schema-errors (validate-plugin-json! (json/parse-string (slurp clojure-pj) true))]
          (if (seq schema-errors)
            (do
              (println "plugins/clojure plugin.json schema errors:")
              (doseq [e schema-errors] (println (str "  - " e)))
              (System/exit 1))
            (println "plugins/clojure/.github/plugin/plugin.json passes plugin.schema.json.")))))))
