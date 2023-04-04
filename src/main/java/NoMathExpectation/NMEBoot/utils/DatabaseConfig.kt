package NoMathExpectation.NMEBoot.utils

import NoMathExpectation.NMEBoot.inventory.modules.reload
import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.value
import net.mamoe.mirai.console.plugin.jvm.reloadPluginConfig
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Random
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.ResultSet

object DatabaseConfig : AutoSavePluginConfig("database") {
    private val path by value("data/NoMathExpectation.NMEBoot/sqlite.db")

    init {
        reload {
            plugin.reloadPluginConfig(this)
        }
    }

    fun load() {
        Database.connect("jdbc:sqlite:$path", "org.sqlite.JDBC")
        transaction {
            SchemaUtils.create(MessageHistoryTable)
        }
    }
}

internal fun <T : Any> String.execAndMap(transform: (ResultSet) -> T): List<T> {
    val result = arrayListOf<T>()
    TransactionManager.current().exec(this) { rs ->
        while (rs.next()) {
            result += transform(rs)
        }
    }
    return result
}

internal typealias SQLRandom = Random