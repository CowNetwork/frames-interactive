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
abstract class FrameHandle(protected val plugin: JavaPlugin) : Listener {

    private val listener = FrameHandleListener(this::handleLeftClick)

    init {
        Bukkit.getPluginManager().registerEvents(this.listener, this.plugin)
        Bukkit.getScheduler().runTaskTimerAsynchronously(this.plugin, Runnable(this::tick), 1L, 1L)
    }

    protected open fun tick() {
        val frame = this.getFrame()

        this.updateCursor(frame)

        frame.tick()

        if (!frame.hasUpdatedSections()) return
        val sections = frame.pullUpdatedSections()

        this.renderSections(sections)
    }

    protected abstract fun updateCursor(frame: InteractiveFrame)

    protected abstract fun renderSections(sections: List<Rectangle>)

    protected abstract fun getFrame(): InteractiveFrame

    protected abstract fun handleLeftClick(player: Player)

    // TODO: WASD and Chat

    fun finalize() {
        HandlerList.unregisterAll(this.listener)
    }

}
