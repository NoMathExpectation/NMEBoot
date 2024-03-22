package NoMathExpectation.NMEBoot.simBasedCommand

import NoMathExpectation.NMEBoot.inventory.modules.reload
import NoMathExpectation.NMEBoot.utils.plugin
import kotlinx.serialization.Serializable
import net.mamoe.mirai.console.data.AutoSavePluginData
import net.mamoe.mirai.console.data.value
import net.mamoe.mirai.console.plugin.jvm.reloadPluginData

object PermissionService : AutoSavePluginData("sim/permissions") {
    @Serializable
    class PermissionNode(
        val children: MutableMap<String, PermissionNode> = mutableMapOf(),
        val allow: MutableMap<String, Boolean> = mutableMapOf()
    )

    private val root by value(
        PermissionNode(
            allow = mutableMapOf("console" to true)
        )
    )

    init {
        reload {
            plugin.reloadPluginData(this)
        }
    }

    private fun getNodes(path: String): List<PermissionNode> {
        val nodes = mutableListOf(root)
        var current = root
        for (name in path.split(".")) {
            if (name == "*") {
                break
            }

            val next = current.children[name] ?: break
            nodes += next
            current = next
        }
        nodes.reverse()
        return nodes
    }

    private fun makeNodes(path: String): List<PermissionNode> {
        val nodes = mutableListOf(root)
        var current = root
        for (name in path.split(".")) {
            if (name == "*") {
                break
            }

            val next = current.children.getOrPut(name) { PermissionNode() }
            nodes += next
            current = next
        }
        nodes.reverse()
        return nodes
    }

    fun hasPermission(path: String, vararg ids: String): Boolean {
        val nodes = getNodes(path)
        ids.reversed().forEach { id ->
            nodes.forEach { node ->
                val status = node.allow[id]
                if (status != null) {
                    return status
                }
            }
        }
        return false
    }

    fun setPermission(path: String, id: String, allow: Boolean? = null) {
        val node = makeNodes(path).first()
        if (allow != null) {
            node.allow[id] = allow
        } else {
            node.allow.remove(id)
        }
    }
}