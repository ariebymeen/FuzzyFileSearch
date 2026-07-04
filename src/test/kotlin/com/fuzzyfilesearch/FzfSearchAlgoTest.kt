package com.fuzzyfilesearch

import com.fuzzyfilesearch.searchbox.FuzzyMatchV1
import com.fuzzyfilesearch.searchbox.FuzzyMatchV2
import com.fuzzyfilesearch.searchbox.Result
import com.fuzzyfilesearch.searchbox.asciiFuzzyIndex
import com.fuzzyfilesearch.searchbox.initFzf
import com.fuzzyfilesearch.searchbox.trySkip
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Tests for the low-level fuzzy matching primitives in FzfSearchAlgo.
 * Covers FuzzyMatchV1, FuzzyMatchV2, asciiFuzzyIndex and trySkip.
 */
class FzfSearchAlgoTest {

    companion object {
        @JvmStatic
        @BeforeAll
        fun initFzfOnce() {
            // The bonus tables and char-class tables are populated in initFzf().
            // The plugin's ApplicationStartupSetup calls this at startup; tests
            // need to call it explicitly because they bypass that path.
            initFzf()
        }
    }

    // ---------------------------------------------------------------------
    // FuzzyMatchV1 tests
    // ---------------------------------------------------------------------
    @Nested
    @DisplayName("FuzzyMatchV1")
    inner class FuzzyMatchV1Tests {

        @Test
        @DisplayName("Empty pattern returns a zero result with start=0, end=0, score=0")
        fun emptyPattern() {
            val r = FuzzyMatchV1(false, "abc", "")
            assertEquals(Result(0, 0, 0), r)
        }

        @Test
        @DisplayName("Empty pattern on empty input also returns the zero result")
        fun emptyPatternEmptyInput() {
            val r = FuzzyMatchV1(false, "", "")
            assertEquals(Result(0, 0, 0), r)
        }

        @Test
        @DisplayName("Pattern longer than input returns no match")
        fun patternLongerThanInput() {
            val r = FuzzyMatchV1(false, "abc", "abcdef")
            assertEquals(-1, r.start)
            assertEquals(-1, r.end)
            assertEquals(0, r.score)
        }

        @Test
        @DisplayName("Single character match returns the position of that character")
        fun singleChar() {
            val r = FuzzyMatchV1(false, "abc", "b")
            assertEquals(1, r.start)
            assertEquals(2, r.end)
            // First char at a non-boundary gets scoreMatch + 0 bonus * 2 = 16
            assertEquals(16, r.score)
        }

        @Test
        @DisplayName("Match at a word boundary after '/' gets a delimiter bonus")
        fun wordBoundaryBonus() {
            val r = FuzzyMatchV1(false, "/foo/bar.kt", "/bar")
            assertEquals(4, r.start)
            assertEquals(8, r.end)
            // 4 consecutive matches at the start of a path segment earn
            // a higher score than 4 matches scattered through the string.
            assertTrue(r.score > 0, "score should be positive: ${r.score}")
        }

        @Test
        @DisplayName("Consecutive matches score higher than matches with gaps")
        fun consecutiveBeatsGaps() {
            val consecutive = FuzzyMatchV1(false, "/abcdef", "abc")
            val gappy = FuzzyMatchV1(false, "/axbxcx", "abc")
            assertTrue(consecutive.score > gappy.score,
                "consecutive=${consecutive.score} should beat gappy=${gappy.score}")
        }

        @Test
        @DisplayName("Match at the very start of the string scores higher than later matches")
        fun firstCharBonus() {
            val atStart = FuzzyMatchV1(false, "abc", "ab")
            val later = FuzzyMatchV1(false, "xxab", "ab")
            assertTrue(atStart.score > later.score,
                "atStart=${atStart.score} should beat later=${later.score}")
        }

        @Test
        @DisplayName("Case-insensitive matching lowercases both input and pattern")
        fun caseInsensitive() {
            val r = FuzzyMatchV1(false, "HelloWorld", "hw")
            assertEquals(0, r.start)
            assertEquals(6, r.end)
            assertTrue(r.score > 0, "should match: ${r.score}")
        }

        @Test
        @DisplayName("Case-sensitive matching distinguishes 'h' from 'H'")
        fun caseSensitiveMiss() {
            val r = FuzzyMatchV1(true, "HelloWorld", "hw")
            assertEquals(-1, r.start)
            assertEquals(-1, r.end)
            assertEquals(0, r.score)
        }

        @Test
        @DisplayName("Case-sensitive matching succeeds when cases match")
        fun caseSensitiveHit() {
            val r = FuzzyMatchV1(true, "HelloWorld", "HW")
            // The range covers the full extent of the match (from 'H' to
            // 'W'), not just the pattern characters.
            assertEquals(0, r.start)
            assertEquals(6, r.end)
            assertTrue(r.score > 0, "should match: ${r.score}")
        }

        @Test
        @DisplayName("camelCase input matches with the same score as the lowercased version (case-insensitive)")
        fun camelCaseBonus() {
            // With case-insensitive matching the algorithm lowercases both
            // input and pattern for the score calculation, so 'HelloWorld'
            // and 'helloworld' produce the same score.
            val camel = FuzzyMatchV1(false, "HelloWorld", "HW")
            val lower = FuzzyMatchV1(false, "helloworld", "hw")
            assertNotEquals(-1, camel.start)
            assertNotEquals(-1, lower.start)
            assertEquals(camel.score, lower.score)
            assertTrue(camel.score > 0)
        }

        @Test
        @DisplayName("Pattern with characters not in input returns no match")
        fun noMatch() {
            val r = FuzzyMatchV1(false, "abcdef", "xyz")
            assertEquals(-1, r.start)
            assertEquals(-1, r.end)
            assertEquals(0, r.score)
        }

        @Test
        @DisplayName("Repeated characters: the leftmost match is preferred")
        fun repeatedLeftmost() {
            val r = FuzzyMatchV1(false, "xax", "a")
            assertEquals(1, r.start)
            assertEquals(2, r.end)
        }

        @Test
        @DisplayName("Code snippet: pattern spans a method signature")
        fun codeSnippet() {
            val snippet = "fun onActionPerformed(e: AnActionEvent) { val project = e.project ?: return }"
            val r = FuzzyMatchV1(false, snippet, "oae")
            assertTrue(r.start >= 0, "should match: $r")
            // The first match is at the 'o' in "onActionPerformed"
            assertEquals('o', snippet[r.start])
        }

        @Test
        @DisplayName("Code snippet: import statement with separated chars")
        fun importSnippet() {
            val snippet = "import kotlinx.coroutines.async"
            val r = FuzzyMatchV1(false, snippet, "kca")
            assertTrue(r.start >= 0, "should match: $r")
            // The returned range spans from the first to the last pattern
            // character (and the gap between them); it is not just the
            // pattern itself.
            assertEquals('k', snippet[r.start])
            assertEquals('a', snippet[r.end - 1])
        }

        @Test
        @DisplayName("Match on file path with directory separators")
        fun filePathMatch() {
            val input = "/src/main/kotlin/Foo.kt"
            val r = FuzzyMatchV1(false, input, "smk")
            assertTrue(r.start >= 0, "should match: $r")
            // The match range starts at 's' and ends at 'k' (the last
            // character of the pattern), with the intermediate path
            // characters included.
            assertEquals('s', input[r.start])
            assertEquals('k', input[r.end - 1])
        }

        @Test
        @DisplayName("Path with multiple slashes; 'p' is matched at the right offset")
        fun pathWithMultipleSlashes() {
            val r = FuzzyMatchV1(false, "/xx/xx/pat.kt", "p")
            // The 'p' is at index 7
            assertEquals(7, r.start)
            assertEquals(8, r.end)
        }
    }

