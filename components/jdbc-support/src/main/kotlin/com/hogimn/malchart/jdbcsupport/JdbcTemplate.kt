package com.hogimn.malchart.jdbcsupport

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

    fun <T> createWithGeneratedKeys(sql: String, id: (Long) -> T, vararg params: Any) =
        dataSource.connection.use { connection ->
            createWithGeneratedKeys(connection, sql, id, *params)
        }

    fun <T> createWithGeneratedKeys(connection: Connection, sql: String, id: (Long) -> T, vararg params: Any): T {
        return connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS).use { statement ->
            for (i in params.indices) {
                val param = params[i]
                val parameterIndex = i + 1

                when (param) {
                    is String -> statement.setString(parameterIndex, param)
                    is Int -> statement.setInt(parameterIndex, param)
                    is Long -> statement.setLong(parameterIndex, param)
                    is Boolean -> statement.setBoolean(parameterIndex, param)
                    is LocalDate -> statement.setDate(parameterIndex, Date.valueOf(param))

                }
            }
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
        val list = query(sql, { ps -> ps.setLong(1, id) }, mapper)
        return when {
            list.isEmpty() -> null

            else -> list.first()
        }
    }

    fun <T> findBy(sql: String, mapper: (ResultSet) -> T, id: Long) = query(sql, { ps -> ps.setLong(1, id) }, mapper)

    fun <T> findObject(sql: String, mapper: (ResultSet) -> T, vararg params: Any): T? {
        val list = query(sql, { ps -> bindParams(ps, params) }, mapper)
        return when {
            list.isEmpty() -> null
            else -> list.first()
        }
    }

    fun <T> findBy(sql: String, mapper: (ResultSet) -> T, vararg params: Any) =
        query(sql, { ps -> bindParams(ps, params) }, mapper)

    fun update(sql: String, vararg params: Any): Int =
        dataSource.connection.use { connection ->
            update(connection, sql, *params)
        }

    fun update(connection: Connection, sql: String, vararg params: Any): Int {
        return connection.prepareStatement(sql).use { statement ->
            bindParams(statement, params)
            statement.executeUpdate()
        }
    }

    /// USED FOR TESTING

    fun execute(sql: String) {
        dataSource.connection.use { connection ->
            connection.prepareCall(sql).use(CallableStatement::execute)
        }
    }

    fun <T> query(sql: String, params: (PreparedStatement) -> Unit, mapper: (ResultSet) -> T): List<T> {
        val results = ArrayList<T>()

        dataSource.connection.use { connection ->
            connection.prepareStatement(sql).use { statement ->
                params(statement)
                statement.executeQuery().use { rs ->
                    while (rs.next()) {
                        results.add(mapper(rs))
                    }
                }
            }
        }
        return results
    }
}
