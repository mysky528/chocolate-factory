package cc.unitmesh.cf.language

import cc.unitmesh.cf.language.TokenType.*
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class TrieTest {
    @Test
    fun simple() {
        val matches = Trie()
        matches.insert(TComplexity, "for ")
        matches.insert(TComplexity, "for(")
        matches.insert(TComplexity, "if ")
        matches.insert(TComplexity, "if(")
        matches.insert(TComplexity, "switch ")
        matches.insert(TComplexity, "while ")
        matches.insert(TComplexity, "else ")
        matches.insert(TComplexity, "|| ")
        matches.insert(TComplexity, "&& ")
        matches.insert(TComplexity, "!= ")
        matches.insert(TComplexity, "== ")

        matches.match("for ".toByteArray()).tokenType shouldBe TComplexity
        matches.match("for ()".toByteArray()).tokenType shouldBe TComplexity
    }

    @Test
    fun comments() {
        val matches = Trie()
        matches.insert(TSlcomment, "//");
        matches.insert(TMlcomment, "/*");

        val content = "//".toByteArray()

        matches.match(content).shouldBe(TrieMatch(TSlcomment, 2, byteArrayOf()))
    }
}