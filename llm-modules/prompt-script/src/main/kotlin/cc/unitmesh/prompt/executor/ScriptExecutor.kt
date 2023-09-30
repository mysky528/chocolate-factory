package cc.unitmesh.prompt.executor

import cc.unitmesh.cf.core.llms.LlmMsg
import cc.unitmesh.cf.core.llms.MockLlmProvider
import cc.unitmesh.connection.BaseConnection
import cc.unitmesh.connection.MockLlmConnection
import cc.unitmesh.connection.OpenAiConnection
import cc.unitmesh.openai.OpenAiProvider
import cc.unitmesh.prompt.model.Job
import cc.unitmesh.prompt.model.PromptScript
import cc.unitmesh.prompt.template.TemplateCompilerFactory
import cc.unitmesh.prompt.template.TemplateEngineType
import com.charleskorn.kaml.PolymorphismStyle
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import kotlinx.datetime.*
import kotlinx.serialization.decodeFromString
import java.io.File

class ScriptExecutor(val content: String) {
    fun execute() {
        val script: PromptScript = PromptScript.fromString(content) ?: return

        // execute script
        script.jobs.forEach { (name, job) ->
            println("execute job: $name")
            val result = runJob(name, job)

            val resultFileName = createFileName(name, job)
            val resultFile = File(resultFileName)
            resultFile.writeText(result)
        }
    }

    private fun runJob(name: String, job: Job): String {
        val connection = createConnection(job)
        val llmProvider = when (connection) {
            is OpenAiConnection -> OpenAiProvider(connection.apiKey, connection.apiHost)
            is MockLlmConnection -> MockLlmProvider()
            else -> throw Exception("unsupported connection type: ${connection.type}")
        }

        // todo: handle repeat here

        val prompt = createTemplate(job)
        val msgs = TemplateRoleSplitter().split(prompt)
        val messages = toMessages(msgs)

        return llmProvider.completion(messages)
    }

    private fun createFileName(name: String, job: Job): String {
        val currentMoment: Instant = Clock.System.now()
        val datetimeInUtc: LocalDateTime = currentMoment.toLocalDateTime(TimeZone.UTC)
        val timeStr = datetimeInUtc.toString().replace(":", "-")
        val jobName = name.replace(" ", "-")

        return "${jobName}-${timeStr}.txt"
    }

    private fun createTemplate(job: Job): String {
        val ext = job.template.substringAfterLast(".")
        return when (ext) {
            "vm", "vsl", "ft" -> {
                val factory = TemplateCompilerFactory(type = TemplateEngineType.VELOCITY)
                factory.compile(job.template, job.templateDatasource)
            }

            else -> throw Exception("unsupported template type: $ext")
        }
    }

    private fun createConnection(job: Job): BaseConnection {
        val connectionFile: File = File(job.connection)
        val text = connectionFile.readBytes().toString(Charsets.UTF_8)

        val configuration = YamlConfiguration(polymorphismStyle = PolymorphismStyle.Property)
        val connection = Yaml(configuration = configuration).decodeFromString<BaseConnection>(text)
        return connection.convert()
    }
}

private fun toMessages(msgs: Map<String, String>): List<LlmMsg.ChatMessage> {
    return msgs.map {
        LlmMsg.ChatMessage(
            role = LlmMsg.ChatRole.valueOf(it.key),
            content = it.value,
        )
    }
}
