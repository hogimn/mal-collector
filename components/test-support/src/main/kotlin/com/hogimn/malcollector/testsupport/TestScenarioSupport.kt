package com.hogimn.malcollector.testsupport

import com.hogimn.malcollector.jdbcsupport.JdbcTemplate
import javax.sql.DataSource

class TestScenarioSupport(val dataSource: DataSource) {
    val template = JdbcTemplate(dataSource)

    fun loadTestScenario(name: String) {
        @Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
        val fullSql = this.javaClass.classLoader.getResourceAsStream("$name.sql").reader().readLines()
            .joinToString("\n")

        fullSql.split(";")
            .map { it.trim() }
            .filterNot { it.isBlank() }
            .forEach { template.execute(it) }
    }
}