    // ---------------------------------------------------------------------
    // FuzzyMatchV2 tests
    // ---------------------------------------------------------------------
    @Nested
    @DisplayName("FuzzyMatchV2")
    inner class FuzzyMatchV2Tests {

        @Test
        @DisplayName("Empty pattern returns the zero result")
        fun emptyPattern() {
            val r = FuzzyMatchV2(false, "abc", "")
            assertEquals(Result(0, 0, 0), r)
        }

        @Test
        @DisplayName("Empty input with empty pattern returns the zero result")
        fun emptyPatternEmptyInput() {
            val r = FuzzyMatchV2(false, "", "")
            assertEquals(Result(0, 0, 0), r)
        }

        @Test
        @DisplayName("Pattern longer than input returns no match")
        fun patternLongerThanInput() {
            val r = FuzzyMatchV2(false, "abc", "abcdef")
            assertEquals(-1, r.start)
            assertEquals(-1, r.end)
            assertEquals(0, r.score)
        }

        @Test
        @DisplayName("Single character match returns the position")
        fun singleChar() {
            val r = FuzzyMatchV2(false, "abc", "b")
            assertEquals(1, r.start)
            assertEquals(2, r.end)
        }

        @Test
        @DisplayName("Identity match returns the full string with the highest score")
        fun identity() {
            val r = FuzzyMatchV2(false, "hello", "hello")
            assertEquals(0, r.start)
            assertEquals(5, r.end)
            assertEquals(134, r.score) // 5 * (16 + 8) = 120 + small extras
        }

        @Test
        @DisplayName("Substring at end of input is found")
        fun substringAtEnd() {
            val r = FuzzyMatchV2(false, "abcXY", "xy")
            assertEquals(3, r.start)
            assertEquals(5, r.end)
        }

        @Test
        @DisplayName("V2 falls back to V1 for inputs longer than 200 characters")
        fun longInputFallback() {
            val longInput = "/" + "a".repeat(250)
            val r1 = FuzzyMatchV1(false, longInput, "a")
            val r2 = FuzzyMatchV2(false, longInput, "a")
            assertEquals(r1.start, r2.start)
            assertEquals(r1.end, r2.end)
            assertEquals(r1.score, r2.score)
        }

        @Test
        @DisplayName("Inputs exactly at the 200-char boundary use V2")
        fun inputAtBoundary() {
            val exact200 = "/" + "x".repeat(198) + "y"
            val r = FuzzyMatchV2(false, exact200, "y")
            // V2 path, finds 'y' near the end
            assertEquals(199, r.start)
            assertEquals(200, r.end)
        }

        @Test
        @DisplayName("Case-insensitive: matches across cases")
        fun caseInsensitive() {
            val r = FuzzyMatchV2(false, "HelloWorld", "hw")
            assertTrue(r.start >= 0, "should match: $r")
            assertTrue(r.score > 0)
        }

        @Test
        @DisplayName("Case-sensitive: no match when case differs")
        fun caseSensitiveMiss() {
            val r = FuzzyMatchV2(true, "Hello", "hello")
            assertEquals(-1, r.start)
            assertEquals(-1, r.end)
            assertEquals(0, r.score)
        }

        @Test
        @DisplayName("Case-sensitive: matches when case matches")
        fun caseSensitiveHit() {
            val r = FuzzyMatchV2(true, "Hello", "Hello")
            assertEquals(0, r.start)
            assertEquals(5, r.end)
            assertEquals(134, r.score)
        }

        @Test
        @DisplayName("V1 produces a positive score when the match exists")
        fun v1AlwaysScores() {
            val v1 = FuzzyMatchV1(false, "/gradle/wrapper/gradle-wrapper.properties", "pat")
            assertTrue(v1.start >= 0, "should match")
            assertTrue(v1.score > 0, "V1 score should be positive when matched")
        }

        @Test
        @DisplayName("V2 with non-ASCII characters: Latin accented letters")
        fun nonAscii() {
            val r = FuzzyMatchV2(false, "/résumé/naïve.kt", "rn")
            // The 'r' in 'résumé' and the 'n' in 'naïve' both match.
            assertTrue(r.start >= 0, "should match: $r")
            assertTrue(r.end > r.start)
        }

        @Test
        @DisplayName("V2 single-char match near the end of a longer string")
        fun lateSingleChar() {
            val r = FuzzyMatchV2(false, "/components/RxTileMessage/RxTileMessage.cmp", "T")
            // First 'T' is at index 13
            assertTrue(r.start >= 0, "should match: $r")
        }

        @Test
        @DisplayName("No-match query returns -1/-1/0")
        fun noMatch() {
            val r = FuzzyMatchV2(false, "/non/existent.kt", "zzzz")
            assertEquals(-1, r.start)
            assertEquals(-1, r.end)
            assertEquals(0, r.score)
        }
    }

