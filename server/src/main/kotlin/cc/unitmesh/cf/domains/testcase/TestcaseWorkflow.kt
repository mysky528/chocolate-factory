package cc.unitmesh.cf.domains.testcase

import cc.unitmesh.cf.core.prompt.UpdatableExample
import cc.unitmesh.cf.core.flow.model.StageContext
import cc.unitmesh.cf.core.flow.Workflow
import cc.unitmesh.cf.core.flow.model.WorkflowResult
import cc.unitmesh.cf.domains.testcase.context.TestcaseVariableResolver
import cc.unitmesh.cf.domains.testcase.flow.TestcaseProblemAnalyzer
import cc.unitmesh.cf.domains.testcase.flow.TestcaseSolutionDesigner
import cc.unitmesh.cf.domains.testcase.flow.TestcaseSolutionReviewer
import cc.unitmesh.cf.core.llms.LlmProvider
import cc.unitmesh.cf.core.llms.TemperatureMode
import cc.unitmesh.cf.core.llms.LlmMsg
import cc.unitmesh.cf.core.flow.model.ChatWebContext
import io.reactivex.rxjava3.core.Flowable
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class TestcaseWorkflow : Workflow() {
    @Autowired
    private lateinit var variableResolver: TestcaseVariableResolver

    @Autowired
    private lateinit var llmProvider: LlmProvider

    /**
     * TODO: add multiple temperatures support
     *
     * In testcase workflow, we have two modes of temperature, but only one mode is active at a time. So we need to
     * cache the creative for the next stage. Once the next stage is executed, we need to clear the cache.
     */
    private var cachedCreative: MutableMap<String, String> = mutableMapOf()

    override val stages: LinkedHashMap<StageContext.Stage, StageContext> = linkedMapOf(
        ANALYZE.stage to ANALYZE,
        DESIGN.stage to DESIGN,
        REVIEW.stage to REVIEW
    )

    override fun execute(prompt: StageContext, chatWebContext: ChatWebContext): Flowable<WorkflowResult> {
        var stage = prompt.stage
        var messages = chatWebContext.messages
        var lastMsg = messages.last().content

        val lowercase = lastMsg.lowercase()

        val isGotoNextStage =
            lowercase == "yes" || lowercase == "y" || lowercase == "ok" || lowercase == "好" || lowercase == "可以"
        if (isGotoNextStage) {
            when (stage) {
                StageContext.Stage.Analyze -> {
                    stage = StageContext.Stage.Design
                    // we need to drop the yes from messages
                    messages = messages.dropLast(1)
                    lastMsg = messages.last().content
                }

                StageContext.Stage.Design -> {
                    stage = StageContext.Stage.Review
                    // we need to drop the yes from messages
                    messages = messages.dropLast(1)
                    lastMsg = messages.last().content
                }

                else -> {}
            }
        }

        val originQuestion = messages.first { it.role == LlmMsg.ChatRole.User.value }.content

        val result = when (stage) {
            StageContext.Stage.Analyze -> {
                val analyzer = TestcaseProblemAnalyzer(llmProvider, variableResolver, TemperatureMode.Default)
                // start creative temperature
                Thread(Runnable {
                    val creativeAnalyzer = TestcaseProblemAnalyzer(llmProvider, variableResolver, TemperatureMode.Creative)
                    val creativeResult = creativeAnalyzer.analyze(
                        domain = "testcase",
                        question = lastMsg
                    )

                    cachedCreative[lastMsg] = creativeResult.content
                }).start()


                // start default temperature
                val output = analyzer.analyze(
                    domain = "testcase",
                    question = lastMsg
                )

                WorkflowResult(
                    currentStage = StageContext.Stage.Analyze,
                    nextStage = StageContext.Stage.Analyze,
                    responseMsg = output.content,
                    resultType = String::class.java.name,
                    result = output.toString()
                )
            }

            StageContext.Stage.Design -> {
                val designer = TestcaseSolutionDesigner(llmProvider, variableResolver,)
                // TODO: update the logic for question pass
                val question = originQuestion

                if(cachedCreative[question] != null) {
                    designer.putCreativeCase(cachedCreative[question]!!)
                }

                val output = designer.design(
                    domain = "testcase",
                    question = question,
                    // TODO: handle for long message, more than 4k
                    histories = messages.map { it.content }
                )

                WorkflowResult(
                    currentStage = StageContext.Stage.Design,
                    nextStage = StageContext.Stage.Design,
                    responseMsg = output.content,
                    resultType = String::class.java.name,
                    result = output.toString()
                )
            }

            StageContext.Stage.Review -> {
                val review = TestcaseSolutionReviewer(llmProvider, variableResolver).review(
                    question = originQuestion,
                    histories = messages.map { it.content }
                )

                WorkflowResult(
                    currentStage = StageContext.Stage.Review,
                    nextStage = StageContext.Stage.Review,
                    responseMsg = review.content,
                    resultType = String::class.java.name,
                    result = review.toString()
                )
            }

            else -> TODO()
        }

        return Flowable.just(result)
    }

    companion object {
        private val log = org.slf4j.LoggerFactory.getLogger(TestcaseWorkflow::class.java)
        val ANALYZE: StageContext = StageContext(
            id = "Testcase Analyse",
            stage = StageContext.Stage.Analyze,
            systemPrompt = """你是一个世界级的质量工程师（Quality assurance），职责是帮助用户识别测试要点，并定义测试目标。
                |要求如下：
                |- 当用户提供的信息不足，你必须提出问题，直到你能够识别测试要点，并定义测试目标。
                |- 当用户的问题比较发散、不明确，请和用户沟通，收集更多的信息，帮助用户明确他们的要点。
                |- 当用户的需求太大，你必须建议用户拆解成多个场景，并建议用户一个个完成。
                |
                |你应该遵循以下的方法：
                |
                |- 基于测试目标，识别系统的各种使用场景和情境。考虑不同用户、操作流程、输入数据等因素。
                |- 将每个场景进一步分解成具体的测试步骤和预期结果。确保每个场景都可以清晰地分解为可执行的测试用例。
                |- 在场景设计过程中，特别关注边界条件和极端情况，因为这些情况通常容易导致问题。确保测试包括了这些边界条件。
                |- 对场景和测试用例进行分类，根据功能的重要性和影响确定测试的优先级。优先测试关键功能，然后再测试次要功能。
                |
            """.trimMargin(),
            updatableExamples = listOf(
                UpdatableExample(
                    question = "用户登录功能",
                    answer = """
                        |请确认以下的测试目标是否符合您的要求。如果符合，请回复 "YES"，如果不符合，请提出你的要求。
                        |```testcases
                        |测试目标：
                        |- 验证用户可以使用有效的用户名和密码成功登录到系统。
                        |- 验证用户在使用无效的用户名或密码时无法登录。
                        |- 验证系统是否正确处理特殊字符的用户名和密码。
                        |```
                    """.trimMargin(),
                    userResponse = "提供的边界条件太少了，能不能再添加一些。",
                    finalOutput = """
                        |请确认以下的测试目标是否符合您的要求。如果符合，请回复 "YES"，如果不符合，请提出你的要求。
                        |```testcases
                        |测试目标：
                        |- 验证用户可以使用有效的用户名和密码成功登录到系统。
                        |- 验证用户在使用无效的用户名或密码时无法登录。
                        |- 验证系统是否正确处理特殊字符的用户名和密码。
                        |- 验证系统对密码长度的限制。
                        |- 验证系统是否记录登录失败的尝试次数，并在一定次数后锁定账户。
                        |```
                    """.trimMargin()
                ),
                UpdatableExample(
                    question = "商品管理",
                    answer = """您想要编写的测试用例功能场景太大，建议拆分为多个场景，再往下进行：商品上架、商品编辑、商品删除、商品检索、商品库存管理、商品排序等。""".trimMargin(),
                    userResponse = "商品管理-商品上架",
                    finalOutput = """
                        |请确认以下的测试目标是否符合您的要求。如果符合，请回复 "YES"，如果不符合，请提出你的要求。
                        |```testcases
                        |测试目标：
                        | - 验证管理员可以成功将商品上架到系统，并能显示已上架的商品。
                        | - 验证系统能正确处理商品的上架日期和状态。
                        | - 验证系统能够正确处理商品上架的价格和库存信息。
                        | - 验证系统能够正确处理上架商品的图片上传和显示。
                        | - 验证系统能够正确处理上架商品的分类和标签。
                        | - 验证系统能够正确计算和显示上架商品的销售统计信息。
                        |``` 
                    """.trimMargin()
                )
            ),
            temperatures = listOf(TemperatureMode.Creative, TemperatureMode.Default),
        )
        val DESIGN: StageContext = StageContext(
            id = "Testcase Design",
            stage = StageContext.Stage.Design,
            systemPrompt = """你是一个资深的质量工程师（Quality assurance）教练，职责是根据多个不同级别的 QA 测试用例，构造出更合理的测试用例。
                |
                |资深 QA 写的测试用例集：
                |```testcases
                |${'$'}{testcases}
                |```
                |
                |${'$'}{creative_cases}
                |
                |最后，你需要将这些测试用例，整理成一个测试计划，使用 markdown 表格输出。
                |markdown 表格格式如下：测试要点,案例名称,案例描述,测试数据,测试步骤,预期结果,案例,属性,案例,等级,执行,方式,执行结果
                | 
            """.trimMargin(),
            temperatures = listOf(TemperatureMode.Default),
        )
        val REVIEW: StageContext = StageContext(
            id = "Testcase Review",
            stage = StageContext.Stage.Review,
            systemPrompt = """你是一个资深的质量工程师（Quality assurance），职责是帮助用户评审测试用例，并根据评审结果，生成更合理的测试用例。
                | 
                |请根据如下的原则，评审测试用例：
                |
                |###
                |基于需求: 从需求出发，设计能有效验证需求的用例；明确不在需求范围内的功能，不设计用例；在需求范围内的功能，不过度设计。
                |场景化：真实用户的使用场景全部覆盖；围绕场景进行更多的探索；第一人称主观视角；按照使用的自然顺序设计用例。
                |描述精准：语⾔准确，没有歧义；描述精炼，留必要信息，去掉⽆关信息；避免⼤段描述，信息分层；描述⻆度关注给⽤户带来的价值，⽽⾮操作步骤。
                |原⼦化：只针对⼀个验证点进⾏设计；如发现验证点多余⼀个，可拆分；⽤例的颗粒度要适宜。
                |可判定：同⼀条件下，不同⼈回归结果应⼀致；在不同时间内，回归结果应⼀致；使⽤满⾜条件的任何数据，回归结果应⼀致。
                |可回归：多个⽤例之间应彼此正交；不重复验证同⼀测试点。
                |可正交：判定准则应明确可判，避免模糊；除⾮业务规则变化，否则判定准则不变；同⼀条件下，多次执⾏结果判定应⼀致。
                |可独⽴：测试用例执行结果不影响其他测试案例的执行；测试用例执行，不依赖其他测试用例。
                |###
                |
                |${'$'}{testcases}
                |
                |现在，你需要根据评审结果，生成更合理的测试用例。
                |
            """.trimMargin(),
            temperatures = listOf(TemperatureMode.Default),
        )
    }
}
