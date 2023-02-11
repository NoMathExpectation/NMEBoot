@file:OptIn(ConsoleExperimentalApi::class)

package NoMathExpectation.NMEBoot.utils

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToStream
import kotlinx.serialization.modules.plus
import net.mamoe.mirai.console.data.MultiFilePluginDataStorage
import net.mamoe.mirai.console.data.PluginConfig
import net.mamoe.mirai.console.data.PluginData
import net.mamoe.mirai.console.data.PluginDataHolder
import net.mamoe.mirai.console.util.ConsoleExperimentalApi
import net.mamoe.mirai.message.MessageSerializers
import java.io.File
import java.lang.System.currentTimeMillis

@OptIn(ExperimentalSerializationApi::class)
internal object JsonMultiFilePluginDataStorage : MultiFilePluginDataStorage {
    override val directoryPath = plugin.dataFolderPath
    val configDirectoryPath = plugin.configFolderPath

    private fun getJson(instance: PluginData) = Json {
        serializersModule = MessageSerializers.serializersModule + instance.serializersModule
        prettyPrint = true
        ignoreUnknownKeys = true
        isLenient = true
        allowStructuredMapKeys = true
        encodeDefaults = true

        classDiscriminator = "#class" // for compatibility with interface Item
    }

    private fun getFile(instance: PluginData): File {
        val file = kotlin.run {
            return@run if (instance is PluginConfig) configDirectoryPath else directoryPath
        }.resolve("${instance.saveName}.json").toFile()

        check(!file.isDirectory) { "无法创建用于储存 ${instance::class.qualifiedName ?: "<匿名类>"} 的文件，因为文件名 ${file.name} 已被文件夹占用" }

        return file.apply {
            parentFile?.mkdirs()
            createNewFile()
        }
    }

    override fun load(holder: PluginDataHolder, instance: PluginData) {
        logger.verbose("加载 ${instance::class.qualifiedName ?: "<匿名类>"} 的数据")

        instance.onInit(holder, this)

        val file = getFile(instance)
        val fileString = file.readText().removePrefix("\uFEFF")

        if (fileString.isBlank()) {
            store(holder, instance)
            return
        }

        val json = getJson(instance)
        try {
            json.decodeFromString(instance.updaterSerializer, fileString)
        } catch (e: Throwable) {
            file.copyTo(file.resolveSibling("${file.name}.${currentTimeMillis()}.bak"))
            throw e
        }
    }

    override fun store(holder: PluginDataHolder, instance: PluginData) {
        logger.verbose("保存 ${instance::class.qualifiedName ?: "<匿名类>"} 的数据")

        val file = getFile(instance)
        val json = getJson(instance)

        file.outputStream().use { json.encodeToStream(instance.updaterSerializer, Unit, it) }
    }
}

internal fun PluginData.reloadAsJson() = JsonMultiFilePluginDataStorage.load(plugin, this)