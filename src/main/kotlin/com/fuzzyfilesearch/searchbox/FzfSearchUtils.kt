package com.fuzzyfilesearch.searchbox

fun CharArray.leadingWhitespaces(): Int {
    var count = 0
    for (char in this) {
        if (!char.isWhitespace()) {
            break
        }
        count++
    }
    return count
}

fun CharArray.trailingWhitespaces(): Int {
    var count = 0
    for (i in this.size - 1 downTo 0) {
        if (!this[i].isWhitespace()) {
            break
        }
        count++
    }
    return count
}

public fun normalizeRune(r: Char): Char {
    if (r < '\u00C0' || r > '\u2184') {
        return r
    }

    val n = normalized[r]
    if (n != null) {
        return n
    }
    return r
}
