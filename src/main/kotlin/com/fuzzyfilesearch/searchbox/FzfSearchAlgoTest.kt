package com.fuzzyfilesearch.searchbox
import org.junit.jupiter.api.Test


class FzfSearchAlgoTest {
    @Test
    fun `test_fzf_search_algo`() {
        val input1 = "/gradle/wrapper/gradle-wrapper.properties".toCharArray()
        val input2 = "/src/main/kotlin/com/fuzzyfilesearch/actions/SearchFileInPathAction.kt".toCharArray()
        val query = "pat".toCharArray()
        val result1 = FuzzyMatchV1(false, input1, query)
        val result2 = FuzzyMatchV1(false, input2, query)

        assert(result1.score < result2.score)
        println("Result 1 score: ${result1.score}, result score 2: ${result2.score}")

        val result3 = FuzzyMatchV2(false,
            "/src/main/kotlin/com/fuzzyfilesearch/actions/OpenRelativeFileAction.kt".toCharArray(),
            "keeper".toCharArray())
        println("Result 3 score: ${result3.score}")
    }
}