package com.quickfilesearch.searchbox

import com.quickfilesearch.showErrorNotification
import java.io.BufferedReader
import java.io.InputStreamReader

fun sumMatchingMatrix(matrix: Array<IntArray>) : Int {
    var count = 0
    for (row in matrix) {
        for (item in row) {
            count += item
        }
    }
    return count
}

fun constructCommonCharsMatrix(searchPattern: String, s2: String): Array<IntArray> {
    val m = searchPattern.length
    val n = s2.length

    // Create a table to store lengths of longest common suffixes of substrings.
    // Note that LCSuff[i][j] contains length of longest common suffix of
    // s1[0..i-1] and s2[0..j-1].
    val LCSuff = Array(m + 1) { IntArray(n + 1) }

    // Build LCSuff[m+1][n+1] in bottom up fashion
    for (i in 0..m) {
        for (j in 0..n) {
            if (i == 0 || j == 0) {
                LCSuff[i][j] = 0
            } else if (searchPattern[i - 1] == s2[j - 1]) {
                LCSuff[i][j] = LCSuff[i - 1][j - 1] + 1
            } else {
                LCSuff[i][j] = 0
            }
        }
    }

    return LCSuff
}

fun sortCandidatesBasedOnPattern(pattern: String, candidates: List<String>): List<String> {
    val matchScores = candidates.map { candidate -> constructCommonCharsMatrix(pattern, candidate) }
                                .map { matrix -> sumMatchingMatrix(matrix) }

    val sortedMatchIndices = matchScores.indices.sortedByDescending { matchScores[it] }
    val sortedCandidates = sortedMatchIndices.map { candidates[it] }.toList()
    val sortedScores = matchScores.sortedDescending()
//    for (idx in 0.. sortedScores.size - 1) {
//        println("$idx -> ${sortedCandidates[idx]} - ${sortedScores[idx]}")
//    }

    return ArrayList(sortedCandidates)
}

fun isFzfAvailable(): Boolean {
    return try {
        val process = ProcessBuilder("fzf", "--version")
            .redirectErrorStream(true)
            .start()
        val reader = BufferedReader(InputStreamReader(process.inputStream))
        val output = reader.readLine()
        process.waitFor()
        output != null && output.contains("(")
    } catch (e: Exception) {
        false
    }
}

fun runFzf(customInput: String, query: String, readNofLines: Int) : List<String> {
    try {
        val process = ProcessBuilder("/bin/bash", "-c", "echo \"$customInput\" | fzf -f \"${query}\"")
            .redirectErrorStream(true)
            .start()

        val reader = BufferedReader(InputStreamReader(process.inputStream))
        val output = reader.readLines() // TODO: Read only required lines
        process.waitFor()

        return output
    } catch (e: Exception) {
        e.printStackTrace()
        showErrorNotification("Searching error", "Error running fzf: ${e.message}. Are you sure that fzf is installed?")
    }
    return emptyList<String>()
}
