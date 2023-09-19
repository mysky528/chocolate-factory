package cc.unitmesh.cf.infrastructure.cache

import cc.unitmesh.cf.core.cache.CachableEmbedding
import cc.unitmesh.nlp.embedding.Embedding
import cc.unitmesh.nlp.embedding.EmbeddingProvider
import cc.unitmesh.cf.infrastructure.repository.EmbeddingCacheRepository
import org.springframework.stereotype.Component

@Component
class CachedEmbeddingService(
    val embeddingProvider: EmbeddingProvider,
    val cacheRepository: EmbeddingCacheRepository,
) : CachableEmbedding {
    override fun create(text: String): Embedding {
        val cache = EmbeddingCache(text = text, embedding = embeddingProvider.embed(text))
        val entry = cacheRepository.findByText(text) ?: cacheRepository.save(cache)
        return entry.embedding
    }
}