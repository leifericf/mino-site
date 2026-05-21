(ns mino-site.content.bytes
  "Bytes + bit-syntax reference page content.

  Documents MINO_BYTES (the immutable binary-data value type) and the
  Erlang-inspired bit-syntax surface that builds on it."
  (:require
    [hiccup2.core :as h]))

(defn bytes-page
  "Generates the bytes/bit-syntax page HTML body."
  []
  (str
    (h/html
      [:h1 "Bytes and Bit Syntax"]
      [:p "mino has a dedicated immutable binary-data value type and "
       "a destructure-shaped surface for packing and unpacking bit "
       "fields. The design borrows from Erlang's "
       [:a {:href "https://www.erlang.org/doc/system/bit_syntax.html"}
        "bit syntax"]
       ", which has been a first-class data type on BEAM for two "
       "decades. JVM Clojure does not ship anything equivalent; mino's "
       "embedded focus makes binary protocols, sensor packets, "
       "hardware register layouts, and similar bit-precise workloads "
       "a target use case."]

      ;; --- The MINO_BYTES value type ---

      [:h2 "The bytes value type"]
      [:p [:code "(byte-array N)"] " returns a "
       [:code "MINO_BYTES"] " value of "
       [:code "N"] " zero-initialised bytes; "
       [:code "(byte-array coll)"] " packs each element of the "
       "collection as an unsigned byte (range "
       [:code "-128..255"] "). The value is immutable; "
       [:code "aset"] " on it throws "
       [:code "eval/state MST005"] ". The persistent-value model "
       "does not allow in-place writes."]
      [:pre [:code
        "(byte-array 4)\n"
        ";=> #bytes \"00000000\"\n\n"
        "(byte-array [0x41 0x42 0x43])\n"
        ";=> #bytes \"414243\"\n\n"
        "(byte-array (range 5))\n"
        ";=> #bytes \"0001020304\""]]
      [:p "The literal form " [:code "#bytes \"HEX...\""]
       " round-trips through reader and printer. Whitespace between "
       "hex pairs is tolerated by the reader."]

      [:h3 "Predicates"]
      [:ul
       [:li [:code "(bytes? x)"] " is true for byte-aligned "
        "values -- the JVM-compatible subset."]
       [:li [:code "(bitstring? x)"] " is true for any "
        "MINO_BYTES, byte-aligned or bit-aligned."]]
      [:p "Bit-aligned values appear once the bit-syntax surface "
       "(below) packs a total bit length that is not a multiple of "
       "8. Their print form carries a trailing "
       [:code "/N"] " suffix, e.g. "
       [:code "#bytes \"08/5\""]
       " is a five-bit value."]

      [:h3 "Sequence integration"]
      [:p "Every standard seq abstraction works:"]
      [:pre [:code
        "(first (byte-array [9 8 7]))    ; => 9\n"
        "(rest (byte-array [9 8 7]))     ; => (8 7)\n"
        "(nth (byte-array [9 8 7]) 1)    ; => 8\n"
        "(count (byte-array [1 2 3]))    ; => 3\n"
        "(reduce + (byte-array [1 2 3])) ; => 6\n"
        "(map inc (byte-array [1 2 3]))  ; => (2 3 4)\n"
        "(take 5 (filter odd?\n"
        "                (byte-array (range 50))))\n"
        "; => (1 3 5 7 9)\n"
        "(into [] (byte-array [10 20 30]))\n"
        "; => [10 20 30]"]]
      [:p "Internally, " [:code "(seq bytes)"]
       " returns a chunked-cons spine of 32-element chunks, "
       "matching how vector seq works. Pipelines like "
       [:code "(map ...)"] " over "
       [:code "(filter ...)"]
       " over bytes propagate chunkedness end-to-end without "
       "per-element allocation overhead."]

      ;; --- Bit-syntax surface ---

      [:h2 "Constructing bit-aligned values"]
      [:p [:code "bits"] " takes any number of "
       [:code "[value & options]"]
       " segments and packs them in order:"]
      [:pre [:code
        "(bits [0x12  :size 8]\n"
        "      [0x34  :size 8])\n"
        ";=> #bytes \"1234\"\n\n"
        "(bits [0x1234 :size 16])\n"
        ";=> #bytes \"1234\"\n\n"
        "(bits [0x1234 :size 16 :endian :little])\n"
        ";=> #bytes \"3412\"\n\n"
        "(bits [3.14 :size 32 :type :float])\n"
        ";=> #bytes \"4048f5c3\""]]
      [:p "Supported options:"]
      [:ul
       [:li [:code ":size N"] " - bit count. Default 8 for "
        [:code ":int"] " / " [:code ":uint"] ", 64 for "
        [:code ":float"] ", "
        [:code "(* 8 (count v))"] " for "
        [:code ":bytes"] "."]
       [:li [:code ":type T"] " - one of "
        [:code ":int"] ", " [:code ":uint"] ", "
        [:code ":float"] " (32 or 64), or "
        [:code ":bytes"] "."]
       [:li [:code ":endian E"] " - "
        [:code ":big"] " (default) or " [:code ":little"] ". "
        "Little-endian requires " [:code ":size"]
        " to be a multiple of 8 (matching Erlang)."]
       [:li [:code ":signed? B"] " - read-side modifier; affects "
        [:code "bits-get"] " on this segment."]]
      [:p "When the total bit length is not a multiple of 8, the "
       "result is a bit-aligned bitstring:"]
      [:pre [:code
        "(bits [1 :size 5])\n"
        ";=> #bytes \"08/5\"\n"
        "(bytes? (bits [1 :size 5]))     ; => false\n"
        "(bitstring? (bits [1 :size 5])) ; => true"]]

      ;; --- bits-get ---

      [:h2 "Random-access reads"]
      [:p [:code "bits-get"] " reads a bit field at an arbitrary "
       "offset:"]
      [:pre [:code
        "(let [bs (bits [0xABCD :size 16])]\n"
        "  [(bits-get bs :offset 0 :size 16)\n"
        "   (bits-get bs :offset 0 :size 4)\n"
        "   (bits-get bs :offset 12 :size 4)\n"
        "   (bits-get bs :offset 0 :size 8 :signed? true)])\n"
        ";=> [43981 10 13 -85]"]]
      [:p "For " [:code ":type :bytes"]
       ", " [:code "bits-get"] " returns a fresh bytes value "
       "covering the requested range -- a zero-copy-semantics slice."]
      [:p [:code "subbits"] " is the dedicated slice over a "
       "half-open bit range:"]
      [:pre [:code
        "(subbits (bits [0xFF :size 8] [0x00 :size 8]) 4 12)\n"
        ";=> #bytes \"f0\""]]

      ;; --- let-bits ---

      [:h2 "Destructuring with let-bits"]
      [:p [:code "let-bits"] " is the destructure-shape over "
       [:code "bits-get"] ". Each segment is a "
       [:code "[symbol & options]"]
       " vector; the macro emits the running-offset chain "
       "automatically. The final "
       [:code ":type :bytes"]
       " segment without an explicit "
       [:code ":size"] " binds the bit-aligned remainder."]
      [:pre [:code
        "(let-bits [packet\n"
        "           [version  :size 4]\n"
        "           [ihl      :size 4]\n"
        "           [dscp     :size 6]\n"
        "           [ecn      :size 2]\n"
        "           [total    :size 16]\n"
        "           [id       :size 16]\n"
        "           [flags    :size 3]\n"
        "           [frag-off :size 13]\n"
        "           [ttl      :size 8]\n"
        "           [proto    :size 8]\n"
        "           [checksum :size 16]\n"
        "           [src      :size 32 :type :bytes]\n"
        "           [dst      :size 32 :type :bytes]]\n"
        "  ...)"]]
      [:p "That's the canonical IPv4 header decoder. The full "
       "worked example is in mino-examples under "
       [:code "use-cases/packet_parsing.cpp"] "."]

      ;; --- Why this matters ---

      [:h2 "Why this is in mino"]
      [:p "Embedded use cases routinely deal with binary protocols "
       "that JVM Clojure makes awkward: every "
       [:code "ByteBuffer.get*"] " call is a separate ceremony, and "
       "sub-byte fields require manual shift / mask arithmetic. "
       "Erlang's bit syntax solved this in 2001 and remains its most "
       "distinctive surface feature; mino borrows the shape because "
       "the embedded-runtime niche has the same needs."]
      [:p "mino's surface is not a drop-in clone -- Clojure's "
       "destructure idioms shape the API names "
       "(" [:code "let-bits"] " mirrors the rest of the "
       [:code "let"] " family) -- but the semantics line up: type "
       "tag, size, endianness, and sign all work the way an Erlang "
       "programmer expects."]
      [:p "Chess engines are another classic bitboard use case; the "
       [:code "use-cases/chess_bitboard.cpp"]
       " example in mino-examples shows how to use "
       [:code "bits"] " / " [:code "bits-get"]
       " plus mino's bitwise primitives to represent piece positions "
       "as 64-bit bitboards and compute knight attacks."]

      ;; --- C API ---

      [:h2 "Embedder C-API"]
      [:p "Host code can construct and inspect bytes values without "
       "going through the script surface:"]
      [:ul
       [:li [:code "mino_bytes(S, src, n)"] " - copy "
        [:code "n"] " bytes from "
        [:code "src"] " (NULL = zero-fill)."]
       [:li [:code "mino_bytes_from_array(S, src, n)"]
        " - signed-byte-pointer peer."]
       [:li [:code "mino_is_bytes(v)"] ", "
        [:code "mino_is_bitstring(v)"] " - tag-aware predicates."]
       [:li [:code "mino_bytes_len(v)"] ", "
        [:code "mino_bytes_bit_len(v)"]
        " - byte and total-bit counts."]
       [:li [:code "mino_bytes_data(v)"]
        " - pointer into the GC-managed buffer."]
       [:li [:code "mino_bytes_get(v, i)"]
        " - read a single byte as 0..255 unsigned int."]]
      [:p "The buffer is GC-managed; the pointer is stable for the "
       "value's lifetime. Cross-state "
       [:code "mino_clone"] " deep-copies the bytes so each "
       [:code "mino_state"] " owns its own storage."])))
