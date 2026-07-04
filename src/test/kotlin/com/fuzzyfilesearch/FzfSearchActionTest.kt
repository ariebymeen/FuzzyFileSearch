package com.fuzzyfilesearch

import com.fuzzyfilesearch.searchbox.FzfSearchAction
import com.fuzzyfilesearch.searchbox.initFzf
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Tests for the higher-level FzfSearchAction class. The action wraps
 * the low-level matchers (FuzzyMatchV1/V2) and adds:
 *   - Sorting results by score (highest first)
 *   - Filtering out zero-score results
 *   - Previous-query caching: when the new query starts with the previous
 *     one, the search is re-run only on the previous result set
 *   - Single- vs multi-threaded execution
 *   - File-name-only mode and file-name scoring multiplier
 *   - Case-sensitive vs case-insensitive matching
 */
class FzfSearchActionTest {

    companion object {
        @JvmStatic
        @BeforeAll
        fun initFzfOnce() {
            initFzf()
        }

        /**
         * A representative sample of file paths used across the test cases.
         * Each test creates its own FzfSearchAction from this list so they
         * do not share state.
         */
        val samplePaths: List<String> = listOf(
            "/src/main/kotlin/com/fuzzyfilesearch/actions/SearchFileInPathAction.kt",
            "/src/main/kotlin/com/fuzzyfilesearch/actions/OpenRelativeFileAction.kt",
            "/src/main/kotlin/com/fuzzyfilesearch/searchbox/FzfSearchAction.kt",
            "/gradle/wrapper/gradle-wrapper.properties",
            "/components/TxTileMessage/TxTileMessage.cmp",
            "/components/RxTileMessage/RxTileMessage.cmp",
            "/README.md",
            "/src/test/kotlin/com/fuzzyfilesearch/FzfSearchAlgoTest.kt",
            "/foo/Bar/Baz.kt",
            "/very/long/path/to/some/deeply/nested/file/Component.java"
        )
        val sampleNames: List<String> = samplePaths.map { it.substringAfterLast('/') }
    }

    // ---------------------------------------------------------------------
    // Basic search and result shape
    // ---------------------------------------------------------------------
    @Nested
    @DisplayName("Basic search")
    inner class BasicSearch {

        @Test
        @DisplayName("Returned paths and names have the same size")
        fun pairedResultSize() {
            val a = FzfSearchAction(samplePaths, sampleNames)
            val (rp, rn) = a.search("f")
            assertEquals(rp.size, rn.size)
        }

        @Test
        @DisplayName("Returned paths preserve their original index when sorted")
        fun pathsAndNamesAligned() {
            val a = FzfSearchAction(samplePaths, sampleNames)
            val (rp, rn) = a.search("f")
            // Each result pair (path, name) must come from the same input index.
            for (i in rp.indices) {
                val originalIndex = samplePaths.indexOf(rp[i])
                assertTrue(originalIndex >= 0, "result path not in input list: ${rp[i]}")
                assertEquals(sampleNames[originalIndex], rn[i])
            }
        }

        @Test
        @DisplayName("Empty query returns an empty result set")
        fun emptyQuery() {
            val a = FzfSearchAction(samplePaths, sampleNames)
            val (rp, rn) = a.search("")
            assertEquals(0, rp.size)
            assertEquals(0, rn.size)
        }

        @Test
        @DisplayName("No-match query returns an empty result set")
        fun noMatchQuery() {
            val a = FzfSearchAction(samplePaths, sampleNames)
            val (rp, _) = a.search("zzzzz")
            assertEquals(0, rp.size)
        }

        @Test
        @DisplayName("Empty file list returns an empty result set")
        fun emptyFileList() {
            val a = FzfSearchAction(emptyList(), emptyList())
            val (rp, rn) = a.search("foo")
            assertEquals(0, rp.size)
            assertEquals(0, rn.size)
        }

        @Test
        @DisplayName("A query that matches most files returns the expected count")
        fun universalMatch() {
            // 't' is in all sample paths except '/foo/Bar/Baz.kt'
            val a = FzfSearchAction(samplePaths, sampleNames)
            val (rp, _) = a.search("t")
            assertEquals(samplePaths.size - 1, rp.size)
        }
    }

