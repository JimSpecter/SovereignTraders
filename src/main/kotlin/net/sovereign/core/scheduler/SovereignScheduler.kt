package net.sovereign.core.scheduler

import org.bukkit.Bukkit
import org.bukkit.plugin.Plugin
import java.util.concurrent.TimeUnit

interface TaskScheduler {
    fun runTask(plugin: Plugin, runnable: Runnable): Any?
    fun runTaskAsynchronously(plugin: Plugin, runnable: Runnable): Any?
    fun runTaskLater(plugin: Plugin, runnable: Runnable, delayTicks: Long): Any?
    fun runTaskLaterAsynchronously(plugin: Plugin, runnable: Runnable, delayTicks: Long): Any?
    fun runTaskTimer(plugin: Plugin, runnable: Runnable, delayTicks: Long, periodTicks: Long): Any?
    fun cancelTask(task: Any?)
}

class BukkitTaskScheduler : TaskScheduler {
    override fun runTask(plugin: Plugin, runnable: Runnable): Any? {
        return Bukkit.getScheduler().runTask(plugin, runnable)
    }
    override fun runTaskAsynchronously(plugin: Plugin, runnable: Runnable): Any? {
        return Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable)
    }
    override fun runTaskLater(plugin: Plugin, runnable: Runnable, delayTicks: Long): Any? {
        return Bukkit.getScheduler().runTaskLater(plugin, runnable, delayTicks)
    }
    override fun runTaskLaterAsynchronously(plugin: Plugin, runnable: Runnable, delayTicks: Long): Any? {
        return Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, runnable, delayTicks)
    }
    override fun runTaskTimer(plugin: Plugin, runnable: Runnable, delayTicks: Long, periodTicks: Long): Any? {
        return Bukkit.getScheduler().runTaskTimer(plugin, runnable, delayTicks, periodTicks)
    }
    override fun cancelTask(task: Any?) {
        if (task == null) return
        if (task is Int) {
            Bukkit.getScheduler().cancelTask(task)
        } else if (task is org.bukkit.scheduler.BukkitTask) {
            task.cancel()
        }
    }
}

class FoliaTaskScheduler : TaskScheduler {
    override fun runTask(plugin: Plugin, runnable: Runnable): Any? {
        return Bukkit.getGlobalRegionScheduler().run(plugin) { runnable.run() }
    }
    override fun runTaskAsynchronously(plugin: Plugin, runnable: Runnable): Any? {
        return Bukkit.getAsyncScheduler().runNow(plugin) { runnable.run() }
    }
    override fun runTaskLater(plugin: Plugin, runnable: Runnable, delayTicks: Long): Any? {
        return Bukkit.getGlobalRegionScheduler().runDelayed(plugin, { runnable.run() }, Math.max(1L, delayTicks))
    }
    override fun runTaskLaterAsynchronously(plugin: Plugin, runnable: Runnable, delayTicks: Long): Any? {
        return Bukkit.getAsyncScheduler().runDelayed(plugin, { runnable.run() }, delayTicks * 50L, TimeUnit.MILLISECONDS)
    }
    override fun runTaskTimer(plugin: Plugin, runnable: Runnable, delayTicks: Long, periodTicks: Long): Any? {
        return Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, { runnable.run() }, Math.max(1L, delayTicks), Math.max(1L, periodTicks))
    }
    override fun cancelTask(task: Any?) {
        if (task == null) return
        try {
            task.javaClass.getMethod("cancel").invoke(task)
        } catch (_: Exception) {}
    }
}

object SovereignScheduler : TaskScheduler {

    var IS_FOLIA: Boolean = try {
        Class.forName("io.papermc.paper.threadedregions.RegionizedServer", false, this::class.java.classLoader)
        true
    } catch (e: Exception) {
        false
    }

    fun determineScheduler(): TaskScheduler {
        return if (IS_FOLIA) FoliaTaskScheduler() else BukkitTaskScheduler()
    }

    private val backend: TaskScheduler by lazy { determineScheduler() }

    override fun runTask(plugin: Plugin, runnable: Runnable): Any? = backend.runTask(plugin, runnable)
    override fun runTaskAsynchronously(plugin: Plugin, runnable: Runnable): Any? = backend.runTaskAsynchronously(plugin, runnable)
    override fun runTaskLater(plugin: Plugin, runnable: Runnable, delayTicks: Long): Any? = backend.runTaskLater(plugin, runnable, delayTicks)
    override fun runTaskLaterAsynchronously(plugin: Plugin, runnable: Runnable, delayTicks: Long): Any? = backend.runTaskLaterAsynchronously(plugin, runnable, delayTicks)
    override fun runTaskTimer(plugin: Plugin, runnable: Runnable, delayTicks: Long, periodTicks: Long): Any? = backend.runTaskTimer(plugin, runnable, delayTicks, periodTicks)
    override fun cancelTask(task: Any?) = backend.cancelTask(task)
}
