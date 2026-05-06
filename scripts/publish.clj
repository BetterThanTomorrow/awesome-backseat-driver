(ns publish
  (:require [babashka.fs :as fs]
            [babashka.process :as p]
            [cheshire.core :as json]
            [clojure.string :as string]))

(def marketplace-path ".github/plugin/marketplace.json")

;; ============================================================
;; Pure helpers
;; ============================================================

(defn read-marketplace-version
  "Reads the current version from marketplace.json."
  []
  (-> (slurp marketplace-path)
      (json/parse-string true)
      (get-in [:metadata :version])))

(defn bump-patch
  "Increments the patch component of a semver string."
  [version]
  (let [parts (string/split version #"\.")
        major (first parts)
        minor (second parts)
        patch (parse-long (nth parts 2))]
    (str major "." minor "." (inc patch))))

(defn parse-unreleased
  "Extracts unreleased changelog entries. Returns a vector of non-blank lines."
  [changelog-content]
  (let [lines (string/split-lines changelog-content)
        unreleased-idx (->> lines
                            (map-indexed vector)
                            (some (fn [[i l]]
                                    (when (re-matches #"## \[Unreleased\].*" l) i))))
        next-heading-idx (->> lines
                              (map-indexed vector)
                              (drop (inc unreleased-idx))
                              (some (fn [[i l]]
                                      (when (re-matches #"##[# ].*" l) i))))]
    (when unreleased-idx
      (->> lines
           (drop (inc unreleased-idx))
           (take (- (or next-heading-idx (count lines)) unreleased-idx 1))
           (filterv (complement string/blank?))))))

(defn update-changelog
  "Returns new changelog content with unreleased entries moved to a versioned section."
  [changelog-content version date]
  (let [lines (string/split-lines changelog-content)
        unreleased-idx (->> lines
                            (map-indexed vector)
                            (some (fn [[i l]]
                                    (when (re-matches #"## \[Unreleased\].*" l) i))))
        next-heading-idx (->> lines
                              (map-indexed vector)
                              (drop (inc unreleased-idx))
                              (some (fn [[i l]]
                                      (when (re-matches #"##[# ].*" l) i))))
        before (subvec (vec lines) 0 (inc unreleased-idx))
        unreleased-entries (->> lines
                                (drop (inc unreleased-idx))
                                (take (- (or next-heading-idx (count lines)) unreleased-idx 1))
                                vec)
        after (when next-heading-idx
                (subvec (vec lines) next-heading-idx))
        new-heading (str "## [" version "] - " date)
        ;; Filter trailing blanks from unreleased entries to avoid double spacing
        trimmed-entries (vec (reverse (drop-while string/blank? (reverse unreleased-entries))))]
    (string/join "\n"
                 (concat before
                         [""]
                         [new-heading]
                         trimmed-entries
                         (when after [""])
                         after))))

(defn update-marketplace-version
  "Returns new marketplace.json content with updated version."
  [content version]
  (let [parsed (json/parse-string content true)
        updated (assoc-in parsed [:metadata :version] version)]
    (json/generate-string updated {:pretty true})))

;; ============================================================
;; Git helpers
;; ============================================================

(defn git-current-branch []
  (-> (p/shell {:out :string} "git" "rev-parse" "--abbrev-ref" "HEAD")
      :out
      string/trim))

(defn git-clean? []
  (-> (p/shell {:out :string} "git" "status" "--porcelain")
      :out
      string/trim
      string/blank?))

(defn git-fast-forwardable?
  "Checks if origin/master is an ancestor of HEAD."
  []
  (try
    (p/shell {:out :string :err :string} "git" "merge-base" "--is-ancestor" "origin/master" "HEAD")
    true
    (catch Exception _
      false)))

;; ============================================================
;; Publish (local)
;; ============================================================

(defn publish!
  "Validates preconditions and pushes a [publish] marker commit."
  [{:keys [dry-run]}]
  (let [branch (git-current-branch)
        clean? (git-clean?)
        ff? (git-fast-forwardable?)
        changelog (slurp "CHANGELOG.md")
        unreleased (parse-unreleased changelog)
        current-version (read-marketplace-version)
        release-version current-version
        errors (cond-> []
                 (not= branch "next")
                 (conj (str "Must be on 'next' branch (currently on '" branch "')"))
                 (not clean?)
                 (conj "Working directory is not clean")
                 (not ff?)
                 (conj "Branch 'next' is not fast-forwardable onto 'master'")
                 (empty? unreleased)
                 (conj "No unreleased entries in CHANGELOG.md"))]
    (if (seq errors)
      (do
        (println "Publish blocked:")
        (doseq [e errors]
          (println (str "  - " e)))
        (System/exit 1))
      (do
        (println "Ready to publish:")
        (println (str "  Release version: " release-version))
        (println (str "  Unreleased entries:"))
        (doseq [entry unreleased]
          (println (str "    " entry)))
        (println)
        (if dry-run
          (println (str "[dry-run] Would create empty commit '[publish] v" release-version "' and push next"))
          (do
            (print "Proceed? [y/N] ")
            (flush)
            (let [answer (string/trim (read-line))]
              (if (= (string/lower-case answer) "y")
                (do
                  (p/shell "git" "commit" "--allow-empty" "-m" (str "[publish] v" release-version))
                  (p/shell "git" "push" "origin" "next")
                  (println (str "Pushed [publish] v" release-version " to next.")))
                (println "Aborted.")))))))))

;; ============================================================
;; CI release
;; ============================================================

(defn ci-release!
  "Called by CI to update changelog and marketplace version."
  [version]
  (let [date (str (java.time.LocalDate/now))
        changelog (slurp "CHANGELOG.md")
        new-changelog (update-changelog changelog version date)
        marketplace-content (slurp marketplace-path)
        new-marketplace (update-marketplace-version marketplace-content version)]
    (spit "CHANGELOG.md" new-changelog)
    (spit marketplace-path new-marketplace)
    (println (str "Updated CHANGELOG.md and " marketplace-path " to v" version))))

(defn bump-version!
  "Called by CI to bump only the marketplace version (no changelog changes)."
  [version]
  (let [marketplace-content (slurp marketplace-path)
        new-marketplace (update-marketplace-version marketplace-content version)]
    (spit marketplace-path new-marketplace)
    (println (str "Bumped " marketplace-path " to v" version))))

;; ============================================================
;; Marketplace plugins generation
;; ============================================================

(defn- scan-plugin-dirs
  "Returns sorted list of plugin directories that have a plugin.json."
  []
  (let [marketplace (json/parse-string (slurp marketplace-path) true)
        plugin-root (get-in marketplace [:metadata :pluginRoot] "./plugins")]
    (->> (fs/list-dir plugin-root)
         (filter fs/directory?)
         (filter #(fs/exists? (str % "/.github/plugin/plugin.json")))
         (sort-by str)
         vec)))

(defn- plugin-dir->entry
  "Reads a plugin directory's plugin.json and returns a marketplace entry."
  [plugin-dir]
  (let [pj (json/parse-string (slurp (str plugin-dir "/.github/plugin/plugin.json")) true)
        dir-name (str (fs/file-name plugin-dir))]
    {:name (:name pj)
     :source dir-name
     :description (:description pj)
     :version (:version pj)}))

(defn generate-marketplace-plugins!
  "Regenerates the plugins array in marketplace.json from plugin directories."
  []
  (let [content (slurp marketplace-path)
        parsed (json/parse-string content true)
        plugin-dirs (scan-plugin-dirs)
        plugins (mapv plugin-dir->entry plugin-dirs)
        updated (assoc parsed :plugins plugins)]
    (spit marketplace-path (json/generate-string updated {:pretty true}))
    (println (str "marketplace.json: updated " (count plugins) " plugins."))))

;; ============================================================
;; README generation
;; ============================================================

(defn- parse-frontmatter
  "Parses YAML frontmatter from a markdown file. Returns a map of string keys to values."
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
                                        (string/replace #"^['\"](.*)['\"]\.?\s*$" "$1"))])))
                           fm-lines))))))))

(defn- collect-plugin-items
  "Collects skill and agent names with paths from a plugin directory using its plugin.json."
  [plugin-dir pj]
  (let [skills (->> (:skills pj [])
                    (keep (fn [ref]
                            (let [ref (string/replace-first ref #"^\./" "")
                                  skill-md (str plugin-dir "/" ref "/SKILL.md")]
                              (when (fs/exists? skill-md)
                                {:name (get (parse-frontmatter skill-md) "name")
                                 :path (str plugin-dir "/" ref)}))))
                    vec)
        agents (->> (:agents pj [])
                    (mapcat (fn [ref]
                              (let [ref (string/replace-first ref #"^\./" "")
                                    path (str plugin-dir "/" ref)]
                                (if (fs/directory? path)
                                  (->> (fs/list-dir path)
                                       (filter #(string/ends-with? (str %) ".md"))
                                       (keep (fn [f]
                                               (let [fm (parse-frontmatter (str f))]
                                                 (when (get fm "name")
                                                   {:name (get fm "name")
                                                    :path (str f)})))))
                                  (when (fs/exists? path)
                                    (let [fm (parse-frontmatter path)]
                                      (when (get fm "name")
                                        [{:name (get fm "name")
                                          :path path}])))))))
                    vec)]
    {:skills skills :agents agents}))

(defn- format-item-link
  "Formats a single item as a markdown link, stripping leading ./"
  [{:keys [name path]}]
  (let [link-path (string/replace-first path #"^\./" "")]
    (str "[" name "](" link-path ")")))

(defn- format-items
  "Formats skill and agent names as linked items for table display."
  [{:keys [skills agents]}]
  (let [parts (concat
               (when (seq agents)
                 [(str (if (> (count agents) 1) "Agents" "Agent") ": "
                       (string/join ", " (map format-item-link agents)))])
               (when (seq skills)
                 [(str (if (> (count skills) 1) "Skills" "Skill") ": "
                       (string/join ", " (map format-item-link skills)))]))]
    (string/join " · " parts)))

(defn generate-plugins-table
  "Generates a markdown table of plugins with links and contents from marketplace.json."
  []
  (let [marketplace (json/parse-string (slurp marketplace-path) true)
        plugin-root (get-in marketplace [:metadata :pluginRoot] "./plugins")
        plugins (:plugins marketplace)
        header "| Plugin | Description | Contents |\n|---|---|---|"
        rows (mapv (fn [{:keys [name source description]}]
                     (let [plugin-dir (str plugin-root "/" source)
                           link-path (string/replace-first plugin-dir #"^\./" "")
                           pj-path (str plugin-dir "/.github/plugin/plugin.json")
                           items (when (fs/exists? pj-path)
                                   (let [pj (json/parse-string (slurp pj-path) true)]
                                     (collect-plugin-items plugin-dir pj)))
                           items-str (format-items items)]
                       (str "| [" name "](" link-path "/) | " description " | " items-str " |")))
                   plugins)]
    (str header "\n" (string/join "\n" rows))))

(defn update-readme!
  "Updates the plugins table in README.md between marker comments."
  []
  (let [readme (slurp "README.md")
        table (generate-plugins-table)
        updated (string/replace readme
                                #"(?s)<!-- plugins-table-start -->\n.*?\n<!-- plugins-table-end -->"
                                (str "<!-- plugins-table-start -->\n" table "\n<!-- plugins-table-end -->"))]
    (if (= readme updated)
      (println "README.md plugins table: already up to date.")
      (do
        (spit "README.md" updated)
        (println "README.md plugins table updated.")))))
