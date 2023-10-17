/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cc.unitmesh.rag.splitter

import cc.unitmesh.nlp.embedding.EncodingTokenizer
import cc.unitmesh.nlp.embedding.OpenAiEncoding
import kotlin.math.max
import kotlin.math.min


/**
 * The TokenTextSplitter class is responsible for splitting a text into smaller chunks based on token size and character length.
 * It uses an encoding tokenizer to encode and decode the text into tokens.
 *
 * @property encoding The encoding tokenizer used to encode and decode the text into tokens. The default tokenizer is OpenAiEncoding.
 * @property chunkSize The target size of each text chunk in tokens. The default value is 800.
 * @property minChunkSizeChars The minimum size of each text chunk in characters. The default value is 350.
 * @property minChunkLengthToEmbed The minimum length of a chunk text to be included in the final chunks. The default value is 5.
 * @property maxNumChunks The maximum number of chunks to generate from a text. The default value is 10000.
 */
class TokenTextSplitter(
    private val encoding: EncodingTokenizer = OpenAiEncoding(),
    // The target size of each text chunk in tokens
    override var chunkSize: Int = 800,
    // The minimum size of each text chunk in characters
    private val minChunkSizeChars: Int = 350,
    // The maximum number of chunks to generate from a text
    private val minChunkLengthToEmbed: Int = 5,
    // The maximum number of chunks to generate from a text
    private val maxNumChunks: Int = 10000,
) : TextSplitter() {
    override fun splitText(text: String): List<String> {
        return split(text, chunkSize)
    }

    private fun split(text: String?, chunkSize: Int): List<String> {
        if (text == null || text.trim { it <= ' ' }.isEmpty()) {
            return ArrayList()
        }
        var tokens = getEncodedTokens(text)
        val chunks: MutableList<String> = ArrayList()
        var numChunks = 0
        while (tokens.isNotEmpty() && numChunks < maxNumChunks) {
            val chunk = tokens.subList(
                0,
                min(chunkSize.toDouble(), tokens.size.toDouble()).toInt()
            )
            var chunkText = decodeTokens(chunk)

            // Skip the chunk if it is empty or whitespace
            if (chunkText.trim { it <= ' ' }.isEmpty()) {
                tokens = tokens.subList(chunk.size, tokens.size)
                continue
            }

            // Find the last period or punctuation mark in the chunk
            val lastPunctuation = max(
                chunkText.lastIndexOf('.'),
                max(chunkText.lastIndexOf('?'), max(chunkText.lastIndexOf('!'), chunkText.lastIndexOf('\n')))
            )

            if (lastPunctuation != -1 && lastPunctuation > minChunkSizeChars) {
                // Truncate the chunk text at the punctuation mark
                chunkText = chunkText.substring(0, lastPunctuation + 1)
            }
            val chunkTextToAppend =
                if (keepSeparator) chunkText.trim { it <= ' ' } else chunkText.replace("\n", " ").trim { it <= ' ' }
            if (chunkTextToAppend.length > minChunkLengthToEmbed) {
                chunks.add(chunkTextToAppend)
            }

            // Remove the tokens corresponding to the chunk text from the remaining tokens
            tokens = tokens.subList(getEncodedTokens(chunkText).size, tokens.size)
            numChunks++
        }

        // Handle the remaining tokens
        if (tokens.isNotEmpty()) {
            val remainingText = decodeTokens(tokens).replace("\n", " ").trim { it <= ' ' }
            if (remainingText.length > minChunkLengthToEmbed) {
                chunks.add(remainingText)
            }
        }

        return chunks
    }

    private fun getEncodedTokens(text: String): List<Int> {
        return encoding.encode(text)
    }

    private fun decodeTokens(tokens: List<Int>): String {
        return encoding.decode(tokens)
    }
}
