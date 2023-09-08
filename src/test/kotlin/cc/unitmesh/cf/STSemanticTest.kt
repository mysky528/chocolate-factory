package cc.unitmesh.cf

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

class STSemanticTest {

    @Test
    fun embed_demo() {
        val semantic = STSemantic.create()
        val embedding = semantic.embed("what is the weather today?")

        println(embedding.joinToString(","))
    }
}