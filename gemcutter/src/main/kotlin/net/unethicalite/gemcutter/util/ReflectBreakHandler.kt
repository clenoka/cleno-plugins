package net.unethicalite.gemcutter.util

import lombok.extern.slf4j.Slf4j
import net.runelite.client.plugins.Plugin
import net.runelite.client.plugins.PluginManager
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Slf4j
@Singleton
class ReflectBreakHandler {
    @Inject
    private var pluginManager: PluginManager? = null
    private var instance: Any? = null
    private var chinMethods: HashMap<String, Method>? = null
    private var chinBreakHandlerInstalled = true
    fun registerPlugin(p: Plugin?, configure: Boolean) {
        performReflection("registerPlugin2", p!!, configure)
    }

    fun registerPlugin(p: Plugin?) {
        performReflection("registerPlugin1", p!!)
    }

    fun unregisterPlugin(p: Plugin?) {
        performReflection("unregisterPlugin1", p!!)
    }

    fun startPlugin(p: Plugin?) {
        performReflection("startPlugin1", p!!)
    }

    fun stopPlugin(p: Plugin?) {
        performReflection("stopPlugin1", p!!)
    }

    fun isBreakActive(p: Plugin?): Boolean {
        val o = performReflection("isBreakActive1", p!!)
        return if (o != null) o as Boolean else false
    }

    fun shouldBreak(p: Plugin?): Boolean {
        val o = performReflection("shouldBreak1", p!!)
        return if (o != null) o as Boolean else false
    }

    fun startBreak(p: Plugin?) {
        performReflection("startBreak1", p!!)
    }

    private fun performReflection(methodName: String, vararg args: Any): Any? {
        var methodName = methodName
        if (checkReflection() && chinMethods!!.containsKey(methodName.lowercase(Locale.getDefault()).also {
                methodName = it
            })) try {
            return chinMethods!![methodName]!!.invoke(instance, *args)
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        } catch (e: InvocationTargetException) {
            e.printStackTrace()
        }
        return null
    }

    private fun checkReflection(): Boolean {
        if (!chinBreakHandlerInstalled) return false
        if (chinMethods != null && instance != null) return true
        chinMethods = HashMap()
        for (p in pluginManager!!.plugins) {
            if (p.javaClass.simpleName.lowercase(Locale.getDefault()) == "chinbreakhandlerplugin") {
                for (f in p.javaClass.declaredFields) {
                    if (f.name.lowercase(Locale.getDefault()) == "chinbreakhandler") {
                        f.isAccessible = true
                        try {
                            instance = f[p]
                            for (m in instance?.javaClass!!.declaredMethods) {
                                m.isAccessible = true
                                chinMethods!![m.name.lowercase(Locale.getDefault()) + m.parameterCount] = m
                            }
                            return true
                        } catch (e: IllegalAccessException) {
                            e.printStackTrace()
                        }
                        return false
                    }
                }
            }
        }
        chinBreakHandlerInstalled = false
        return false
    }
}