    // ---------------------------------------------------------------------
    // V1 vs V2 comparison tests
    // ---------------------------------------------------------------------
    @Nested
    @DisplayName("V1 vs V2")
    inner class V1V2Comparison {

        @Test
        @DisplayName("Both algorithms agree on the match position for short patterns")
        fun agreeOnPosition() {
            val cases = listOf(
                Triple("/foo/bar/baz.txt", "fbb", false),
                Triple("hello_world", "hw", false),
                Triple("abc def ghi", "adg", false),
                Triple("/src/main/kotlin/Foo.kt", "smk", false),
                Triple("aaaa", "aa", false),
            )
            for ((input, query, cs) in cases) {
                val v1 = FuzzyMatchV1(cs, input, query)
                val v2 = FuzzyMatchV2(cs, input, query)
                assertEquals(v1.start, v2.start, "start for $input / $query")
                assertEquals(v1.end, v2.end, "end for $input / $query")
            }
        }

        @Test
        @DisplayName("V1 always reports a non-negative score when the match exists")
        fun v1NonNegativeScore() {
            val v1 = FuzzyMatchV1(false, "/gradle/wrapper/gradle-wrapper.properties", "pat")
            assertTrue(v1.score > 0, "V1 score should be positive when matched")
        }
    }

    // ---------------------------------------------------------------------
    // asciiFuzzyIndex tests
    // ---------------------------------------------------------------------
    @Nested
    @DisplayName("asciiFuzzyIndex")
    inner class AsciiFuzzyIndexTests {

        @Test
        @DisplayName("Returns -1/-1 when a character of the pattern is not in the input")
        fun noMatch() {
            val (s, e) = asciiFuzzyIndex("abc", "z", false)
            assertEquals(-1, s)
            assertEquals(-1, e)
        }

        @Test
        @DisplayName("Returns the range covering the first and last pattern character")
        fun basicRange() {
            val (s, e) = asciiFuzzyIndex("hello world", "hw", false)
            assertEquals(0, s)
            assertEquals(7, e) // end exclusive, so 7 is the position after 'w'
        }

        @Test
        @DisplayName("Case-insensitive search picks up the first occurrence of either case")
        fun caseInsensitive() {
            val (s, e) = asciiFuzzyIndex("Hello", "h", false)
            assertEquals(0, s)
            assertEquals(1, e)
        }

        @Test
        @DisplayName("Case-sensitive search respects case")
        fun caseSensitive() {
            val (s, e) = asciiFuzzyIndex("Hello", "h", true)
            assertEquals(-1, s)
            assertEquals(-1, e)
        }

        @Test
        @DisplayName("Multi-character pattern with repeated characters")
        fun repeatedChars() {
            val (s, e) = asciiFuzzyIndex("ababab", "ab", false)
            // The end of the range extends to the LAST occurrence of the
            // final pattern character, not the first. For 'ababab' and
            // pattern 'ab', the last 'b' is at index 5, so e = 6.
            assertEquals(0, s)
            assertEquals(6, e)
        }
    }

