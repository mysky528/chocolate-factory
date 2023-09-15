@file:Suppress("UnstableApiUsage")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "chocolate-factory"

include(":cocoa-core")
include(":code-interpreter")
include(":cocoa-local-embedding")

include(":dsl:design")
include(":rag-modules:code-splitter")
include(":rag-modules:document")
include(":rag-modules:store-milvus")
include(":rag-modules:store-pinecone")

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        mavenLocal()
    }
}

