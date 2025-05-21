package com.fuzzyfilesearch.searchbox

import kotlinx.coroutines.*
import ai.grazie.text.find
import java.util.concurrent.atomic.AtomicIntegerArray
import kotlin.system.measureTimeMillis

class FzfSearchAction(val files: List<String>,
                      val caseSensitive: Boolean = false) {
    var previousResult: List<CharArray>? = null
    var previousQuery: String? = null
    val mFiles = files.map { file -> file.toCharArray() }

    fun search(query: String) : List<String> {
        val result : List<CharArray>
        if (previousQuery != null
            && previousResult != null
            && query.find(previousQuery!!)?.start == 0) {

            result = searchFzf(previousResult!!, query)
        } else {
            result = searchFzf(mFiles, query)
        }

        previousQuery = query
        previousResult = result
        return result.map { it.joinToString(separator = "") }
    }

    fun filterAndSort(ints: Array<Int>, strings: List<CharArray>): List<CharArray>
    {
        require(ints.size == strings.size) { "Arrays must be of equal length" }

        // Pair each int with its corresponding string, filter out zeros
        val filtered = ints.zip(strings.asIterable())
            .filter { (num, _) -> num != 0 }

        // Sort by the integer values while keeping strings paired
        val sorted = filtered.sortedBy { (num, _) -> num }

        // Unzip back into separate arrays
        val (sortedInts, sortedStrings) = sorted.unzip()

        return sortedStrings
    }

    private fun searchFzf(searchFiles: List<CharArray>, query: String) : List<CharArray> {
        var searchFunc = ::FuzzyMatchV2
        if (searchFiles.size > 200) {
            searchFunc = ::FuzzyMatchV1
        }

        val queryChars = if (!caseSensitive) query.toCharArray() else query.lowercase().toCharArray()

        val result: List<CharArray>
        val scores = Array<Int>(searchFiles.size) { 0 }

        val timeTaken = measureTimeMillis {
        val indices = searchFiles.indices.toList()
        indices.parallelStream().forEach { index ->
            // This handles concurrency internally
            scores[index] = searchFunc(caseSensitive, searchFiles[index], queryChars).score
        }
        }
        println("Sorting files took ${timeTaken} ms")

//        indices.forEach { index -> scores[index] = searchFunc(caseSensitive, searchFiles[index], queryChars).score }


        val timeTaken1 = measureTimeMillis {
            result = filterAndSort(scores, searchFiles)
        }
        println("Sorting files took ${timeTaken1} ms")

        return result

//        val sortedMatchIndices = scores.indices.sortedByDescending { scores[it] }
//        val sortedCandidates = sortedMatchIndices.map { results[it] }.toList()

//        return sortedCandidates
    }
}