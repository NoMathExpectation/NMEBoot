package NoMathExpectation.NMEBoot.utils

import NoMathExpectation.NMEBoot.Main
import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.value
import net.mamoe.mirai.console.plugin.jvm.reloadPluginConfig
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseConfig : AutoSavePluginConfig("database") {
    private val path by value("data/NoMathExpectation.NMEBoot/sqlite.db")

    init {
        Main.INSTANCE.reloadPluginConfig(this)
    }

    fun load() {
        Database.connect("jdbc:sqlite:$path", "org.sqlite.JDBC")
        transaction {
            SchemaUtils.create(MessageHistoryTable)
        }
    }
}