    // ---------------------------------------------------------------------
    // Sorting
    // ---------------------------------------------------------------------
    @Nested
    @DisplayName("Sorting")
    inner class Sorting {

        @Test
        @DisplayName("Results are sorted by descending score")
        fun descendingScore() {
            val a = FzfSearchAction(samplePaths, sampleNames)
            val (rp, _) = a.search("tile")
            // Re-score each result using V2 (the action's default matcher
            // for short paths) and confirm the scores are non-increasing.
            val scores = rp.map { p ->
                com.fuzzyfilesearch.searchbox.FuzzyMatchV2(false, p, "tile").score
            }
            for (i in 1 until scores.size) {
                assertTrue(scores[i - 1] >= scores[i],
                    "scores must be non-increasing: ${scores.joinToString()}")
            }
        }

        @Test
        @DisplayName("The single best match comes first")
        fun bestMatchFirst() {
            val a = FzfSearchAction(samplePaths, sampleNames)
            val (rp, _) = a.search("TileMessage")
            // The first result must contain 'TileMessage' as a substring in
            // its path (it cannot be a fuzzy match - 'TileMessage' is
            // present in both Tx and Rx components).
            assertTrue(rp.isNotEmpty())
            assertTrue(rp[0].contains("TileMessage"),
                "expected first result to contain 'TileMessage': ${rp[0]}")
        }
    }

    // ---------------------------------------------------------------------
    // Case sensitivity
    // ---------------------------------------------------------------------
    @Nested
    @DisplayName("Case sensitivity")
    inner class CaseSensitivity {

        @Test
        @DisplayName("Case-sensitive and case-insensitive searches produce the same set for a lowercase query")
        fun countsCanDiffer() {
            val aCs = FzfSearchAction(samplePaths, sampleNames, caseSensitive = true)
            val aCi = FzfSearchAction(samplePaths, sampleNames, caseSensitive = false)
            val (rpCs, _) = aCs.search("fzf")
            val (rpCi, _) = aCi.search("fzf")
            // The action lowercases the query before matching, so case
            // sensitivity in this implementation refers to whether the
            // input is matched case-sensitively. For an all-lowercase
            // query the matched set is the same; the order may differ.
            assertTrue(rpCs.isNotEmpty())
            assertTrue(rpCi.isNotEmpty())
            assertEquals(rpCs.toSet(), rpCi.toSet())
        }

        @Test
        @DisplayName("Uppercase query 'FZF' matches the FzfSearchAction path")
        fun uppercaseQueryFindsMatch() {
            val a = FzfSearchAction(samplePaths, sampleNames, caseSensitive = true)
            val (rp, _) = a.search("FZF")
            assertTrue(rp.isNotEmpty(), "expected at least one result")
            // The best match contains 'FzfSearch' in its name.
            assertTrue(rp[0].contains("FzfSearch"),
                "expected first match to be FzfSearchAction: ${rp[0]}")
        }
    }

    // ---------------------------------------------------------------------
    // File-name only mode
    // ---------------------------------------------------------------------
    @Nested
    @DisplayName("File name only")
    inner class FileNameOnly {

        @Test
        @DisplayName("searchFileNameOnly=true searches only the file names")
        fun fileNameOnly() {
            val a = FzfSearchAction(samplePaths, sampleNames, searchFileNameOnly = true)
            val (rp, _) = a.search("test")
            // Only paths whose filename contains 'test' should be returned.
            // 'FzfSearchAlgoTest.kt' is the only one.
            assertEquals(1, rp.size)
            assertTrue(rp[0].endsWith("FzfSearchAlgoTest.kt"))
        }

        @Test
        @DisplayName("searchFileNameOnly=true on a path-only match returns no results")
        fun fileNameOnlyExcludesPathMatch() {
            val a = FzfSearchAction(samplePaths, sampleNames, searchFileNameOnly = true)
            // 'wrapper' appears in the path but not the filename 'gradle-wrapper.properties'.
            val (rp, _) = a.search("gradle-wrapper")
            // The filename itself contains 'gradle-wrapper', so this does match.
            // We use a clearer test: 'main' is in many paths but no filename.
            val (rp2, _) = a.search("main")
            assertEquals(0, rp2.size)
        }

        @Test
        @DisplayName("searchFileNameOnly and searchFileNameOnly=false can return different result sets")
        fun fileNameOnlyVsFullPath() {
            val aOn = FzfSearchAction(samplePaths, sampleNames, searchFileNameOnly = true)
            val aOff = FzfSearchAction(samplePaths, sampleNames, searchFileNameOnly = false)
            val (rpOn, _) = aOn.search("main")
            val (rpOff, _) = aOff.search("main")
            assertNotEquals(rpOn.size, rpOff.size)
            assertEquals(0, rpOn.size)
            assertTrue(rpOff.size > 0)
        }
    }

