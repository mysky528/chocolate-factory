package cc.unitmesh.cf.presentation

import cc.unitmesh.cf.infrastructure.llms.embedding.SentenceTransformersEmbedding
import cc.unitmesh.rag.document.Document
import cc.unitmesh.rag.store.EmbeddingMatch
import cc.unitmesh.store.ElasticsearchStore
import org.archguard.action.checkout.GitCommandManager
import org.archguard.action.checkout.GitSourceSettings
import org.archguard.action.checkout.doCheckout
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.io.path.pathString


/**
 * 1. `code/indexing` will just pass a git url
 * 2. clone the git repo, and run ArchGuard scanner
 * 3. save the code document to the elastic search
 */
@RestController
@RequestMapping("/code")
class CodeDocumentController(
    @Value("\${elasticsearch.uris}")
    private var elasticsearchUrl: String,
) {
    val store: ElasticsearchStore = ElasticsearchStore(elasticsearchUrl)
    val embedding = SentenceTransformersEmbedding()

    companion object {
        private val log = org.slf4j.LoggerFactory.getLogger(CodeDocumentController::class.java)
    }

    @GetMapping
    fun handleQuery(@RequestParam q: String): ResponseEntity<List<EmbeddingMatch<Document>>> {
        val embed = embedding.embed(q)
        val list = store.findRelevant(embed, 5)
        return ResponseEntity.ok(list)
    }

    @PostMapping("/clone")
    fun setupRepository(@RequestBody request: CloneRequest) {
        // validate git url
        val urlRegex = Regex("^(https?|git|ftp)://[^\\s/\$.?#].[^\\s]*\$")
        if (!urlRegex.matches(request.gitUrl)) {
            throw IllegalArgumentException("invalid git url")
        }

        // clone the git repo
        val workdir = createTempDirectory("chocolate")
        val repositoryPath = request.gitUrl.substringAfterLast("/")
        val workingDirectory = File(workdir.pathString, repositoryPath)

        log.info("workdir: {}", workdir)
        val settings = GitSourceSettings(
            request.gitUrl,
            branch = request.branch,
            ref = request.ref,
            workdir = workdir.pathString,
        )

        val git = GitCommandManager(workingDirectory.toString())
        doCheckout(git, settings)
    }

    @PostMapping("/indexing")
    fun indexing() {
        // TODO Download ArchGuard Client and run scan
    }
}

data class CloneRequest(
    val gitUrl: String,
    val branch: String = "master",
    val ref: String = "HEAD",
)