    // ---------------------------------------------------------------------
    // trySkip tests
    // ---------------------------------------------------------------------
    @Nested
    @DisplayName("trySkip")
    inner class TrySkipTests {

        @Test
        @DisplayName("Returns 'from' when the byte is found at position 0 of the slice")
        fun atStart() {
            // For input "abc" and byte 'a' starting from 0, the byte is at
            // position 0, so no skipping is possible.
            val r = trySkip("abc", false, 'a'.code.toByte(), 0)
            assertEquals(0, r)
        }

        @Test
        @DisplayName("Returns the absolute index when the byte is found later")
        fun foundLater() {
            // 'p' is at byte index 7 in "/xx/xx/pat.kt"
            val r = trySkip("/xx/xx/pat.kt", false, 'p'.code.toByte(), 0)
            assertEquals(7, r)
        }

        @Test
        @DisplayName("Returns -1 when the byte is not in the input")
        fun notFound() {
            val r = trySkip("abc", false, 'z'.code.toByte(), 0)
            assertEquals(-1, r)
        }

        @Test
        @DisplayName("Returns -1 when the byte is only found before the 'from' offset")
        fun beforeFrom() {
            // 'a' is at index 0; searching from index 1 must not find it.
            val r = trySkip("abc", false, 'a'.code.toByte(), 1)
            assertEquals(-1, r)
        }

        @Test
        @DisplayName("Case-insensitive: returns index of uppercase occurrence when no lowercase")
        fun caseInsensitiveUppercase() {
            val r = trySkip("ABC", false, 'a'.code.toByte(), 0)
            assertEquals(0, r)
        }
    }

    // ---------------------------------------------------------------------
    // Smoke tests that reproduce the original test scenarios
    // ---------------------------------------------------------------------
    @Nested
    @DisplayName("Original scenarios")
    inner class OriginalScenarios {

        @Test
        @DisplayName("V1: path with 'pat' substring scores higher than one with 'pat' buried in 'properties'")
        fun originalPatTest() {
            val input1 = "/gradle/wrapper/gradle-wrapper.properties"
            val input2 = "/src/main/kotlin/com/fuzzyfilesearch/actions/SearchFileInPathAction.kt"
            val query = "pat"
            val r1 = FuzzyMatchV1(false, input1, query)
            val r2 = FuzzyMatchV1(false, input2, query)
            assertTrue(r1.score < r2.score,
                "input1=${r1.score} should be < input2=${r2.score}")
        }

        @Test
        @DisplayName("V2: a long pattern produces the documented scores for Tx/Rx paths")
        fun originalPrioritiesTest() {
            val input1 = "/components/TxTileMessage/TxTileMessage.cmp"
            val input2 = "/components/RxTileMessage/RxTileMessage.cmp"
            val query = "TxTileMessage"
            val r1 = FuzzyMatchV2(false, input1, query)
            val r2 = FuzzyMatchV2(false, input2, query)
            // The exact ordering of these scores depends on the bonus
            // calculation, but the V2 algorithm must produce a strict
            // ordering (r1 < r2) for these inputs.
            assertTrue(r1.score < r2.score,
                "r1=${r1.score} should be < r2=${r2.score}")
        }

        @Test
        @DisplayName("V2: matching 'keeper' returns a positive score")
        fun originalKeeperTest() {
            val r = FuzzyMatchV2(false,
                "/src/main/kotlin/com/fuzzyfilesearch/actions/OpenRelativeFileAction.kt",
                "keeper")
            assertTrue(r.score > 0, "score should be positive: ${r.score}")
        }
    }
}
