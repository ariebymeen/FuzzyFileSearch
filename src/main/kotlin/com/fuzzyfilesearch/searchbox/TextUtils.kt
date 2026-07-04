package com.fuzzyfilesearch.searchbox

/**
 * Invokes [block] once per line in [text].
 *
 * The block receives the line's content (without the trailing newline), the
 * absolute start offset of the line in [text], and the absolute end offset
 * (one past the last char of the line content — so for a line "abc\ndef\n"
 * the first call gets ("abc", 0, 3) and the second gets ("def", 4, 7)).
 *
 * If [block] returns false the iteration stops early; otherwise it continues
 * to the next line.
 */
inline fun forEachLine(text: String, block: (line: String, absStart: Int, absEnd: Int) -> Boolean) {
    if (text.isEmpty()) return
    var lineStart = 0
    var idx = 0
    val n = text.length
    while (idx < n) {
        val nl = text.indexOf('\n', idx)
        val lineEnd = if (nl < 0) n else nl
        val cont = block(text.substring(lineStart, lineEnd), lineStart, lineEnd)
        if (!cont) return
        if (nl < 0) return
        idx = nl + 1
        lineStart = idx
    }
}