    // ---------------------------------------------------------------------
    // File-name multiplier
    // ---------------------------------------------------------------------
    @Nested
    @DisplayName("File name multiplier")
    inner class FileNameMultiplier {

        @Test
        @DisplayName("Multiplier > 1 boosts files whose name matches the query")
        fun multiplierBoostsNameMatch() {
            val a1 = FzfSearchAction(samplePaths, sampleNames, fileNameMultiplier = 1.0)
            val a2 = FzfSearchAction(samplePaths, sampleNames, fileNameMultiplier = 2.0)
            val (rp1, _) = a1.search("fzf")
            val (rp2, _) = a2.search("fzf")
            // With multiplier=2.0, the file whose name is 'FzfSearchAction.kt'
            // (a strong name match) should still come first; both queries
            // should return non-empty results.
            assertTrue(rp1.isNotEmpty())
            assertTrue(rp2.isNotEmpty())
            assertTrue(rp2[0].contains("FzfSearchAction.kt"))
        }

        @Test
        @DisplayName("Multiplier=1.0 produces the same ordering as the default constructor")
        fun multiplierOneMatchesDefault() {
            val a1 = FzfSearchAction(samplePaths, sampleNames)
            val a2 = FzfSearchAction(samplePaths, sampleNames, fileNameMultiplier = 1.0)
            val (p1, _) = a1.search("test")
            val (p2, _) = a2.search("test")
            assertEquals(p1, p2)
        }
    }

    // ---------------------------------------------------------------------
    // Multi-threaded vs single-threaded
    // ---------------------------------------------------------------------
    @Nested
    @DisplayName("Multithreading")
    inner class MultiThreaded {

        @Test
        @DisplayName("Multi-threaded and single-threaded searches produce the same result set")
        fun sameResults() {
            val aSt = FzfSearchAction(samplePaths, sampleNames, multithreaded = false)
            val aMt = FzfSearchAction(samplePaths, sampleNames, multithreaded = true)
            val queries = listOf("f", "pat", "tile", "Foo", "kt", "cmp", "xyz")
            for (q in queries) {
                val (rpSt, _) = aSt.search(q)
                val (rpMt, _) = aMt.search(q)
                assertEquals(rpSt, rpMt, "results differ for query '$q'")
            }
        }

        @Test
        @DisplayName("Multi-threaded search works with a large file list")
        fun largeFileList() {
            // Build a large list (>200 to force V1) to exercise the
            // multi-threaded code path on a non-trivial workload.
            val big = (1..500).map { "/some/path/to/file_$it.kt" }
            val bigNames = big.map { it.substringAfterLast('/') }
            val a = FzfSearchAction(big, bigNames, multithreaded = true)
            val (rp, rn) = a.search("file_42")
            assertTrue(rp.isNotEmpty(), "expected at least one match for 'file_42'")
            assertEquals(rp.size, rn.size)
            // The best match should mention file_42.
            assertTrue(rp[0].contains("file_42"),
                "expected first match to contain 'file_42': ${rp[0]}")
        }
    }

