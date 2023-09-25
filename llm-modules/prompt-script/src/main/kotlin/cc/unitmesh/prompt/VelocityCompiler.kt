package cc.unitmesh.prompt

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.apache.velocity.VelocityContext
import org.apache.velocity.app.Velocity
import java.io.File
import java.io.StringWriter


class VelocityCompiler : PromptCompiler {
    private val velocityContext = VelocityContext()

    private fun loadData(fileName: String): JsonObject? {
        return try {
            val fileContent = File(fileName).readText()
            val jsonObject = JsonParser.parseString(fileContent)?.getAsJsonObject()
            jsonObject
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun compile(templatePath: String, dataPath: String): String {
        val obj = loadData(dataPath)!!
        val template = File(templatePath).readText()

        obj.asMap().forEach { (key, u) ->
            velocityContext.put(key, u)
        }

        val sw = StringWriter()
        Velocity.evaluate(velocityContext, sw, "#" + this.javaClass.name, template)
        return sw.toString()
    }
}