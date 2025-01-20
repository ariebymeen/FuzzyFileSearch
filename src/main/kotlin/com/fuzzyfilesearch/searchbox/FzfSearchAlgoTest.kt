package com.fuzzyfilesearch.searchbox
import org.junit.Test
import kotlin.test.assertContains

class FzfSearchAlgoTest {
    @Test
    fun `test_fzf_search_algo`() {
        val input1 = "/gradle/wrapper/gradle-wrapper.properties".toCharArray()
        val input2 = "/src/main/kotlin/com/fuzzyfilesearch/actions/SearchFileInPathAction.kt".toCharArray()
        val query = "pat".toCharArray()
        val result1 = FuzzyMatchV1(false, true, true, input1, query, false)
        val result2 = FuzzyMatchV2(false, true, true, input2, query, false)

        assert(result1.score < result2.score)
        println("Result 1 score: ${result1.score}, result score 2: ${result2.score}")
    }
}