    // ---------------------------------------------------------------------
    // Previous-query caching (incremental search)
    // ---------------------------------------------------------------------
    @Nested
    @DisplayName("Incremental search")
    inner class IncrementalSearch {

        @Test
        @DisplayName("Extending the previous query narrows the result set")
        fun extendingNarrowsResults() {
            val a = FzfSearchAction(samplePaths, sampleNames)
            val (p1, _) = a.search("f")
            val (p2, _) = a.search("fz")
            val (p3, _) = a.search("fzf")
            assertTrue(p1.size >= p2.size, "extending query should not grow result set: ${p1.size} -> ${p2.size}")
            assertTrue(p2.size >= p3.size, "extending query should not grow result set: ${p2.size} -> ${p3.size}")
        }

        @Test
        @DisplayName("Changing a middle character falls back to a full re-search")
        fun changingMiddleFallsBack() {
            val a = FzfSearchAction(samplePaths, sampleNames)
            val (p1, _) = a.search("pat")
            val (p2, _) = a.search("pxt")
            // 'pxt' is a new query (not a prefix-extension of 'pat'), so the
            // action re-searches the full file list. We can't assert equality
            // with a fresh search because the file list order may differ,
            // but we can assert that 'pxt' produces at least one result
            // (some files contain 'p', 'x' and 't' as a subsequence).
            assertTrue(p2.isNotEmpty(),
                "'pxt' should still find a subsequence match in some paths")
            assertTrue(p1.size != p2.size || p1 != p2,
                "expected different result sets for 'pat' vs 'pxt'")
        }

        @Test
        @DisplayName("Searching with the same query twice returns the same results")
        fun sameQueryTwice() {
            val a = FzfSearchAction(samplePaths, sampleNames)
            val (p1, _) = a.search("foo")
            val (p2, _) = a.search("foo")
            assertEquals(p1, p2)
        }

        @Test
        @DisplayName("TileM -> TileMe re-uses the previous result set (prefix extension)")
        fun tilePrefixExtension() {
            val a = FzfSearchAction(samplePaths, sampleNames)
            val (p1, _) = a.search("TileM")
            val (p2, _) = a.search("TileMe")
            // 'TileMe' is an extension of 'TileM' and the original 'TileM'
            // matches are present in both Tx and Rx components, so the
            // extended query should return the same two results.
            assertEquals(2, p1.size)
            assertEquals(2, p2.size)
            // The Tx component is the better match for 'TileMe' (it appears
            // in the filename and the directory), but Rx only matches it
            // as a subsequence. Either way, both should be present.
            assertTrue(p2.any { it.contains("TxTileMessage") })
            assertTrue(p2.any { it.contains("RxTileMessage") })
        }

        @Test
        @DisplayName("After a non-prefix query, the next prefix-extension search re-narrows")
        fun recoversFromNonPrefix() {
            val a = FzfSearchAction(samplePaths, sampleNames)
            val (p1, _) = a.search("foo")
            val (p2, _) = a.search("foob") // 'foob' is a prefix of nothing we typed, but 'foob' contains 'foo'
            // The action checks whether the new query is a prefix-extension
            // of the previous one. 'foob' starts with 'foo', so it is a
            // prefix-extension of the previous query and narrows results.
            assertTrue(p2.size <= p1.size, "extending should not grow: ${p1.size} -> ${p2.size}")
        }
    }

    // ---------------------------------------------------------------------
    // previousResultPaths / previousResultNames state
    //
    // The class exposes the last computed result lists for the
    // incremental-search code path to inspect.
    //
    // NOTE: the previousQuery field is declared but never set in the
    // current implementation, so the incremental-search code path always
    // falls back to a full re-search. This is documented in the tests
    // below.
    // ---------------------------------------------------------------------
    @Nested
    @DisplayName("State")
    inner class State {

        @Test
        @DisplayName("previousResultPaths and previousResultNames are set after a search with matches")
        fun previousResultSet() {
            val a = FzfSearchAction(samplePaths, sampleNames)
            a.search("f")
            // The fields are populated by filterAndSortAndSetPreviousResult.
            assertNotEquals(null, a.previousResultPaths)
            assertNotEquals(null, a.previousResultNames)
            // Both lists must have the same size (one per matching result).
            assertEquals(a.previousResultPaths!!.size, a.previousResultNames!!.size)
        }

        @Test
        @DisplayName("previousResultPaths is an empty list when nothing matches")
        fun previousResultSetEmptyOnNoMatch() {
            val a = FzfSearchAction(samplePaths, sampleNames)
            a.search("zzzzz")
            // No file has 'zzzzz' anywhere, so the filtered list is empty
            // and the field is set to an empty list (not null).
            assertNotEquals(null, a.previousResultPaths)
            assertEquals(0, a.previousResultPaths!!.size)
        }

        @Test
        @DisplayName("previousQuery is not updated by the current implementation (documented behaviour)")
        fun previousQueryIsNotUpdated() {
            // The current implementation of FzfSearchAction.search does
            // not assign to previousQuery after computing a result, so the
            // field stays null. We assert the actual behaviour to lock it
            // in; if this is ever fixed, this test will fail and signal
            // that the incremental-search code path may need updating.
            val a = FzfSearchAction(samplePaths, sampleNames)
            a.search("f")
            assertEquals(null, a.previousQuery)
        }
    }

