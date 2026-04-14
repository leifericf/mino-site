(ns mino-site.highlight
  "Inline JavaScript for client-side syntax highlighting of C and mino code.")

(def highlight-js
  "JavaScript that highlights C and mino/Lisp code blocks on page load.
  Applied to <pre><code> elements with data-lang='c' or data-lang='mino'."
  "
(function() {
  function escHtml(s) {
    return s.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;');
  }

  function hlC(code) {
    code = escHtml(code);
    var toks = [];
    // Pull out comments and strings first
    code = code.replace(/(\\/\\/[^\\n]*)/g, function(m) {
      toks.push('<span class=\"hl-comment\">' + m + '</span>');
      return '\\x00T' + (toks.length-1) + 'T\\x00';
    });
    code = code.replace(/(\"(?:[^\"\\\\]|\\\\.)*\")/g, function(m) {
      toks.push('<span class=\"hl-string\">' + m + '</span>');
      return '\\x00T' + (toks.length-1) + 'T\\x00';
    });
    code = code.replace(/(\\/\\*[\\s\\S]*?\\*\\/)/g, function(m) {
      toks.push('<span class=\"hl-comment\">' + m + '</span>');
      return '\\x00T' + (toks.length-1) + 'T\\x00';
    });
    // C keywords
    code = code.replace(/\\b(void|int|long|double|char|const|static|struct|enum|typedef|return|if|else|while|for|do|switch|case|break|continue|sizeof|NULL|unsigned|signed)\\b/g,
      '<span class=\"hl-keyword\">$1</span>');
    // C types (mino-specific)
    code = code.replace(/\\b(mino_val_t|mino_env_t|mino_repl_t|mino_prim_fn|mino_type_t|mino_resolve_fn|mino_vec_node_t|mino_hamt_node_t|size_t|FILE)\\b/g,
      '<span class=\"hl-type\">$1</span>');
    // Preprocessor
    code = code.replace(/^(#\\w+)/gm, '<span class=\"hl-keyword\">$1</span>');
    // Numbers
    code = code.replace(/\\b(\\d+\\.?\\d*[fFlL]?)\\b/g, '<span class=\"hl-number\">$1</span>');
    // Restore tokens
    code = code.replace(/\\x00T(\\d+)T\\x00/g, function(_, i) { return toks[parseInt(i)]; });
    return code;
  }

  function hlMino(code) {
    code = escHtml(code);
    var toks = [];
    // Comments
    code = code.replace(/(;;[^\\n]*)/g, function(m) {
      toks.push('<span class=\"hl-comment\">' + m + '</span>');
      return '\\x00T' + (toks.length-1) + 'T\\x00';
    });
    // Strings
    code = code.replace(/(\"(?:[^\"\\\\]|\\\\.)*\")/g, function(m) {
      toks.push('<span class=\"hl-string\">' + m + '</span>');
      return '\\x00T' + (toks.length-1) + 'T\\x00';
    });
    // Keywords
    code = code.replace(/(:[a-zA-Z][a-zA-Z0-9_\\-.*+!?\\/<>]*)/g,
      '<span class=\"hl-type\">$1</span>');
    // Numbers
    code = code.replace(/\\b(\\d+\\.?\\d*)\\b/g, '<span class=\"hl-number\">$1</span>');
    // Special forms and builtins after open paren
    code = code.replace(/(?<=\\()\\b(def|defmacro|let|fn|if|when|cond|do|loop|recur|try|catch|throw|quote|and|or|->|->>|map|filter|reduce|require|doc|source|apropos)\\b/g,
      '<span class=\"hl-keyword\">$1</span>');
    // Restore tokens
    code = code.replace(/\\x00T(\\d+)T\\x00/g, function(_, i) { return toks[parseInt(i)]; });
    return code;
  }

  document.querySelectorAll('pre code[data-lang]').forEach(function(el) {
    var lang = el.getAttribute('data-lang');
    if (lang === 'c') el.innerHTML = hlC(el.textContent);
    else if (lang === 'mino') el.innerHTML = hlMino(el.textContent);
  });
})();
")
