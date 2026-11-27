(ns claude-plugin
  (:require [babashka.fs :as fs]
            [cheshire.core :as json]
            [clj-yaml.core :as yaml]
            [clojure.string :as string]
            [cursor-plugin :as cp]
            [publish :as pub]))

(def copilot-marketplace-path ".github/plugin/marketplace.json")
(def claude-marketplace-path ".claude-plugin/marketplace.json")
(def claude-plugin-subdir ".claude-plugin")

(def claude-agents-subdir
  "Plugin folder for the Claude Code versions of the Copilot agents in agents/.
  Claude Code loads agents only from the paths plugin.json lists, and does not
  load them from inside .claude-plugin/, so they sit at the plugin root."
  "claude-agents")

(def allowed-plugin-keys
  "plugin.json keys copied from the Copilot manifest. Claude Code strips other
  top-level keys with a validation warning. Component keys are not copied:
  skills/ is scanned by default, and agents are listed by this generator."
  #{:name :displayName :description :version :author :homepage :repository
    :license :keywords})

(def claude-agent-suppressed-plugins
  "Plugins whose Copilot agent is a main-agent persona. In Claude Code a plugin
  agent is a subagent, and running it as the main thread replaces the whole
  Claude Code system prompt, so the agent is left out. The plugin's skill
  carries the same method. An empty `agents` array keeps Claude Code from
  loading the Copilot file in agents/."
  #{"clojure"})

;; ============================================================
;; Agent translation
;; ============================================================

(def copilot-tool-set->claude-tools
  "Claude Code built-in tools for each Copilot tool set. A Copilot agent's
  `tools` is an allowlist; the Claude Code agent gets the tools of the sets it
  does not grant as `disallowedTools`."
  (array-map
    "read" ["Read"]
    "search" ["Grep" "Glob"]
    "edit" ["Edit" "Write" "NotebookEdit"]
    "execute" ["Bash" "PowerShell"]
    "web" ["WebFetch" "WebSearch"]
    "agent" ["Agent"]))

(defn- copilot-tool-set
  "The tool set a Copilot tool id belongs to: `read` for `read` and `read/problems`."
  [tool-id]
  (first (string/split tool-id #"/")))

(defn- extension-tool?
  "True for a tool contributed by a VS Code extension, such as
  `betterthantomorrow.calva-backseat-driver/clojure-eval`. In Claude Code the
  same tools come from MCP servers, under names that depend on what the user
  called the server."
  [tool-id]
  (boolean (re-find #"^[\w-]+\.[\w-]+/" tool-id)))

(defn claude-disallowed-tools
  "Translates a Copilot `tools` allowlist into Claude Code `disallowedTools`.
  A denylist keeps the MCP tools inherited, whatever the user named the server.
  An agent granted no extension tools also loses every MCP tool."
  [copilot-tools]
  (let [granted (set (map copilot-tool-set copilot-tools))]
    (cond-> (->> copilot-tool-set->claude-tools
                 (remove (fn [[tool-set _]] (granted tool-set)))
                 (mapcat val)
                 vec)
      (not-any? extension-tool? copilot-tools) (conj "mcp__*"))))

(defn split-frontmatter
  "Returns [frontmatter-map body] for a markdown file's content, or nil when it
  has no YAML frontmatter."
  [content]
  (when-let [[_ fm body] (re-matches #"(?s)---\r?\n(.*?)\r?\n---\r?\n(.*)" content)]
    [(yaml/parse-string fm) body]))

(defn copilot-agent->claude
  "Rewrites a Copilot agent file's content as a Claude Code agent. Keeps `name`,
  `description`, and the body. Translates `tools` to `disallowedTools`. Leaves
  out `model`, so the agent follows the user's subagent model, and the
  Copilot-only fields."
  [content]
  (let [[{:keys [name description tools]} body] (split-frontmatter content)
        fm (cond-> (array-map :name name :description description)
             (seq tools) (assoc :disallowedTools
                                (string/join ", " (claude-disallowed-tools tools))))]
    (str "---\n"
         (yaml/generate-string fm {:dumper-options {:flow-style :block
                                                    :split-lines false}})
         "---\n"
         body)))

(defn copilot-agent-files
  "The agent .md files a Copilot plugin.json lists, as directories or files."
  [plugin-dir pj]
  (->> (cp/normalize-refs (:agents pj))
       (mapcat (fn [ref]
                 (let [path (fs/path plugin-dir (string/replace-first ref #"^\./" ""))]
                   (if (fs/directory? path)
                     (fs/glob path "*.md")
                     [path]))))
       (map str)
       sort
       vec))

;; ============================================================
;; Expected content
;; ============================================================

(defn- read-copilot-plugin-json
  [plugin-dir]
  (json/parse-string (slurp (str plugin-dir "/.github/plugin/plugin.json")) true))

(defn expected-claude-agents
  "Map of plugin-relative path → content for the plugin's Claude Code agents."
  [plugin-dir]
  (let [pj (read-copilot-plugin-json plugin-dir)]
    (if (contains? claude-agent-suppressed-plugins (:name pj))
      {}
      (into (sorted-map)
            (map (fn [path]
                   [(str claude-agents-subdir "/" (fs/file-name path))
                    (copilot-agent->claude (slurp path))]))
            (copilot-agent-files plugin-dir pj)))))

(defn expected-claude-plugin-json
  "Returns the expected Claude Code plugin.json map for a plugin directory."
  [plugin-dir]
  (let [pj (read-copilot-plugin-json plugin-dir)
        agent-paths (mapv #(str "./" %) (keys (expected-claude-agents plugin-dir)))]
    (cond-> (into {} (filter (fn [[k _]] (allowed-plugin-keys k)) pj))
      (seq (copilot-agent-files plugin-dir pj)) (assoc :agents agent-paths))))

(defn- claude-marketplace-entry
  [plugin-dir]
  (let [pj (read-copilot-plugin-json plugin-dir)]
    {:name (:name pj)
     :source (str "./plugins/" (fs/file-name plugin-dir))
     :description (:description pj)}))

(defn expected-claude-marketplace
  "Returns the expected Claude Code marketplace map from the Copilot sources.
  Sources are ./plugins/<dir> paths, so `pluginRoot` is left out."
  []
  (let [copilot (json/parse-string (slurp copilot-marketplace-path) true)]
    (cp/sort-keys-deep
      {:name (:name copilot)
       :owner (:owner copilot)
       :metadata (dissoc (:metadata copilot) :pluginRoot)
       :plugins (mapv claude-marketplace-entry (pub/scan-plugin-dirs))})))

;; ============================================================
;; Generation
;; ============================================================

(defn- delete-stale-claude-agents!
  "Deletes generated agents whose Copilot source is gone."
  [agents-dir expected]
  (doseq [f (when (fs/directory? agents-dir) (fs/list-dir agents-dir))
          :when (not (contains? expected (str claude-agents-subdir "/" (fs/file-name f))))]
    (fs/delete f)))

(defn- write-claude-agents!
  "Writes the plugin's Claude Code agents and deletes generated agents whose
  Copilot source is gone."
  [plugin-dir]
  (let [expected (expected-claude-agents plugin-dir)
        agents-dir (fs/path plugin-dir claude-agents-subdir)]
    (delete-stale-claude-agents! agents-dir expected)
    (if (seq expected)
      (do (fs/create-dirs agents-dir)
          (doseq [[rel content] expected]
            (spit (str (fs/path plugin-dir rel)) content)))
      (fs/delete-if-exists agents-dir))))

(defn- write-claude-plugin-json!
  [plugin-dir]
  (let [out-dir (fs/path plugin-dir claude-plugin-subdir)]
    (fs/create-dirs out-dir)
    (spit (str (fs/path out-dir "plugin.json"))
          (str (cp/json->pretty-string (expected-claude-plugin-json plugin-dir)) "\n"))))

(defn generate-claude-plugins!
  "Regenerates the Claude Code marketplace, plugin manifests, and agents from
  the Copilot sources."
  []
  (let [plugin-dirs (map str (pub/scan-plugin-dirs))]
    (fs/create-dirs claude-plugin-subdir)
    (spit claude-marketplace-path
          (str (cp/json->pretty-string (expected-claude-marketplace)) "\n"))
    (doseq [plugin-dir plugin-dirs]
      (write-claude-plugin-json! plugin-dir)
      (write-claude-agents! plugin-dir))
    (println (str "Generated Claude Code manifests: " claude-marketplace-path
                  " and " (count plugin-dirs) " plugin.json files."))))
