(ns validate-claude
  (:require [babashka.fs :as fs]
            [cheshire.core :as json]
            [claude-plugin :as clp]
            [cursor-plugin :as cp]
            [publish :as pub]))

(defn- drift-error
  [path expected]
  (when (not= expected (cp/committed-cursor-json-string path))
    [(str "L4 drift: " path " does not match expected generated content")]))

(defn- agent-drift-errors
  "Generated agents that differ from their Copilot source, and files in
  claude-agents/ that no Copilot agent produces."
  [plugin-dir]
  (let [expected (clp/expected-claude-agents plugin-dir)
        agents-dir (fs/path plugin-dir clp/claude-agents-subdir)
        stray (when (fs/directory? agents-dir)
                (->> (fs/list-dir agents-dir)
                     (map #(str clp/claude-agents-subdir "/" (fs/file-name %)))
                     (remove #(contains? expected %))))]
    (concat
      (mapcat (fn [[rel content]]
                (let [path (str plugin-dir "/" rel)]
                  (when (not= content (when (fs/exists? path) (slurp path)))
                    [(str "L4 drift: " path " does not match expected generated content")])))
              expected)
      (map #(str "L4 drift: " plugin-dir "/" % " has no Copilot source agent") stray))))

(defn- unlisted-skill-errors
  "Claude Code loads every skills/<name>/SKILL.md, so a skill the Copilot
  manifest leaves out would still reach Claude Code users."
  [plugin-dir]
  (let [pj (json/parse-string (slurp (str plugin-dir "/.github/plugin/plugin.json")) true)
        listed (set (map #(str (fs/normalize (fs/path plugin-dir %)))
                         (cp/normalize-refs (:skills pj))))]
    (->> (fs/glob plugin-dir "skills/*/SKILL.md")
         (map (comp str fs/normalize fs/parent))
         (remove listed)
         (map #(str "Skill " % " is not in the Copilot plugin.json; Claude Code would load it anyway")))))

(defn validate-claude!
  "Validates the generated Claude Code files against the Copilot sources
  without writing anything. Returns error strings."
  []
  (into []
        (concat
          (drift-error clp/claude-marketplace-path
                       (cp/json->pretty-string (clp/expected-claude-marketplace)))
          (mapcat (fn [plugin-dir]
                    (let [plugin-dir (str plugin-dir)]
                      (concat
                        (drift-error (str plugin-dir "/.claude-plugin/plugin.json")
                                     (cp/json->pretty-string (clp/expected-claude-plugin-json plugin-dir)))
                        (agent-drift-errors plugin-dir)
                        (unlisted-skill-errors plugin-dir))))
                  (pub/scan-plugin-dirs)))))
