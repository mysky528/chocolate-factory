package cc.unitmesh.cf.domains.sql

import cc.unitmesh.cf.factory.dsl.Dsl
import cc.unitmesh.cf.factory.dsl.DslBase

class AbstractSqlDsl : Dsl {
    override var domain: String = ""

    override lateinit var interpreters: List<DslBase>

}
