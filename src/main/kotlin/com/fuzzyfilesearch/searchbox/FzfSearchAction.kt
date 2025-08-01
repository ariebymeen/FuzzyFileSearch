package com.fuzzyfilesearch.searchbox

import kotlinx.coroutines.*
import ai.grazie.text.find
import java.util.concurrent.Executors
import kotlin.system.measureTimeMillis

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

    fun filterAndSortAndSetPreviousResult(ints: Array<Int>, paths: List<String>, names: List<String>)
    {
        require(ints.size == paths.size) { "Arrays must be of equal length" }
        require(ints.size == names.size) { "Arrays must be of equal length" }

        // Pair each int with its corresponding string, filter out zeros
        val filtered = ints.zip(ints.indices)
                           .filter { (num, _) -> num != 0 }

        // Sort by the integer values while keeping strings paired
        val sorted = filtered.sortedByDescending{ (num, _) -> num }

        // Unzip back into separate arrays
        val (_, sortedIndices) = sorted.unzip()
        previousResultPaths = sortedIndices.map { index -> paths[index] }
        previousResultNames = sortedIndices.map { index -> names[index] }
    }

    private fun searchFzf(searchPaths: List<String>, searchNames: List<String>, query: String) {
        var searchFunc = ::FuzzyMatchV2
        if (searchPaths.size > 200) {
            searchFunc = ::FuzzyMatchV1
        }

        val queryNorm = if (!caseSensitive) query else query.lowercase()

        val scores = Array<Int>(searchPaths.size) { 0 }
        val timeTaken = measureTimeMillis {
           // Process chunks in parallel but collect results in chunk order
            if (multithreaded) {
                runBlocking {
                    searchPaths.indices.chunked(30).map { chunk ->
                        async(cpuDispatcher) {
                            for (index in chunk) {
                                if (searchFileNameOnly) {
                                    scores[index] = searchFunc(caseSensitive, searchNames[index], queryNorm).score
                                } else {
                                    scores[index] = searchFunc(caseSensitive, searchPaths[index], queryNorm).score
                                    scores[index] = (scores[index] + (fileNameMultiplier - 1) * searchFunc(caseSensitive, searchNames[index], queryNorm).score).toInt()
                                }
                            }
                        }
                    }.awaitAll()
                }
            } else {
                for (index in searchPaths.indices) {
                    if (searchFileNameOnly) {
                        scores[index] = searchFunc(caseSensitive, searchNames[index], queryNorm).score
                    } else {
                        scores[index] = searchFunc(caseSensitive, searchPaths[index], queryNorm).score
                        scores[index] = (scores[index] + (fileNameMultiplier - 1) * searchFunc(caseSensitive, searchNames[index], queryNorm).score).toInt()
                    }
                }
            }
        }

        filterAndSortAndSetPreviousResult(scores, searchPaths, searchNames)
    }
}