(ns validate-cursor
  (:require [babashka.fs :as fs]
            [cheshire.core :as json]
            [clojure.string :as string]
            [cursor-plugin :as cp]
            [cursor-schema :as cs]
            [publish :as pub]))

(def kebab-name-pattern #"^[a-z0-9]([a-z0-9.-]*[a-z0-9])?$")
(def cursor-marketplace-path ".cursor-plugin/marketplace.json")

(defn- path-refs
  [plugin-dir pj key]
  (->> (get pj key [])
       (mapcat (fn [ref]
                 (let [ref (string/replace-first ref #"^\./" "")
                       path (str plugin-dir "/" ref)]
                   (if (and (= key :agents) (fs/directory? path))
                     (->> (fs/list-dir path)
                          (filter #(string/ends-with? (str %) ".md"))
                          (map str))
                     [path]))))
       vec))

(defn- skill-md-files
  [plugin-dir pj]
  (->> (:skills pj [])
       (mapcat (fn [ref]
                 (let [ref (string/replace-first ref #"^\./" "")
                       skill-md (str plugin-dir "/" ref "/SKILL.md")]
                   (when (fs/exists? skill-md)
                     [skill-md]))))))

(defn- collect-md-files
  [plugin-dir pj]
  (distinct
    (concat
      (skill-md-files plugin-dir pj)
      (path-refs plugin-dir pj :agents)
      (path-refs plugin-dir pj :commands)
      (path-refs plugin-dir pj :rules))))

(defn- invalid-name?
  [fm]
  (let [n (get fm "name")]
    (and n (not (re-matches kebab-name-pattern n)))))

(defn- validate-frontmatter
  [path {:keys [require-name?]}]
  (let [fm (pub/parse-frontmatter path)]
    (cond
      (nil? fm)
      [(str "Missing or invalid YAML frontmatter in " path)]

      (not (get fm "description"))
      [(str "Missing required frontmatter 'description' in " path)]

      (and require-name? (not (get fm "name")))
      [(str "Missing required frontmatter 'name' in " path)]

      (invalid-name? fm)
      [(str "Frontmatter 'name' must be kebab-case in " path ": " (get fm "name"))]

      :else [])))

(defn- validate-paths
  [plugin-dir pj]
  (let [refs (concat (:skills pj []) (:agents pj []) (:commands pj []) (:rules pj []))]
    (->> refs
         (remove (fn [ref]
                   (fs/exists? (str plugin-dir "/" (string/replace-first ref #"^\./" "")))))
         (mapv (fn [ref] (str "Path '" ref "' not found under " plugin-dir))))))

(defn- validate-plugin-manifest
  [plugin-dir pj]
  (into []
        (concat
          (cs/validate-plugin-json! pj)
          (validate-paths plugin-dir pj)
          (mapcat (fn [path]
                    (validate-frontmatter path
                                          {:require-name? (string/ends-with? path "SKILL.md")}))
                  (collect-md-files plugin-dir pj)))))

(defn- validate-marketplace-graph
  [marketplace]
  (into []
        (mapcat
          (fn [{:keys [name source]}]
            (let [cursor-pj (str source "/.cursor-plugin/plugin.json")]
              (if (fs/exists? cursor-pj)
                (let [pj (json/parse-string (slurp cursor-pj) true)]
                  (validate-plugin-manifest source pj))
                [(str "Plugin '" name "': missing " cursor-pj)])))
          (:plugins marketplace))))

(defn- l4-drift-errors
  []
  (let [expected-marketplace (cp/json->pretty-string (cp/expected-cursor-marketplace))
        committed-marketplace (cp/committed-cursor-json-string cursor-marketplace-path)
        marketplace-errors (when (not= expected-marketplace committed-marketplace)
                             [(str "L4 drift: " cursor-marketplace-path
                                   " does not match expected generated content")])
        plugin-errors
        (mapcat
          (fn [plugin-dir]
            (let [out-path (str plugin-dir "/.cursor-plugin/plugin.json")
                  expected (cp/json->pretty-string (cp/expected-cursor-plugin-json (str plugin-dir)))
                  committed (cp/committed-cursor-json-string out-path)]
              (when (not= expected committed)
                [(str "L4 drift: " out-path
                      " does not match expected generated content")])))
          (pub/scan-plugin-dirs))]
    (into [] (concat marketplace-errors plugin-errors))))

(defn validate-cursor!
  "Validates Cursor manifests (L1–L4) without writing files. Returns error strings."
  []
  (let [expected-marketplace (cp/expected-cursor-marketplace)
        schema-errors (cs/validate-marketplace-json! expected-marketplace)
        graph-errors (validate-marketplace-graph expected-marketplace)
        plugin-manifest-errors
        (mapcat
          (fn [plugin-dir]
            (validate-plugin-manifest (str plugin-dir)
                                      (cp/expected-cursor-plugin-json (str plugin-dir))))
          (pub/scan-plugin-dirs))
        drift-errors (l4-drift-errors)]
    (into [] (concat schema-errors graph-errors plugin-manifest-errors drift-errors))))
