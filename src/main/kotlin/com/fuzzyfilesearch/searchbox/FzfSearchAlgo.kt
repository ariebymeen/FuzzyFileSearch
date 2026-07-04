package com.fuzzyfilesearch.searchbox

import java.io.File
import kotlin.math.min

// Constants:
val scoreMatch        = 16
val scoreGapStart     = -3
val scoreGapExtension = -1

// We prefer matches at the beginning of a word, but the bonus should not be
// too great to prevent the longer acronym matches from always winning over
// shorter fuzzy matches. The bonus point here was specifically chosen that
// the bonus is cancelled when the gap between the acronyms grows over
// 8 characters, which is approximately the average length of the words found
// in web2 dictionary and my file system.
val bonusBoundary = scoreMatch / 2

// Although bonus point for non-word characters is non-contextual, we need it
// for computing bonus points for consecutive chunks starting with a non-word
// character.
val bonusNonWord = scoreMatch / 2

// Edge-triggered bonus for matches in camelCase words.
// Compared to word-boundary case, they don't accompany single-character gaps
// (e.g. FooBar vs. foo-bar), so we deduct bonus point accordingly.
val bonusCamel123 = bonusBoundary + scoreGapExtension

// Minimum bonus point given to characters in consecutive chunks.
// Note that bonus points for consecutive matches shouldn't have needed if we
// used fixed match score as in the original algorithm.
val bonusConsecutive = -(scoreGapStart + scoreGapExtension)


var delimiterChars = "/,:;|"
const val whiteChars = " \t\n\u000B\u000C\r\u0085\u00A0" // TODO: Is this correct?

typealias CharClass = Int
val charWhite = 0
val charNonWord = 1
val charDelimiter = 2
val charLower = 3
val charUpper = 4
val charLetter = 5
val charNumber = 6

// The first character in the typed pattern usually has more significance
// than the rest so it's important that it appears at special positions where
// bonus points are given, e.g. "to-go" vs. "ongoing" on "og" or on "ogo".
// The amount of the extra bonus should be limited so that the gap penalty is
// still respected.
val bonusFirstCharMultiplier = 2

// Extra bonus for word boundary after whitespace character or beginning of the string
var bonusBoundaryWhite = bonusBoundary + 2

// Extra bonus for word boundary after slash, colon, semi-colon, and comma
var bonusBoundaryDelimiter = bonusBoundary + 1

var initialCharClass = charWhite

// A minor optimization that can give 15%+ performance boost
var asciiCharClasses = Array<CharClass>(128) { 0 }

// A minor optimization that can give yet another 5% performance boost
var bonusMatrix = Array<Array<Short>>(128) { Array(128) { 0 } }

data class Result(
    val start: Int,
    val end  : Int,
    val score: Int)

public fun initFzf(): Boolean {
    bonusBoundaryWhite = bonusBoundary
    bonusBoundaryDelimiter = bonusBoundary + 1
    delimiterChars = if (File.separatorChar == '/') {
        "/"
    } else {
        "${File.separatorChar}/"
    }
    initialCharClass = charDelimiter

    for (i in 0..127) {
        val char = i.toChar()
        val c = when {
            char in 'a'..'z' -> charLower
            char in 'A'..'Z' -> charUpper
            char in '0'..'9' -> charNumber
            whiteChars.contains(char) -> charWhite
            delimiterChars.contains(char) -> charDelimiter
            else -> charNonWord
        }
        asciiCharClasses[i] = c
    }

    for (i in 0..charNumber) {
        for (j in 0..charNumber) {
            bonusMatrix[i][j] = bonusFor(i, j).toShort()
        }
    }
    return true
}

fun bonusFor(prevClass: CharClass, currentClass: CharClass): Int {
    if (currentClass > charNonWord) {
        return when (prevClass) {
            charWhite -> bonusBoundaryWhite      // Word boundary after whitespace
            charDelimiter -> bonusBoundaryDelimiter // Word boundary after delimiter
            charNonWord -> bonusBoundary         // General word boundary
            else -> 0
        }
    }

    if ((prevClass == charLower && currentClass == charUpper) ||
        (prevClass != charNumber && currentClass == charNumber)) {
        // camelCase or letter followed by number
        return bonusCamel123
    }

    return when (currentClass) {
        charNonWord, charDelimiter -> bonusNonWord
        charWhite -> bonusBoundaryWhite
        else -> 0
    }
}

