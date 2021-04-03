package network.cow.frames.interactive.spigot.handle

import network.cow.frames.interactive.InteractiveFrame
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin
import java.awt.Rectangle

/**
 * @author Benedikt Wüller
 */
abstract class FrameHandle<T : InteractiveFrame>(protected val plugin: JavaPlugin, private val frameProvider: () -> T) : Listener {

    private val listener = FrameHandleListener({
        if (!this.shouldUpdate()) return@FrameHandleListener
        this.handleLeftClick(it)
    }, this::invalidate)

    protected var frame: InteractiveFrame = this.frameProvider()

    init {
        Bukkit.getPluginManager().registerEvents(this.listener, this.plugin)
        Bukkit.getScheduler().runTaskTimerAsynchronously(this.plugin, Runnable(this::tick), 1L, 1L)
    }

    protected open fun tick() {
        if (!this.shouldUpdate()) return

        this.updateCursor(this.frame)

        this.frame.tick()

        if (!this.frame.hasUpdatedSections()) return
        val sections = this.frame.pullUpdatedSections()

        this.renderSections(sections)
    }

    protected abstract fun updateCursor(frame: InteractiveFrame)

    protected abstract fun renderSections(sections: List<Rectangle>)

    protected abstract fun handleLeftClick(player: Player)

    protected open fun shouldUpdate(): Boolean = true

    fun reset() {
        this.frame = this.frameProvider()
        this.onReset()
    }

    protected open fun onReset() = Unit

    protected abstract fun invalidate(player: Player)

    protected fun finalize() {
        this.destroy()
    }

    open fun destroy() {
        HandlerList.unregisterAll(this.listener)
    }

}
