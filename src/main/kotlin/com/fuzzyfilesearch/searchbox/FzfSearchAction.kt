package com.fuzzyfilesearch.searchbox

import ai.grazie.text.find

class FzfSearchAction(val files: List<String>,
                      val caseSensitive: Boolean = false) {
    var previousResult: List<String>? = null
    var previousQuery: String? = null

    fun search(query: String) : List<String> {
        val result : List<String>
        if (previousQuery != null
            && previousResult != null
            && query.find(previousQuery!!)?.start == 0) {

            result = searchFzf(previousResult!!, query)
        } else {
            result = searchFzf(files, query)
        }

        previousQuery = query
        previousResult = result
        return result
    }

    private fun searchFzf(searchFiles: List<String>, query: String) : List<String> {
        var searchFunc = ::FuzzyMatchV2
        if (searchFiles.size > 200) {
            searchFunc = ::FuzzyMatchV1
        }

        val results = mutableListOf<String>()
        val scores = mutableListOf<Int>()
        val queryChars = if (!caseSensitive) query.toCharArray() else query.lowercase().toCharArray()
        for (file in searchFiles) {
            val result = searchFunc(caseSensitive, true, true, file.toCharArray(), queryChars, false)
            if (result.score > 0) {
                results.add(file)
                scores.add(result.score)
            }
        }

        val sortedMatchIndices = scores.indices.sortedByDescending { scores[it] }
        val sortedCandidates = sortedMatchIndices.map { results[it] }.toList()

        return sortedCandidates
    }
}