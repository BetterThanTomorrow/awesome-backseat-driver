(ns validate
  (:require [babashka.fs :as fs]
            [cheshire.core :as json]
            [clojure.string :as string]))

(def required-plugin-fields [:name :version :description])

(defn- read-json
  "Reads and parses a JSON file. Returns {:ok data} or {:error message}."
  [path]
  (try
    {:ok (json/parse-string (slurp (str path)) true)}
    (catch Exception e
      {:error (str "Failed to parse " path ": " (.getMessage e))})))

(defn- validate-plugin-json
  "Validates a single plugin.json has required fields and referenced paths exist."
  [plugin-dir plugin-json-path]
  (let [{:keys [ok error]} (read-json plugin-json-path)]
    (if error
      [error]
      (let [missing-fields (->> required-plugin-fields
                                (remove #(get ok %))
                                (mapv #(str "Missing required field '" (name %) "' in " plugin-json-path)))
            skill-errors (->> (:skills ok [])
                              (remove #(fs/exists? (str plugin-dir "/" %)))
                              (mapv #(str "Skills path '" % "' not found in " plugin-dir)))
            agent-errors (->> (:agents ok [])
                              (remove #(fs/exists? (str plugin-dir "/" %)))
                              (mapv #(str "Agents path '" % "' not found in " plugin-dir)))]
        (into [] (concat missing-fields skill-errors agent-errors))))))

(defn- find-unregistered-plugins
  "Finds plugin directories on disk that have a plugin.json but aren't in marketplace.json."
  [plugin-root plugins]
  (let [registered-sources (into #{} (map :source plugins))]
    (->> (fs/list-dir plugin-root)
         (filter fs/directory?)
         (filter #(fs/exists? (str % "/.github/plugin/plugin.json")))
         (remove #(registered-sources (fs/file-name %)))
         (mapv #(str "Plugin directory '" (fs/file-name %) "' has a plugin.json but is not registered in marketplace.json")))))

(defn- validate-marketplace
  "Validates marketplace.json structure and all referenced plugins."
  []
  (let [marketplace-path ".github/plugin/marketplace.json"
        {:keys [ok error]} (read-json marketplace-path)]
    (if error
      [error]
      (let [plugin-root (get-in ok [:metadata :pluginRoot] "./plugins")
            plugins (:plugins ok)
            marketplace-errors (when-not plugins
                                 [(str "No 'plugins' array in " marketplace-path)])]
        (or marketplace-errors
            (into []
                  (concat
                   (mapcat
                    (fn [{:keys [name source]}]
                      (let [plugin-dir (str plugin-root "/" source)
                            plugin-json-path (str plugin-dir "/.github/plugin/plugin.json")]
                        (cond-> []
                          (not (fs/exists? plugin-dir))
                          (conj (str "Plugin '" name "': directory '" plugin-dir "' not found"))

                          (and (fs/exists? plugin-dir)
                               (not (fs/exists? plugin-json-path)))
                          (conj (str "Plugin '" name "': missing " plugin-json-path))

                          (fs/exists? plugin-json-path)
                          (into (validate-plugin-json plugin-dir plugin-json-path)))))
                    plugins)
                   (find-unregistered-plugins plugin-root plugins))))))))

(defn validate!
  "Validates plugin structure. Prints results and exits with appropriate code."
  []
  (println "Validating plugin structure...")
  (let [errors (validate-marketplace)]
    (if (seq errors)
      (do
        (doseq [e errors]
          (println (str "  ERROR: " e)))
        (println (str "\nValidation failed with " (count errors) " error(s)."))
        (System/exit 1))
      (println "  All plugins valid."))))
