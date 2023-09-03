package cc.unitmesh.cf.domains.frontend

import cc.unitmesh.cf.core.process.SolutionDesigner
import cc.unitmesh.cf.domains.frontend.context.FEWorkflow
import cc.unitmesh.cf.domains.frontend.dsl.FEDslContextBuilder
import cc.unitmesh.cf.domains.frontend.model.UiPage
import cc.unitmesh.cf.infrastructure.llms.completion.CompletionProvider
import cc.unitmesh.cf.infrastructure.llms.model.LlmMsg

class FESolutionDesigner(
    private val contextBuilder: FEDslContextBuilder,
    private val completion: CompletionProvider,
) : SolutionDesigner {
    override fun design(domain: String, question: String, histories: List<String>): UiPage {
        val messages = listOf(
            LlmMsg.ChatMessage(LlmMsg.ChatRole.System, FEWorkflow.DESIGN.format()),
            LlmMsg.ChatMessage(LlmMsg.ChatRole.User, question),
        ).filter { it.content.isNotBlank() }

        val completion = completion.createCompletion(messages)
        // parse page name
        return UiPage(
            name = question,
            layout = completion.content
        )
    }
}