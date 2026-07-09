package com.hogimn.malcollector.jdbcsupport

import java.sql.CallableStatement
import java.sql.Connection
import java.sql.Date
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Statement
import java.sql.Timestamp
import java.time.LocalDate
import java.time.LocalDateTime
import javax.sql.DataSource

class JdbcTemplate(val dataSource: DataSource) {

    private val logger = AppLoggerFactory.getLogger(javaClass)

    fun <T> createWithGeneratedKeys(sql: String, id: (Long) -> T, vararg params: Any) =
        dataSource.connection.use { connection ->
            createWithGeneratedKeys(connection, sql, id, *params)
        }

    fun <T> createWithGeneratedKeys(connection: Connection, sql: String, id: (Long) -> T, vararg params: Any): T {
        logSql(sql, params)
        return connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS).use { statement ->
            bindParams(statement, params)
            statement.executeUpdate()
            val keys = statement.generatedKeys
            keys.next()
            id(keys.getLong(1))
        }
    }

    fun <T> create(sql: String, resultSupplier: () -> T, vararg params: Any) =
        dataSource.connection.use { connection ->
            create(connection, sql, resultSupplier, *params)
        }

    fun <T> create(connection: Connection, sql: String, resultSupplier: () -> T, vararg params: Any): T {
        logSql(sql, params)
        return connection.prepareStatement(sql).use { statement ->
            bindParams(statement, params)
            statement.executeUpdate()
            resultSupplier()
        }
    }

    private fun bindParams(statement: PreparedStatement, params: Array<out Any>) {
        for (i in params.indices) {
            val param = params[i]
            val parameterIndex = i + 1

            when (param) {
                is String -> statement.setString(parameterIndex, param)
                is Int -> statement.setInt(parameterIndex, param)
                is Long -> statement.setLong(parameterIndex, param)
                is Double -> statement.setDouble(parameterIndex, param)
                is Boolean -> statement.setBoolean(parameterIndex, param)
                is LocalDate -> statement.setDate(parameterIndex, Date.valueOf(param))
                is LocalDateTime -> statement.setTimestamp(parameterIndex, Timestamp.valueOf(param))
                else -> statement.setObject(parameterIndex, param)
            }
        }
    }

    fun <T> findObject(sql: String, mapper: (ResultSet) -> T, id: Long): T? {
        val list = query(sql, { ps -> ps.setLong(1, id) }, mapper, arrayOf(id))
        return when {
            list.isEmpty() -> null
            else -> list.first()
        }
    }

    fun <T> findObject(sql: String, mapper: (ResultSet) -> T, vararg params: Any): T? {
        val list = query(sql, { ps -> bindParams(ps, params) }, mapper, params)
        return when {
            list.isEmpty() -> null
            else -> list.first()
        }
    }

    fun <T> findList(sql: String, mapper: (ResultSet) -> T, vararg params: Any): List<T> {
        return query(sql, { ps -> bindParams(ps, params) }, mapper, params)
    }

    fun update(sql: String, vararg params: Any): Int =
        dataSource.connection.use { connection ->
            update(connection, sql, *params)
        }

    fun update(connection: Connection, sql: String, vararg params: Any): Int {
        logSql(sql, params)
        return connection.prepareStatement(sql).use { statement ->
            bindParams(statement, params)
            statement.executeUpdate()
        }
    }

    /// USED FOR TESTING

    fun execute(sql: String) {
        logSql(sql)
        dataSource.connection.use { connection ->
            connection.prepareCall(sql).use(CallableStatement::execute)
        }
    }

    fun <T> query(
        sql: String,
        paramsBind: (PreparedStatement) -> Unit,
        mapper: (ResultSet) -> T,
        logParams: Array<out Any> = emptyArray()
    ): List<T> {
        logSql(sql, logParams)
        val results = ArrayList<T>()

        dataSource.connection.use { connection ->
            connection.prepareStatement(sql).use { statement ->
                paramsBind(statement)
                statement.executeQuery().use { rs ->
                    while (rs.next()) {
                        results.add(mapper(rs))
                    }
                }
            }
        }
        return results
    }

    private fun logSql(sql: String, params: Array<out Any> = emptyArray()) {
        logger.info("[Q] $sql")
        if (params.isNotEmpty()) {
            val paramsString = params.joinToString(",") { "[$it]" }
            logger.info("[QP] $paramsString")
        }
    }
}