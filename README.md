# Chocolate Factory

> Chocolate Factory 是一款开源的 LLM 应用引擎/LLM 应用框架，旨在帮助您打造 LLM 生成助手。

Tech Stack:

- [Spring Boot](https://spring.io/projects/spring-boot) is a framework for building web applications.
- [Kotlin](https://kotlinlang.org/) is a modern programming language that makes developers happier.

To spike:

- [Kotlin Jupyter](https://github.com/Kotlin/kotlin-jupyter)  Kotlin kernel for Jupyter/IPython.
- [Kotlin Dataframe](https://github.com/Kotlin/dataframe) is typesafe in-memory structured data processing for JVM.
- [KInference](https://github.com/JetBrains-Research/kinference) is a library that makes it possible to execute complex
  ML models (written via ONNX) in Kotlin.

## Todos

- [ ] Workflow
    - Pre-defined workflow: Classify, Clarify, Analyze, Design, Execute
    - Custom workflow by JSON, Yaml, DSL
    - Auto workflow
- [ ] API calling
    - Auto generate API calling code
- [ ] extend code Trigger
    - like ArchGuard code diff for test suggestion

## Usecases

使用场景，基于知识库的：

- 一句话生成前端页面 (特定框架与场景)
    1. Clarify: 确认需求
        - [ ] 匹配现有需求
    2. Design: 设计布局
        - [ ] 搜索历史组件布局
    3. Execute: 生成页面 (React, React Runtime)
        - [ ] Code Interpreter by [Unit Runtime](https://github.com/unit-mesh/unit-runtime)
- 一句话生成后端 API (Ktor, Spring, Kotless）
    1. 确认需求
    2. 输入和输出
    3. 确认数据库表
        - [ ] 搜索历史数据库表
    4. 确认生成的 API
- 一句话生成 SQL 图表
    1. 识别问题领域
    2. 确认与澄清问题
    3. 分析问题与数据
    4. 生成 SQL 与图表 (Kotlin Jupyter, [Lets-Plot](https://github.com/JetBrains/lets-plot-kotlin))
- 一句话生成测试用例数据
    1. 识别需求（bug、代码变更、用例）
    2. 匹配搜索相关的代码
    3. 生成初步测试用例
    4. 生成结果（测试数据集）

## License

This code is distributed under the MPL 2.0 license. See `LICENSE` in this directory.
