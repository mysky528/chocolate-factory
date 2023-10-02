package cc.unitmesh.prompt.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * StrategyItem is the job's strategy config.
 */
@Serializable
sealed class StrategyItem {
    /**
     * ConnectionItem is a config of [cc.unitmesh.connection.BaseConnection],
     * which will be used for [cc.unitmesh.openai.LlmProvider]
     * like temperature, top-p, top-k, presence_penalty, frequency_penalty, stop etc.
     * for example:
     *
     *```yaml
     * - type: connection
     *   value:
     *     - type: range
     *       key: temperature
     *       range: 0.7~1.0
     *       step: 0.1
     *```
     *
     */
    @SerialName("connection")
    @Serializable
    data class ConnectionItem(val value: List<Variable>) : StrategyItem()

    /**
     * RepeatItem is a config of repeat times.
     * for example:
     * 
     *```yaml
     * - type: repeat
     *   value: 3
     *```
     */
    @SerialName("repeat")
    @Serializable
    data class RepeatItem(val value: Int) : StrategyItem()
}
