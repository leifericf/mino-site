(ns mino-site.content.tooling
  "Tooling and editor integration guide."
  (:require
    [hiccup2.core :as h]))

(defn tooling-page
  "Generates the Tooling page HTML body."
  []
  (str
    (h/html
      [:h1 "Tooling and Editor Integration"]
      [:p "mino speaks the "
       [:a {:href "https://nrepl.org" :target "_blank" :rel "noopener"}
        "nREPL protocol"]
       ", the standard used by every major editor in the ecosystem. "
       "One protocol, every editor."]

      [:h2 "Syntax Highlighting"]
      [:p [:a {:href "https://github.com/leifericf/tree-sitter-mino"
               :target "_blank" :rel "noopener"}
           "tree-sitter-mino"]
       " is a "
       [:a {:href "https://tree-sitter.github.io" :target "_blank"
            :rel "noopener"} "tree-sitter"]
       " grammar for mino. It provides syntax highlighting, bracket "
       "matching, structural navigation, and code folding in any editor "
       "that supports tree-sitter."]

      [:h3 "Neovim"]
      [:p "Add mino to your nvim-treesitter configuration:"]
      [:pre [:code
"local parser_config = require(\"nvim-treesitter.parsers\").get_parser_configs()\nparser_config.mino = {\n  install_info = {\n    url = \"https://github.com/leifericf/tree-sitter-mino\",\n    files = { \"src/parser.c\" },\n    branch = \"main\",\n  },\n  filetype = \"mino\",\n}\n\nvim.filetype.add({ extension = { mino = \"mino\" } })"]]
      [:p "Then run " [:code ":TSInstall mino"] "."]

      [:h3 "Helix"]
      [:p "Add to " [:code "~/.config/helix/languages.toml"] ":"]
      [:pre [:code
"[[language]]\nname = \"mino\"\nscope = \"source.mino\"\nfile-types = [\"mino\"]\ncomment-token = \";\"\nindent = { tab-width = 2, unit = \"  \" }\n\n[[grammar]]\nname = \"mino\"\nsource = { git = \"https://github.com/leifericf/tree-sitter-mino\", rev = \"main\" }"]]
      [:p "Then run " [:code "hx --grammar fetch && hx --grammar build"] "."]

      [:h3 "Emacs (29+)"]
      [:pre [:code
"(add-to-list 'treesit-language-source-alist\n             '(mino \"https://github.com/leifericf/tree-sitter-mino\"))\n(treesit-install-language-grammar 'mino)"]]

      [:h3 "Zed"]
      [:p "Create a language extension or add mino to your settings "
       "with the grammar pointed at the "
       [:a {:href "https://github.com/leifericf/tree-sitter-mino"
            :target "_blank" :rel "noopener"} "tree-sitter-mino"]
       " repository."]

      [:h3 "Structural Editing"]
      [:p "Paredit and parinfer work out of the box with mino files. "
       "mino uses standard balanced brackets " [:code "()"] ", "
       [:code "[]"] ", and " [:code "{}"]
       ", so every editor's structural editing support applies "
       "without any additional configuration."]

      [:h2 "mino-nrepl"]
      [:p [:a {:href "https://github.com/leifericf/mino-nrepl"
               :target "_blank" :rel "noopener"}
           "mino-nrepl"]
       " is a standalone nREPL server for mino. It is a small C program "
       "that links against " [:code "mino.c"] " and implements the nREPL "
       "wire protocol (bencode over TCP). No JVM, no runtime dependencies "
       "beyond mino itself."]

      [:h3 "Build"]
      [:pre [:code "git clone --recursive https://github.com/leifericf/mino-nrepl.git\ncd mino-nrepl\nmake"]]
      [:p "This produces a single " [:code "mino-nrepl"] " binary."]

      [:h3 "Run"]
      [:pre [:code "cd your-project/\nmino-nrepl               # random port, written to .nrepl-port\nmino-nrepl --port 7888   # fixed port"]]
      [:p "The server writes a " [:code ".nrepl-port"] " file to the "
       "current directory on startup and removes it on shutdown. Editors "
       "that support nREPL auto-detect this file."]

      [:h2 "Editor Setup"]

      [:h3 "Conjure (Neovim)"]
      [:p [:a {:href "https://github.com/Olical/conjure" :target "_blank"
               :rel "noopener"} "Conjure"]
       " auto-connects when it finds " [:code ".nrepl-port"]
       " in the project directory. Start " [:code "mino-nrepl"]
       ", open a file, and evaluate with "
       [:code "<localleader>ee"] " (current form) or "
       [:code "<localleader>eb"] " (current buffer)."]

      [:h3 "vim-fireplace (Vim)"]
      [:p [:a {:href "https://github.com/tpope/vim-fireplace"
               :target "_blank" :rel "noopener"} "vim-fireplace"]
       " reads " [:code ".nrepl-port"] " automatically. "
       "Evaluate the innermost form with " [:code "cpp"]
       " or a visual selection with " [:code "cp"] "."]

      [:h3 "CIDER (Emacs)"]
      [:p "Run " [:code "M-x cider-connect-clj"] ", enter "
       [:code "localhost"] " and the port number. Basic eval, "
       "completion, and inline results work out of the box. "
       "Advanced features that depend on cider-nrepl middleware are "
       "not available."]

      [:h3 "Calva (VS Code)"]
      [:p "Open the Command Palette and choose "
       [:strong "Calva: Connect to a Running REPL Server"]
       ". Select " [:strong "Generic"] " as the project type, "
       "then enter the host and port."]

      [:h3 "Cursive (IntelliJ)"]
      [:p "Go to " [:strong "Run > Edit Configurations > + > Remote REPL"]
       ". Set the host and port, then connect. Cursive will use "
       "the nREPL connection for evaluation."]

      [:h2 "Supported Operations"]
      [:p "mino-nrepl implements the following nREPL operations:"]
      [:table
       [:thead
        [:tr [:th "Op"] [:th "Description"]]]
       [:tbody
        [:tr [:td [:code "clone"]] [:td "Create a new session."]]
        [:tr [:td [:code "close"]] [:td "Close a session."]]
        [:tr [:td [:code "describe"]] [:td "List server capabilities."]]
        [:tr [:td [:code "eval"]] [:td "Evaluate code in a session."]]
        [:tr [:td [:code "completions"]] [:td "Symbol completion by prefix."]]
        [:tr [:td [:code "load-file"]] [:td "Evaluate file contents."]]
        [:tr [:td [:code "ls-sessions"]] [:td "List active sessions."]]]]

      [:h2 "Guide for Tools Developers"]
      [:p "If you are building editor plugins, developer tools, or "
       "integrations that work with mino, the nREPL protocol gives "
       "you a ready-made communication layer. You do not need to "
       "parse mino output or invent a custom protocol."]

      [:h3 "Connecting"]
      [:p "Connect a TCP socket to the host and port. If "
       [:code ".nrepl-port"] " exists in the project directory, "
       "read it to discover the port number. The server listens on "
       [:code "127.0.0.1"] " by default."]

      [:h3 "Wire format"]
      [:p "All messages are "
       [:a {:href "https://wiki.theory.org/BitTorrentSpecification#Bencoding"
            :target "_blank" :rel "noopener"} "bencode"]
       " dictionaries. Bencode is a simple binary format with four "
       "types:"]
      [:table
       [:thead
        [:tr [:th "Type"] [:th "Encoding"] [:th "Example"]]]
       [:tbody
        [:tr [:td "String"] [:td "length " [:code ":"] " data"]
         [:td [:code "5:hello"]]]
        [:tr [:td "Integer"] [:td [:code "i"] " number " [:code "e"]]
         [:td [:code "i42e"]]]
        [:tr [:td "List"] [:td [:code "l"] " items " [:code "e"]]
         [:td [:code "l5:helloi42ee"]]]
        [:tr [:td "Dictionary"] [:td [:code "d"] " key/value pairs "
              [:code "e"]]
         [:td [:code "d3:foo3:bare"]]]]]
      [:p "Many languages have bencode libraries. A minimal encoder "
       "and decoder is roughly 50 lines in most languages."]

      [:h3 "Message structure"]
      [:p "Every request is a bencode dictionary with at least an "
       [:code "\"op\""] " field. Most ops also require "
       [:code "\"id\""] " (for correlating responses) and "
       [:code "\"session\""] " (from a prior " [:code "clone"] ")."]
      [:p "Every response includes " [:code "\"status\""]
       ", a list that always contains " [:code "\"done\""]
       " when the operation is complete."]

      [:h3 "Typical session lifecycle"]
      [:pre
       [:code {:data-lang "mino"}
"1. Connect TCP socket to server\n2. Send:   {\"op\" \"clone\" \"id\" \"1\"}\n   Recv:   {\"id\" \"1\" \"new-session\" \"<uuid>\" \"status\" [\"done\"]}\n3. Send:   {\"op\" \"eval\" \"id\" \"2\" \"session\" \"<uuid>\" \"code\" \"(+ 1 2)\"}\n   Recv:   {\"id\" \"2\" \"session\" \"<uuid>\" \"ns\" \"user\" \"value\" \"3\" \"status\" [\"done\"]}\n4. Send:   {\"op\" \"close\" \"id\" \"3\" \"session\" \"<uuid>\"}\n   Recv:   {\"id\" \"3\" \"session\" \"<uuid>\" \"status\" [\"done\"]}\n5. Disconnect"]]

      [:h3 "Evaluating code"]
      [:p "Send an " [:code "eval"] " op with a " [:code "\"code\""]
       " field containing the mino source. The server responds with:"]
      [:ul
       [:li "On success: a " [:code "\"value\""] " field containing the "
        "printed result, plus " [:code "\"ns\""] " set to "
        [:code "\"user\""] "."]
       [:li "On error: an " [:code "\"err\""] " field with the error "
        "message and " [:code "\"status\""] " containing "
        [:code "\"error\""] "."]]
      [:p "If the evaluated code produces side-effect output "
       "(via " [:code "println"] " or " [:code "prn"] "), the server "
       "sends a separate message with an " [:code "\"out\""]
       " field before the result message."]

      [:h3 "Completions"]
      [:p "Send a " [:code "completions"] " op with a "
       [:code "\"prefix\""] " field. The server returns a "
       [:code "\"completions\""] " list of dictionaries, each with a "
       [:code "\"candidate\""] " field:"]
      [:pre
       [:code {:data-lang "mino"}
"Send: {\"op\" \"completions\" \"id\" \"4\" \"prefix\" \"ma\" \"session\" \"<uuid>\"}\nRecv: {\"id\" \"4\" \"completions\" [{\"candidate\" \"map\"}\n                                {\"candidate\" \"map?\"}\n                                {\"candidate\" \"macroexpand\"}\n                                {\"candidate\" \"macroexpand-1\"}]\n       \"status\" [\"done\"]}"]]

      [:h3 "Existing nREPL client libraries"]
      [:p "Most languages already have nREPL client libraries that "
       "handle bencode and the message protocol for you:"]
      [:ul
       [:li [:strong "Python"] ": "
        [:a {:href "https://github.com/cemerick/nrepl-python-client"
             :target "_blank" :rel "noopener"} "nrepl-python-client"]]
       [:li [:strong "JavaScript/TypeScript"] ": "
        [:a {:href "https://github.com/phelrine/nrepl-client"
             :target "_blank" :rel "noopener"} "nrepl-client"]]
       [:li [:strong "Go"] ": "
        [:a {:href "https://github.com/nrepl/bencode"
             :target "_blank" :rel "noopener"} "nrepl/bencode"]
        " + TCP socket"]
       [:li [:strong "Emacs Lisp"] ": built into "
        [:a {:href "https://github.com/clojure-emacs/cider"
             :target "_blank" :rel "noopener"} "CIDER"]]
       [:li [:strong "Vim script / Lua"] ": built into "
        [:a {:href "https://github.com/Olical/conjure"
             :target "_blank" :rel "noopener"} "Conjure"]
        " and "
        [:a {:href "https://github.com/tpope/vim-fireplace"
             :target "_blank" :rel "noopener"} "vim-fireplace"]]]
      [:p "Using one of these libraries, connecting to mino-nrepl is "
       "identical to connecting to any other nREPL server. No "
       "mino-specific client code is needed."]

      [:h3 "Source"]
      [:p "The full source for mino-nrepl is at "
       [:a {:href "https://github.com/leifericf/mino-nrepl"
            :target "_blank" :rel "noopener"}
        "github.com/leifericf/mino-nrepl"]
       ". It is roughly 1,400 lines of C99 with no dependencies "
       "beyond mino itself."])))