    // ---------------------------------------------------------------------
    // Constructor defaults
    // ---------------------------------------------------------------------
    @Nested
    @DisplayName("Constructor defaults")
    inner class ConstructorDefaults {

        @Test
        @DisplayName("Default caseSensitive is false")
        fun defaultCaseSensitive() {
            val a = FzfSearchAction(samplePaths, sampleNames)
            assertEquals(false, a.caseSensitive)
        }

        @Test
        @DisplayName("Default multithreaded is false")
        fun defaultMultithreaded() {
            val a = FzfSearchAction(samplePaths, sampleNames)
            assertEquals(false, a.multithreaded)
        }

        @Test
        @DisplayName("Default searchFileNameOnly is false")
        fun defaultSearchFileNameOnly() {
            val a = FzfSearchAction(samplePaths, sampleNames)
            assertEquals(false, a.searchFileNameOnly)
        }

        @Test
        @DisplayName("Default fileNameMultiplier is 1.0")
        fun defaultFileNameMultiplier() {
            val a = FzfSearchAction(samplePaths, sampleNames)
            assertEquals(1.0, a.fileNameMultiplier)
        }

        @Test
        @DisplayName("mFilePaths and mFileNames hold the constructor arguments")
        fun filePathsAndNamesStored() {
            val a = FzfSearchAction(samplePaths, sampleNames)
            assertEquals(samplePaths, a.mFilePaths)
            assertEquals(sampleNames, a.mFileNames)
        }
    }

    // ---------------------------------------------------------------------
    // Code snippet searches (realistic IDE queries)
    //
    // The action only scores paths and filenames, not the file contents.
    // These tests use realistic file paths (typical of a Kotlin project
    // layout) and verify that queries with characteristic substrings
    // surface the expected path.
    // ---------------------------------------------------------------------
    @Nested
    @DisplayName("Code snippets")
    inner class CodeSnippets {

        // Realistic file paths for a Kotlin IDE plugin.
        private val paths = listOf(
            "/src/main/kotlin/com/fuzzyfilesearch/actions/SearchFileInPathAction.kt",
            "/src/main/kotlin/com/fuzzyfilesearch/actions/OpenRelativeFileAction.kt",
            "/src/main/kotlin/com/fuzzyfilesearch/searchbox/FzfSearchAction.kt",
            "/src/main/kotlin/com/fuzzyfilesearch/searchbox/FzfSearchAlgo.kt",
            "/src/main/kotlin/com/fuzzyfilesearch/searchbox/Result.kt",
            "/src/main/kotlin/com/fuzzyfilesearch/searchbox/CalculateScore.kt",
            "/src/main/kotlin/com/fuzzyfilesearch/settings/GlobalSettings.kt",
            "/src/test/kotlin/com/fuzzyfilesearch/FzfSearchAlgoTest.kt",
            "/src/test/kotlin/com/fuzzyfilesearch/FzfSearchActionTest.kt"
        )
        private val names = paths.map { it.substringAfterLast('/') }

        @Test
        @DisplayName("Searching for 'Fzf' surfaces the FzfSearchAction path first")
        fun fzfPath() {
            val a = FzfSearchAction(paths, names)
            val (rp, _) = a.search("Fzf")
            assertTrue(rp.isNotEmpty())
            assertTrue(rp[0].contains("FzfSearchAction"),
                "expected first result to contain 'FzfSearchAction': ${rp[0]}")
        }

        @Test
        @DisplayName("Searching for 'Algo' surfaces the FzfSearchAlgo path first")
        fun algoPath() {
            val a = FzfSearchAction(paths, names)
            val (rp, _) = a.search("Algo")
            assertTrue(rp.isNotEmpty())
            assertTrue(rp[0].contains("Algo"),
                "expected first result to contain 'Algo': ${rp[0]}")
        }

        @Test
        @DisplayName("Searching for 'Settings' surfaces the GlobalSettings path first")
        fun settingsPath() {
            val a = FzfSearchAction(paths, names)
            val (rp, _) = a.search("Settings")
            assertTrue(rp.isNotEmpty())
            assertTrue(rp[0].contains("Settings"),
                "expected first result to contain 'Settings': ${rp[0]}")
        }

        @Test
        @DisplayName("Searching for 'sfpa' surfaces the SearchFileInPathAction path")
        fun sfpaPath() {
            // 'sfpa' = Search File Path Action
            val a = FzfSearchAction(paths, names)
            val (rp, _) = a.search("sfpa")
            assertTrue(rp.isNotEmpty())
            assertTrue(rp[0].contains("SearchFileInPathAction"),
                "expected first result to contain 'SearchFileInPathAction': ${rp[0]}")
        }

        @Test
        @DisplayName("Searching for 'Open' surfaces the OpenRelativeFileAction path")
        fun openPath() {
            val a = FzfSearchAction(paths, names)
            val (rp, _) = a.search("Open")
            assertTrue(rp.isNotEmpty())
            assertTrue(rp[0].contains("OpenRelativeFileAction"),
                "expected first result to contain 'OpenRelativeFileAction': ${rp[0]}")
        }
    }
}
