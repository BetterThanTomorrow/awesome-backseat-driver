(ns bump-versions
  (:require [babashka.fs :as fs]
            [babashka.process :as p]
            [cheshire.core :as json]
            [clojure.string :as string]
            [publish]))

(def plugin-root "./plugins")
(def marketplace-path ".github/plugin/marketplace.json")

;; ============================================================
;; Pure helpers
;; ============================================================

(defn- git-last-commit-epoch
  "Returns the epoch timestamp of the last commit touching path, or nil."
  [path]
  (let [out (-> (p/shell {:out :string :dir "."}
                         "git" "log" "-1" "--format=%ct" "--" (str path))
                :out
                string/trim)]
    (parse-long out)))

(defn- latest-content-commit
  "Returns the epoch timestamp of the newest commit touching any file
   under plugin-dir, excluding plugin.json itself. Returns nil if none."
  [plugin-dir]
  (let [out (-> (p/shell {:out :string :dir "."}
                         "git" "log" "-1" "--format=%ct" "--"
                         (str plugin-dir)
                         (str ":!" plugin-dir "/.github/plugin/plugin.json"))
                :out
                string/trim)]
    (parse-long out)))

(defn- find-plugins-needing-bump
  "Returns a vector of maps for plugins whose content has been committed
   more recently than their plugin.json."
  []
  (let [plugin-dirs (->> (fs/list-dir plugin-root)
                         (filter fs/directory?)
                         (filter #(fs/exists? (str % "/.github/plugin/plugin.json"))))]
    (->> plugin-dirs
         (keep (fn [dir]
                 (let [pj-path (str dir "/.github/plugin/plugin.json")
                       pj (json/parse-string (slurp pj-path) true)
                       pj-epoch (git-last-commit-epoch pj-path)
                       content-epoch (latest-content-commit (str dir))]
                   (when (and content-epoch pj-epoch (> content-epoch pj-epoch))
                     {:dir (str dir)
                      :name (:name pj)
                      :pj-path pj-path
                      :current-version (:version pj)
                      :new-version (publish/bump-patch (:version pj))}))))
         vec)))

(defn- update-plugin-json!
  "Bumps the version in a plugin.json file using string replacement
   to preserve formatting and key order."
  [{:keys [pj-path current-version new-version]}]
  (let [content (slurp pj-path)
        updated (string/replace-first
                 content
                 (str "\"version\" : \"" current-version "\"")
                 (str "\"version\" : \"" new-version "\""))]
    (spit pj-path updated)))

(defn- update-marketplace-versions!
  "Updates plugin versions in marketplace.json to match bumped plugin.json versions."
  [bumps]
  (let [content (slurp marketplace-path)
        parsed (json/parse-string content true)
        bump-map (into {} (map (juxt :name :new-version) bumps))
        updated-plugins (mapv (fn [plugin]
                                (if-let [new-v (bump-map (:name plugin))]
                                  (assoc plugin :version new-v)
                                  plugin))
                              (:plugins parsed))
        updated (assoc parsed :plugins updated-plugins)]
    (spit marketplace-path (json/generate-string updated {:pretty true}))))

;; ============================================================
;; Entry point
;; ============================================================

(defn- parse-frontmatter
  "Parses YAML frontmatter from a markdown file. Returns a map of key-value pairs."
  [path]
  (let [content (slurp (str path))
        lines (string/split-lines content)]
    (when (= "---" (first lines))
      (let [end-idx (->> (rest lines)
                         (map-indexed vector)
                         (some (fn [[i l]] (when (= "---" l) (inc i)))))]
        (when end-idx
          (let [fm-lines (subvec (vec lines) 1 end-idx)]
            (into {} (keep (fn [l]
                             (let [[_ k v] (re-matches #"(\w+):\s+(.*)" l)]
                               (when (and k v)
                                 [k (-> v
                                        string/trim
                                        (string/replace #"^['\"](.*)['\"]\s*$" "$1"))])))
                           fm-lines))))))))

(defn- collect-skill-info
  "Collects name and description from SKILL.md frontmatter for each skill ref."
  [plugin-dir skills]
  (->> skills
       (keep (fn [skill-ref]
               (let [skill-md (str plugin-dir "/" skill-ref "/SKILL.md")]
                 (when (fs/exists? skill-md)
                   (let [fm (parse-frontmatter skill-md)]
                     {:name (get fm "name")
                      :description (get fm "description")})))))
       vec))

(defn- collect-agent-info
  "Collects name and description from agent .md frontmatter for each agent ref."
  [plugin-dir agents]
  (->> agents
       (mapcat (fn [agent-ref]
                 (let [agent-path (str plugin-dir "/" agent-ref)]
                   (if (fs/directory? agent-path)
                     (->> (fs/list-dir agent-path)
                          (filter #(string/ends-with? (str %) ".md"))
                          (map (fn [f]
                                 (let [fm (parse-frontmatter (str f))]
                                   {:name (get fm "name")
                                    :description (get fm "description")}))))
                     (when (fs/exists? agent-path)
                       [(let [fm (parse-frontmatter agent-path)]
                          {:name (get fm "name")
                           :description (get fm "description")})])))))
       (remove nil?)
       vec))

(defn- generate-plugin-readme
  "Generates README.md content for a plugin from its plugin.json and skill/agent frontmatter."
  [plugin-dir]
  (let [pj-path (str plugin-dir "/.github/plugin/plugin.json")
        pj (json/parse-string (slurp pj-path) true)
        skills (collect-skill-info plugin-dir (:skills pj []))
        agents (collect-agent-info plugin-dir (:agents pj []))
        lines [(str "# " (:name pj))
               ""
               (:description pj)
               ""
               (str "Version: " (:version pj))]]
    (str
     (string/join "\n" lines)
     (when (seq skills)
       (str "\n\n## Skills\n\n"
            (string/join "\n\n"
                         (map (fn [{:keys [name description]}]
                                (str "### " name "\n\n" description))
                              skills))))
     (when (seq agents)
       (str "\n\n## Agents\n\n"
            (string/join "\n\n"
                         (map (fn [{:keys [name description]}]
                                (str "### " name "\n\n" description))
                              agents))))
     "\n")))

(defn generate-readmes!
  "Generates README.md for all plugins that have a plugin.json."
  []
  (let [plugin-dirs (->> (fs/list-dir plugin-root)
                         (filter fs/directory?)
                         (filter #(fs/exists? (str % "/.github/plugin/plugin.json")))
                         (sort-by str))]
    (doseq [dir plugin-dirs]
      (let [readme-path (str dir "/README.md")
            content (generate-plugin-readme (str dir))]
        (spit readme-path content)
        (println (str "  Generated " readme-path))))))

(defn bump-versions!
  "Finds plugins with content newer than their plugin.json and bumps
   their patch version in both plugin.json and marketplace.json."
  [{:keys [dry-run]}]
  (let [bumps (find-plugins-needing-bump)]
    (if (empty? bumps)
      (println "All plugin versions are up to date.")
      (do
        (println (str "Plugins needing version bump (" (count bumps) "):"))
        (doseq [{:keys [name current-version new-version]} bumps]
          (println (str "  " name ": " current-version " → " new-version)))
        (when-not dry-run
          (doseq [bump bumps]
            (update-plugin-json! bump))
          (update-marketplace-versions! bumps)
          (println "Done."))
        (when dry-run
          (println "(dry run — no files changed)"))))))