fun charClassOfNonAscii(char: Char): CharClass {
    return when {
        char.isLowerCase() -> charLower
        char.isUpperCase() -> charUpper
        char.isDigit() -> charNumber
        char.isLetter() -> charLetter
        char.isWhitespace() -> charWhite
        delimiterChars.contains(char) -> charDelimiter
        else -> charNonWord
    }
}

fun charClassOf(char: Char): CharClass {
    return if (char.code <= 127) {
        asciiCharClasses[char.code] // Assuming asciiCharClasses is an array of CharClass
    } else {
        charClassOfNonAscii(char)
    }
}

fun calculateScore(
    caseSensitive: Boolean,
    text: String, // Assuming this is a Kotlin equivalent of util.Chars
    pattern: String, // Use CharArray to represent the slice of runes
    sidx: Int,
    eidx: Int,
//    withPos: Boolean
): Pair<Int, IntArray?> { // Using Pair to return both score and positions
    var pidx = 0
    var score = 0
    var inGap = false
    var consecutive = 0
    var firstBonus: Short = 0
    val pos = null // Initialize position array if needed

    var prevClass = initialCharClass
    if (sidx > 0) {
        if (!caseSensitive) {
            prevClass = charClassOf(text.get(sidx - 1).lowercaseChar())
        } else {
            prevClass = charClassOf(text.get(sidx - 1))
        }
    }

    for (idx in sidx until eidx) {
        var char = text.get(idx)
        var pChar = pattern[pidx]

        // Handle case sensitivity
        if (!caseSensitive) {
            char = char.lowercaseChar()
            pChar = pChar.lowercaseChar()
        }

        val classType = charClassOf(char)
        char = normalizeRune(char)

        if (char == pChar) {
            score += scoreMatch

            var bonus = bonusMatrix[prevClass][classType]
            if (consecutive == 0) {
                firstBonus = bonus
            } else {
                // Break consecutive chunk
                if (bonus >= bonusBoundary && bonus > firstBonus) {
                    firstBonus = bonus
                }
                // Use the maximum bonus for consecutive characters
                bonus = maxOf(bonus.toInt(), firstBonus.toInt(), bonusConsecutive).toShort()
            }

            score += if (pidx == 0) {
                (bonus * bonusFirstCharMultiplier).toInt()
            } else {
                bonus.toInt()
            }

            inGap = false
            consecutive++
            pidx++
            if (pidx == pattern.length) {
                break
            }
        } else {
            score += if (inGap) {
                scoreGapExtension
            } else {
                scoreGapStart
            }
            inGap = true
            consecutive = 0
            firstBonus = 0
        }
        prevClass = classType
    }

    return Pair(score, pos) // Return the score and positions as a Pair
}

//fun bonusAt(input: CharArray,
//            idx: Int) : Short {
//    if (idx == 0) {
//        return bonusBoundaryWhite.toShort()
//    }
//    return bonusMatrix[charClassOf(input[idx-1])][charClassOf(input[idx])]
//}
//
//fun posArray(withPos: Boolean,
//             len: Int): IntArray? {
//    if (withPos) {
//        return IntArray(len) {0}
//    }
//    return null
//}

fun trySkip(input: String,
            caseSensitive: Boolean,
            b: Byte,
            from: Int): Int {
    // Convert byte back to char for String.indexOf. For ASCII bytes the
    // conversion is a sign extension that we mask off.
    val bc = (b.toInt() and 0xFF).toChar()
    var idx = input.indexOf(bc, from)

    if (idx == from) {
        // Can't skip any further (the byte is at position 'from')
        return from
    }

    // We may need to search for the uppercase letter again.
    if (!caseSensitive && bc in 'a'..'z') {
        val upperBc = (bc.code - 32).toChar()
        val uidx = input.indexOf(upperBc, from)
        if (uidx >= 0) {
            idx = if (idx >= 0) min(idx, uidx) else uidx
        }
    }

    if (idx < 0) {
        return -1
    }

    return idx
}

