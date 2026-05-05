(ns publish
  (:require [babashka.process :as p]
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
;; README generation
;; ============================================================

(defn generate-plugins-table
  "Generates a markdown table of plugins from marketplace.json."
  []
  (let [marketplace (json/parse-string (slurp marketplace-path) true)
        plugins (:plugins marketplace)
        header "| Plugin | Description |\n|---|---|"
        rows (mapv (fn [{:keys [name description]}]
                     (str "| `" name "` | " description " |"))
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
      (println "README.md plugins table: no markers found, skipping.")
      (do
        (spit "README.md" updated)
        (println "README.md plugins table updated.")))))
