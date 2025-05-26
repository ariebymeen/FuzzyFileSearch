package com.fuzzyfilesearch.searchbox

import kotlinx.coroutines.*
import ai.grazie.text.find
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicIntegerArray
import kotlin.system.measureTimeMillis

class FzfSearchAction(val files: List<String>,
                      val caseSensitive: Boolean = false,
                      val multithreaded: Boolean = false) {
    var previousResult: List<String>? = null
    var previousQuery: String? = null
    val mFiles = files

    val cpuDispatcher = Executors.newFixedThreadPool(
        Runtime.getRuntime().availableProcessors()
    ).asCoroutineDispatcher()

    fun search(query: String) : List<String> {
        val result : List<String>
        if (previousQuery != null
            && previousResult != null
            && query.find(previousQuery!!)?.start == 0) {

            result = searchFzf(previousResult!!, query)
        } else {
            result = searchFzf(mFiles, query)
        }

        previousQuery = query
        previousResult = result
        return result
    }

    fun filterAndSort(ints: Array<Int>, strings: List<String>): List<String>
    {
        require(ints.size == strings.size) { "Arrays must be of equal length" }

        // Pair each int with its corresponding string, filter out zeros
        val filtered = ints.zip(strings.asIterable())
                           .filter { (num, _) -> num != 0 }

        // Sort by the integer values while keeping strings paired
        val sorted = filtered.sortedBy { (num, _) -> num }

        // Unzip back into separate arrays
        val (_, sortedStrings) = sorted.unzip()

        return sortedStrings
    }

    private fun searchFzf(searchFiles: List<String>, query: String) : List<String> {
        var searchFunc = ::FuzzyMatchV2
        if (searchFiles.size > 200) {
            searchFunc = ::FuzzyMatchV1
        }

        val queryNorm = if (!caseSensitive) query else query.lowercase()

        val scores = Array<Int>(searchFiles.size) { 0 }
        val timeTaken = measureTimeMillis {
           // Process chunks in parallel but collect results in chunk order
            if (multithreaded) {
                runBlocking {
                    searchFiles.indices.chunked(30).map { chunk ->
                        async(cpuDispatcher) {
                            for (index in chunk) {
                                scores[index] = searchFunc(caseSensitive, searchFiles[index], queryNorm).score
                            }
                        }
                    }.awaitAll()
                }
            } else {
                for (index in searchFiles.indices) {
                    scores[index] = searchFunc(caseSensitive, searchFiles[index], queryNorm).score
                }
            }
        }

        println("Searching ${searchFiles.size} files took ${timeTaken} ms. Mutlithreaded: ${multithreaded}")
        return filterAndSort(scores, searchFiles)
    }
}