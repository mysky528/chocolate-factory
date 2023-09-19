package cc.unitmesh.cf.domains.frontend.context

import cc.unitmesh.cf.core.context.DslContext
import cc.unitmesh.cf.core.context.DslContextBuilder
import cc.unitmesh.cf.core.dsl.InterpreterContext
import cc.unitmesh.cf.infrastructure.cache.CachedEmbeddingService
import cc.unitmesh.nlp.similarity.JaccardSimilarity
import org.springframework.stereotype.Component

@Component
class FEDslContextBuilder(
    cachedEmbedding: CachedEmbeddingService,
) : DslContextBuilder(JaccardSimilarity(), cachedEmbedding) {
    override fun buildFor(domain: InterpreterContext, question: String, chatHistories: String): DslContext {
        return FrontendDslContext(
            similarLayouts = listOf(),
            relatedComponents = listOf(),
        )
    }
}