fun asciiFuzzyIndex(input: String,
                    pattern: String,
                    caseSensitive: Boolean): Pair<Int, Int> {

    var firstIdx = 0
    var idx = 0
    var lastIdx = 0
    var b: Byte = 0

    for (pidx in pattern.indices) {
        b = pattern[pidx].code.toByte() // Convert Char to Byte
        idx = trySkip(input, caseSensitive, b, idx)
        if (idx < 0) {
            return Pair(-1, -1)
        }
        if (pidx == 0 && idx > 0) {
            // Step back to find the right bonus point
            firstIdx = idx - 1
        }
        lastIdx = idx
        idx++
    }

    // Find the last appearance of the last character of the pattern to limit
    // the search scope. Walk the String directly with String.lastIndexOf for
    // the lowercase byte and (when relevant) the uppercase byte.
    val bc = (b.toInt() and 0xFF).toChar()
    val upperBc: Char? = if (!caseSensitive && bc in 'a'..'z') (bc.code - 32).toChar() else null
    if (upperBc == null) {
        val pos = input.lastIndexOf(bc, input.length - 1)
        return if (pos >= lastIdx) Pair(firstIdx, pos + 1) else Pair(firstIdx, lastIdx + 1)
    }
    // Search backwards from the end for either case, pick the first match
    // at or after lastIdx.
    var pos = input.length
    while (pos > lastIdx) {
        pos--
        val c = input[pos]
        if (c == bc || c == upperBc) {
            return Pair(firstIdx, pos + 1)
        }
    }
    return Pair(firstIdx, lastIdx + 1)
}

fun FuzzyMatchV2(
    caseSensitive: Boolean,
    input: String,
    pattern: String,
): Result {
    // Assume that pattern is given in lowercase if case-insensitive.
    val M = pattern.length
    if (M == 0) {
        return Result(0, 0, 0)
    }
    var N = input.length
    if (M > N) {
        return Result(-1, -1, 0)
    }

    // Since O(nm) algorithm can be prohibitively expensive for large input,
    // we fall back to the greedy algorithm.
    if (input.length > 200) {
        return FuzzyMatchV1(caseSensitive, input, pattern)
    }

    // Phase 1. Optimized search for ASCII string
    val (minIdx, maxIdx) = asciiFuzzyIndex(input, pattern, caseSensitive)
    if (minIdx < 0) {
        return Result(-1, -1, 0)
    }
    N = maxIdx - minIdx

    val H0 = ShortArray(N)
    val C0 = ShortArray(N)
    // Bonus point for each position
    val B = ShortArray(N)
    // The first occurrence of each character in the pattern
    val F = IntArray(M)

    // Phase 2. Calculate bonus for each point
    var maxScore: Int = 0
    var maxScorePos = 0
    var pidx = 0
    var lastIdx = 0
    var pchar0 = pattern[0]
    var pchar = pchar0
    if (!caseSensitive) {
        pchar0 = pchar0.lowercaseChar()
        pchar = pchar.lowercaseChar()
    }
    var prevH0: Int = 0
    var prevClass = initialCharClass
    var inGap = false

    for (off in 0 until N) {
        val rawChar = input[off + minIdx]
        val char: Char
        val cClass: CharClass
        if (caseSensitive) {
            val code = rawChar.code
            if (code <= 127) {
                char = rawChar
                cClass = asciiCharClasses[code]
            } else {
                char = normalizeRune(rawChar)
                cClass = charClassOfNonAscii(char)
            }
        } else {
            val lc = rawChar.lowercaseChar()
            val code = lc.code
            if (code <= 127) {
                char = lc
                cClass = asciiCharClasses[code]
            } else {
                char = normalizeRune(lc)
                cClass = charClassOfNonAscii(char)
            }
        }

        B[off] = bonusMatrix[prevClass][cClass]
        prevClass = cClass

        if (char == pchar) {
            if (pidx < M) {
                F[pidx] = off
                pidx++
                // Once pidx reaches M the next pchar stays at the last
                // pattern char, so hoist that out of the loop.
                pchar = if (pidx < M) pattern[pidx] else pchar
            }
            lastIdx = off
        }

        if (char == pchar0) {
            val score = scoreMatch + B[off] * bonusFirstCharMultiplier
            H0[off] = score.toShort()
            C0[off] = 1
            if (M == 1 && score > maxScore) {
                maxScore = score
                maxScorePos = off
                if (B[off] >= bonusBoundary) {
                    break
                }
            }
            inGap = false
        } else {
            val gap = if (inGap) scoreGapExtension else scoreGapStart
            H0[off] = maxOf(prevH0 + gap, 0).toShort()
            C0[off] = 0
            inGap = true
        }
        prevH0 = H0[off].toInt()
    }

//    if (pidx != M) {
//        return Result(-1, -1, 0)
//    }

    if (M == 1) {
        return Result(minIdx + maxScorePos, minIdx + maxScorePos + 1, maxScore)
    }

    // Phase 3. Fill in score matrix (H)
    // Unlike the original algorithm, we do not allow omission.
    val f0 = F[0] // First matching index
    val width = lastIdx - f0 + 1
    val H = ShortArray(width * M)
    System.arraycopy(H0, f0, H, 0, lastIdx - f0 + 1)

    // Possible length of consecutive chunk at each position.
    val C = ShortArray(width * M)
    System.arraycopy(C0, f0, C, 0, lastIdx - f0 + 1)

    val Fsub = F.copyOfRange(1, F.size)
//    val Psub = pattern.copyOfRange(1, min(1 + Fsub.size, pattern.size))
    val Psub = pattern.substring(1,  min(1 + Fsub.size, pattern.length))

    for (off in Fsub.indices) {
        val f = Fsub[off]
        pchar = Psub[off]
        if (!caseSensitive) {
            pchar = pchar.lowercaseChar()
        }
        val pidx = off + 1
        val row = pidx * width
        inGap = false

        val TsubLen = lastIdx + 1 - f
        val Bsub = B.copyOfRange(f, minOf(f + TsubLen, B.size))
        val CsubOffset = row + f - f0
        val CdiagOffset = row + f - f0 - 1 - width
        val HsubOffset = row + f - f0
        val HdiagOffset = row + f - f0 - 1 - width
        val Hleft = H.copyOfRange(row + f - f0 - 1, minOf(row + f - f0 - 1 + TsubLen, H.size))
        Hleft[0] = 0

        for (offT in 0 until TsubLen) {
            val char = if (!caseSensitive) input[offT + f + minIdx].lowercaseChar() else input[offT + f + minIdx]
            val col = offT + f
            var s1: Short = 0
            var s2: Short
            var consecutive: Short = 0

            if (inGap) {
                s2 = (Hleft[offT] + scoreGapExtension).toShort()
            } else {
                s2 = (Hleft[offT] + scoreGapStart).toShort()
            }

            if (pchar == char) {
                s1 = (H[HdiagOffset + offT] + scoreMatch).toShort()
                var b = Bsub[offT]
                consecutive = (C[CdiagOffset + offT] + 1).toShort()
                if (consecutive > 1) {
                    val fb = B[col - consecutive + 1]
                    // Break consecutive chunk
                    if (b >= bonusBoundary && b > fb) {
                        consecutive = 1
                    } else {
                        b = maxOf(b.toInt(), maxOf(bonusConsecutive, fb.toInt())).toShort()
                    }
                }
                if (s1 + b < s2) {
                    s1 = (s1 + Bsub[offT]).toShort()
                    consecutive = 0
                } else {
                    s1 = (s1 + b).toShort()
                }
            }
            C[CsubOffset + offT] = consecutive

            inGap = s1 < s2
            val score = maxOf(maxOf(s1.toInt(), s2.toInt()), 0)
            if (pidx == M - 1 && score > maxScore) {
                maxScore = score
                maxScorePos = col
            }
            H[HsubOffset + offT] = score.toShort()
        }
    }

    // Start offset we return here is only relevant when begin tiebreak is used.
    // However, finding the accurate offset requires backtracking, and we don't
    // want to pay extra cost for the option that has lost its importance.
    return Result(minIdx + f0, minIdx + maxScorePos + 1, maxScore)
}


