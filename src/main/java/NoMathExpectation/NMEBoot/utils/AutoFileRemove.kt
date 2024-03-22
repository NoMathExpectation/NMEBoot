package NoMathExpectation.NMEBoot.utils

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import net.mamoe.mirai.contact.file.AbsoluteFileFolder
import kotlin.time.Duration

object AutoFileRemove {
    private suspend fun deleteFileFolder(fileFolder: AbsoluteFileFolder) {
        val path = fileFolder.absolutePath
        kotlin.runCatching {
            logger.info("正在删除文件$path")
            if (!fileFolder.refresh()) {
                logger.info("文件${path}已被第三方删除")
                return@runCatching
            }
            fileFolder.delete()
            logger.info("文件${path}已删除")
        }.onFailure {
            logger.error("无法删除文件$path")
            logger.error(it)
        }
    }

    private val timedDeleteList: MutableList<Pair<AbsoluteFileFolder, Instant>> = mutableListOf()
    private val listMutex = Mutex()

    private suspend fun routine() {
        while (true) {
            val time = Clock.System.now()
            listMutex.withLock {
                val iterator = timedDeleteList.iterator()
                while (iterator.hasNext()) {
                    val (fileFolder, instant) = iterator.next()
                    if (time > instant) {
                        deleteFileFolder(fileFolder)
                        iterator.remove()
                    }
                }
            }

            delay(5000)
        }
    }

    suspend fun addFileFolder(absoluteFileFolder: AbsoluteFileFolder, instant: Instant) {
        listMutex.withLock {
            timedDeleteList += absoluteFileFolder to instant
        }
        logger.info("文件${absoluteFileFolder.absolutePath}将在${instant}后删除")
    }

    fun launchRoutine() = plugin.launch { routine() }
}

suspend fun AbsoluteFileFolder.deleteAfter(instant: Instant) {
    AutoFileRemove.addFileFolder(this, instant)
}

suspend fun AbsoluteFileFolder.deleteAfter(duration: Duration) {
    deleteAfter(Clock.System.now() + duration)
}