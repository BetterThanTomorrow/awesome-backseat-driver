(ns claude-plugin-test
  (:require [claude-plugin :as clp]
            [clojure.string :as string]
            [clojure.test :refer [deftest is]]))

(deftest disallowed-tools-keeps-mcp-for-extension-tools
  (is (= ["Edit" "Write" "NotebookEdit" "Bash" "PowerShell" "WebFetch" "WebSearch" "Agent"]
         (clp/claude-disallowed-tools
           ["vscode/memory" "read" "search" "todo"
            "betterthantomorrow.calva-backseat-driver/clojure-eval"]))
      "Ungranted tool sets are denied; MCP tools stay inherited"))

(deftest disallowed-tools-denies-mcp-without-extension-tools
  (is (= ["Bash" "PowerShell" "WebFetch" "WebSearch" "Agent" "mcp__*"]
         (clp/claude-disallowed-tools ["vscode/memory" "read" "edit" "search" "todo"]))
      "An agent with no extension tools loses every MCP tool"))

(deftest disallowed-tools-reads-tool-set-prefix
  (is (not-any? #{"Read"} (clp/claude-disallowed-tools ["read/problems"]))
      "read/problems grants the read tool set"))

(def copilot-agent
  (str "---\n"
       "description: 'Edits things. Use when: editing.'\n"
       "tools: [read, search, betterthantomorrow.calva-backseat-driver/clojure-eval]\n"
       "name: editor\n"
       "model: Auto (copilot)\n"
       "user-invocable: false\n"
       "---\n"
       "\n"
       "You edit things.\n"))

(deftest copilot-agent-translation
  (let [claude (clp/copilot-agent->claude copilot-agent)
        [fm body] (clp/split-frontmatter claude)]
    (is (= {:name "editor"
            :description "Edits things. Use when: editing."
            :disallowedTools "Edit, Write, NotebookEdit, Bash, PowerShell, WebFetch, WebSearch, Agent"}
           fm)
        "Keeps name and description, translates tools, drops Copilot-only fields")
    (is (= "\nYou edit things.\n" body)
        "Body is copied as is")
    (is (string/starts-with? claude "---\nname: editor\n")
        "name comes first")))

(deftest copilot-agent-without-tools-inherits-all
  (is (= {:name "a" :description "d"}
         (first (clp/split-frontmatter
                  (clp/copilot-agent->claude "---\nname: a\ndescription: d\n---\nbody\n"))))
      "No tools list gives no disallowedTools"))
