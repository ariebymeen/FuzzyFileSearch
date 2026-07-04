package com.fuzzyfilesearch.searchbox

import kotlinx.coroutines.*
import ai.grazie.text.find
import java.util.concurrent.Executors

class FzfSearchAction(filePaths: List<String>,
                      fileNames: List<String>,
                      val caseSensitive: Boolean = false,
                      val multithreaded: Boolean = false,
                      val searchFileNameOnly: Boolean = false,
                      val fileNameMultiplier: Double = 1.0) {
    var previousResultPaths: List<String>? = null
    var previousResultNames: List<String>? = null
    var previousQuery: String? = null
    val mFilePaths = filePaths
    val mFileNames = fileNames
    // Original indices into the array that was searched (mFilePaths/mFileNames
    // for the common path, or previousResultPaths/previousResultNames for the
    // incremental-search path when previousQuery is set). Same length and order
    // as previousResultPaths/previousResultNames. Lets callers map a sorted
    // result back to its source item in O(1) instead of doing an O(M) string
    // equality search per result.
    private var mResultIndices: IntArray = IntArray(0)

    val cpuDispatcher = Executors.newFixedThreadPool(
        Runtime.getRuntime().availableProcessors()
    ).asCoroutineDispatcher()

    fun search(query: String) : Pair<List<String>, List<String>> {
        if (previousQuery != null
            && previousResultPaths != null
            && query.find(previousQuery!!)?.start == 0) {

            searchFzf(previousResultPaths!!, previousResultNames!!, query)
        } else {
            searchFzf(mFilePaths,mFileNames, query)
        }

        return Pair(previousResultPaths!!, previousResultNames!!)
    }

    // Returns a defensive copy of the indices produced by the most recent
    // search. Indices are in the same order as previousResultPaths and refer
    // to positions in the array that was searched (mFilePaths in the common
    // case). Always a fresh copy; callers may mutate freely.
    fun resultIndices(): IntArray = mResultIndices.copyOf()

    fun filterAndSortAndSetPreviousResult(ints: IntArray, paths: List<String>, names: List<String>)
    {
        require(ints.size == paths.size) { "Arrays must be of equal length" }
        require(ints.size == names.size) { "Arrays must be of equal length" }

        // In-place: collect non-zero indices into a single IntArray, sort by
        // score descending, then build the result lists in one pass. This
        // avoids the zip/filter/sortedByDescending/unzip/map pipeline which
        // allocates several intermediate collections per call.
        val n = ints.size
        var nonZero = 0
        for (i in 0 until n) {
            if (ints[i] != 0) nonZero++
        }
        if (nonZero == 0) {
            previousResultPaths = emptyList()
            previousResultNames = emptyList()
            mResultIndices = IntArray(0)
            return
        }
        val indices = IntArray(nonZero)
        var k = 0
        for (i in 0 until n) {
            if (ints[i] != 0) indices[k++] = i
        }
        // Sort indices by score descending. We box into an array of Int
        // because Kotlin's Arrays.sort on IntArray does not accept a
        // Comparator; a boxed sort with a score-keyed comparator is still
        // faster than sortedByDescending on a List<Pair<Int,Int>>.
        val boxed = arrayOfNulls<Int>(nonZero)
        for (i in 0 until nonZero) boxed[i] = indices[i]
        java.util.Arrays.sort(boxed, 0, nonZero,
            java.util.Comparator { a, b -> ints[b!!] - ints[a!!] })
        for (i in 0 until nonZero) indices[i] = boxed[i]!!

        val resultPaths = ArrayList<String>(nonZero)
        val resultNames = ArrayList<String>(nonZero)
        for (i in 0 until nonZero) {
            val idx = indices[i]
            resultPaths.add(paths[idx])
            resultNames.add(names[idx])
        }
        previousResultPaths = resultPaths
        previousResultNames = resultNames
        // Publish a copy of the indices so resultIndices() is independent of
        // any future reuse of the local `indices` array.
        mResultIndices = indices.copyOf()
    }

    private fun searchFzf(searchPaths: List<String>, searchNames: List<String>, query: String) {
        // Pick the right search function once, outside the inner loop. The
        // original re-resolved the method reference on every call.
        val searchFunc: (Boolean, String, String) -> Result =
            if (searchPaths.size > 200) ::FuzzyMatchV1 else ::FuzzyMatchV2

        val queryNorm = if (!caseSensitive) query else query.lowercase()

        // IntArray (primitive) instead of Array<Int> (boxed) — avoids a
        // boxing pass on initialization and on every write below.
        val scores = IntArray(searchPaths.size)
        val nameMultiplierDelta = fileNameMultiplier - 1
        val useNameMultiplier = !searchFileNameOnly && nameMultiplierDelta != 0.0
        val useNameOnly = searchFileNameOnly

        // Process chunks in parallel but collect results in chunk order
        if (multithreaded) {
            runBlocking {
                searchPaths.indices.chunked(30).map { chunk ->
                    async(cpuDispatcher) {
                        for (index in chunk) {
                            if (useNameOnly) {
                                scores[index] = searchFunc(caseSensitive, searchNames[index], queryNorm).score
                            } else {
                                val pathScore = searchFunc(caseSensitive, searchPaths[index], queryNorm).score
                                if (useNameMultiplier) {
                                    val nameScore = searchFunc(caseSensitive, searchNames[index], queryNorm).score
                                    scores[index] = (pathScore + nameMultiplierDelta * nameScore).toInt()
                                } else {
                                    scores[index] = pathScore
                                }
                            }
                        }
                    }
                }.awaitAll()
            }
        } else {
            for (index in searchPaths.indices) {
                if (useNameOnly) {
                    scores[index] = searchFunc(caseSensitive, searchNames[index], queryNorm).score
                } else {
                    val pathScore = searchFunc(caseSensitive, searchPaths[index], queryNorm).score
                    if (useNameMultiplier) {
                        val nameScore = searchFunc(caseSensitive, searchNames[index], queryNorm).score
                        scores[index] = (pathScore + nameMultiplierDelta * nameScore).toInt()
                    } else {
                        scores[index] = pathScore
                    }
                }
            }
        }

        filterAndSortAndSetPreviousResult(scores, searchPaths, searchNames)
    }
}

/**
 * Build the list of `visibleCount` source items corresponding to the leading
 * entries of a sorted FZF result. The sort (in `FzfSearchAction.filterAndSortAndSetPreviousResult`
 * or in the per-action `searchFzf`) produces `sortedPaths` (descending by
 * score) and `sortedIndices` (the original index of each entry into the source
 * list, same length and order). The first `min(sortedPaths.size, visibleCount)`
 * entries of `sortedIndices` are the source indices of the visible results.
 *
 * Replaces the previous `mFilePaths.indexOfFirst { name -> name == file }`
 * pattern which was O(K × M) per keystroke (K visible items, M source items).
 *
 * `fallback` is returned for any out-of-range index (defensive: in practice
 * `sortedIndices` is always in-range, but the caller may pass a slightly
 * different source list from the one indexed, e.g. when the action mutated
 * the source between calls).
 */
fun <T> mapVisibleItems(
    sortedIndices: IntArray,
    visibleCount: Int,
    sourceItems: List<T>,
    fallback: T): List<T> {
    val end = minOf(visibleCount, sortedIndices.size)
    if (end == 0) return emptyList()
    val out = ArrayList<T>(end)
    for (i in 0 until end) {
        val idx = sortedIndices[i]
        out.add(if (idx in sourceItems.indices) sourceItems[idx] else fallback)
    }
    return out
}