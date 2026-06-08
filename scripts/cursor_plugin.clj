(ns cursor-plugin
  (:require [babashka.fs :as fs]
            [cheshire.core :as json]
            [clojure.string :as string]
            [publish :as pub]))

(def copilot-marketplace-path ".github/plugin/marketplace.json")
(def cursor-marketplace-path ".cursor-plugin/marketplace.json")
(def cursor-plugin-subdir ".cursor-plugin")

(def allowed-plugin-keys
  #{:name :displayName :description :version :author :publisher :homepage
    :repository :license :logo :keywords :category :tags :commands :agents
    :skills :rules :hooks :mcpServers})

(def component-dir-keys
  "Manifest keys that Cursor expects as directory path strings, not Copilot-style ref arrays."
  #{:skills :agents :commands :rules})

(def cursor-agent-suppressed-plugins
  "Plugins whose Copilot agent must NOT surface as a Cursor subagent. In Cursor
  the Copilot main-agent persona is delivered via a rule instead; the subagent
  would only invite unwanted auto-delegation. An explicit empty :agents array
  hides the agent in Cursor — confirmed via testing; omitting the key instead
  lets Cursor fall back to ./agents/ folder discovery."
  #{"clojure"})

(defn normalize-refs
  "Coerce plugin.json stringOrStringArray refs to a seq of path strings."
  [v]
  (cond
    (nil? v) []
    (string? v) [v]
    (sequential? v) (vec v)
    :else []))

(defn- cursor-dir-ref
  [key]
  (case key
    :skills "./skills/"
    :agents "./agents/"
    :commands "./commands/"
    :rules "./rules/"))

(defn sort-keys-deep
  "Recursively sorts map keys for stable JSON output."
  [x]
  (cond
    (map? x) (into (sorted-map)
                   (map (fn [[k v]] [k (sort-keys-deep v)]) x))
    (vector? x) (mapv sort-keys-deep x)
    (sequential? x) (mapv sort-keys-deep x)
    :else x))

(defn json->pretty-string
  "Canonical pretty JSON for byte-stable L4 comparisons."
  [data]
  (-> data sort-keys-deep (json/generate-string {:pretty true})))

(defn copilot-plugin-json->cursor
  "Filters a Copilot plugin.json map to Cursor-allowed keys and rewrites
  component refs to directory path strings (Cursor loader convention).

  Options:
   :has-rules-dir?   inject a Cursor `rules` ref even when the Copilot manifest
                     omits it — Copilot does not bundle rules, Cursor does
   :suppress-agents? emit an empty `agents` array to hide the Copilot agent in
                     Cursor (an omitted key falls back to folder discovery)"
  ([pj] (copilot-plugin-json->cursor pj {}))
  ([pj {:keys [has-rules-dir? suppress-agents?]}]
   (let [pj (cond-> pj
              has-rules-dir? (assoc :rules ["./rules"]))
         base (into {} (filter (fn [[k _]] (allowed-plugin-keys k)) pj))
         refs-rewritten (reduce
                          (fn [m k]
                            (if (seq (normalize-refs (get pj k)))
                              (assoc m k (cursor-dir-ref k))
                              (dissoc m k)))
                          base
                          component-dir-keys)]
     (cond-> refs-rewritten
       suppress-agents? (assoc :agents [])))))

(defn cursor-plugin-dir->entry
  "Builds a Cursor marketplace plugins[] entry (no version)."
  [plugin-dir]
  (let [pj (json/parse-string (slurp (str plugin-dir "/.github/plugin/plugin.json")) true)
        dir-name (str (fs/file-name plugin-dir))]
    {:name (:name pj)
     :source (str "plugins/" dir-name)
     :description (:description pj)}))

(defn copilot-marketplace->cursor
  "Transforms Copilot marketplace.json to Cursor marketplace.json."
  [copilot-marketplace]
  (let [plugin-dirs (pub/scan-plugin-dirs)
        plugins (mapv cursor-plugin-dir->entry plugin-dirs)]
    (-> {:name (:name copilot-marketplace)
         :owner (:owner copilot-marketplace)
         :metadata (:metadata copilot-marketplace)
         :plugins plugins}
        sort-keys-deep)))

(defn expected-cursor-plugin-json
  "Returns expected Cursor plugin.json map for a plugin directory."
  [plugin-dir]
  (let [pj (json/parse-string (slurp (str plugin-dir "/.github/plugin/plugin.json")) true)]
    (copilot-plugin-json->cursor
      pj
      {:has-rules-dir? (fs/directory? (str plugin-dir "/rules"))
       :suppress-agents? (contains? cursor-agent-suppressed-plugins (:name pj))})))

(defn expected-cursor-marketplace
  "Returns expected Cursor marketplace map from current Copilot sources."
  []
  (let [copilot (json/parse-string (slurp copilot-marketplace-path) true)]
    (copilot-marketplace->cursor copilot)))

(defn write-cursor-plugin-json!
  [plugin-dir]
  (let [out-dir (str plugin-dir "/" cursor-plugin-subdir)
        out-path (str out-dir "/plugin.json")
        content (json->pretty-string (expected-cursor-plugin-json plugin-dir))]
    (fs/create-dirs out-dir)
    (spit out-path (str content "\n"))
    out-path))

(defn generate-cursor-plugins!
  "Regenerates .cursor-plugin/ manifests from Copilot sources."
  []
  (let [plugin-dirs (pub/scan-plugin-dirs)
        marketplace-content (json->pretty-string (expected-cursor-marketplace))]
    (fs/create-dirs ".cursor-plugin")
    (spit cursor-marketplace-path (str marketplace-content "\n"))
    (doseq [plugin-dir plugin-dirs]
      (write-cursor-plugin-json! (str plugin-dir)))
    (println (str "Generated Cursor manifests: " cursor-marketplace-path
                    " and " (count plugin-dirs) " plugin.json files."))))

(defn read-committed-json
  [path]
  (when (fs/exists? path)
    (json/parse-string (slurp path) true)))

(defn committed-cursor-json-string
  [path]
  (when (fs/exists? path)
    (string/trim (slurp path))))