fun FuzzyMatchV1(
    caseSensitive: Boolean,
    text: String,
    pattern: String, // Using CharArray for rune equivalent in Kotlin
): Result {
    if (pattern.isEmpty()) {
        return Result(0, 0, 0)
    }

    val idx = asciiFuzzyIndex(text, pattern, caseSensitive)
    if (idx.first < 0) {
        return Result(-1, -1, 0)
    }

    var pidx = 0
    var sidx = -1
    var eidx = -1

    val lenRunes = text.length
    val lenPattern = pattern.length

    for (index in 0 until lenRunes) {
        var char = text.get(index)
        if (!caseSensitive) {
            char = char.lowercaseChar()
        }

        char = normalizeRune(char)
        var pchar = pattern[pidx]
        if (!caseSensitive) {
            pchar = pchar.lowercaseChar()
        }
        if (char == pchar) {
            if (sidx < 0) {
                sidx = index
            }
            if (++pidx == lenPattern) {
                eidx = index + 1
                break
            }
        }
    }

    if (sidx >= 0 && eidx >= 0) {
        pidx--
        for (index in eidx - 1 downTo sidx) {
            var char = text.get(index)
            if (!caseSensitive) {
                char = char.lowercaseChar()
            }

            var pchar = pattern[pidx]
            if (!caseSensitive) {
                pchar = pchar.lowercaseChar()
            }
            if (char == pchar) {
                if (--pidx < 0) {
                    sidx = index
                    break
                }
            }
        }

        val (score, _) = calculateScore(caseSensitive, text, pattern, sidx, eidx)
        return Result(sidx, eidx, score)
    }

    return Result(-1, -1, 0)
}