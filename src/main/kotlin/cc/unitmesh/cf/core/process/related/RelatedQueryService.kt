package cc.unitmesh.cf.core.process.related

/**
 * RelatedQueryService is a service that can find related information for a given name.
 */
interface RelatedQueryService {
    fun findRelated(name: String): Any
}

