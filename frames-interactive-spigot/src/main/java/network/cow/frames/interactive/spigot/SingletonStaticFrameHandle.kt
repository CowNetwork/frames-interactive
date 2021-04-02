package network.cow.frames.interactive.spigot

import network.cow.frames.interactive.Input
import network.cow.frames.interactive.InteractiveFrame
import network.cow.frames.interactive.State
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.plugin.java.JavaPlugin
import java.awt.Rectangle

/**
 * @author Benedikt Wüller
 */
class SingletonStaticFrameHandle(plugin: JavaPlugin, val frame: InteractiveFrame) : StaticFrameHandle(plugin), Listener {

    var controllingPlayer: Player? = null

    init {
        Bukkit.getPluginManager().registerEvents(this, this.plugin)
    }

    @Synchronized
    override fun tick() {
        this.controllingPlayer?.let { this.updateTargetViewportCoordinates(it) }
        super.tick()
    }

    override fun distributeUpdate(player: Player, frame: InteractiveFrame, section: Rectangle) {
        this.sendMapData(frame, section, *this.activePlayers.keys.toTypedArray())
    }

    @Synchronized
    private fun updateTargetViewportCoordinates(player: Player) {
        if (this.frame.state == State.INACTIVE) return
        val position = this.calculateTargetViewportCoordinates(player) ?: return
        this.frame.setViewportCursorPosition(position)
    }

    @Synchronized
    private fun updateInput(input: Input, isActive: Boolean) {
        if (this.frame.state == State.INACTIVE) return
        this.frame.setInputActive(input, isActive)
    }

    override fun getFrame(player: Player): InteractiveFrame = this.frame

    @EventHandler
    private fun onLeftClick(event: PlayerInteractEvent) = this.handleLeftClick(event.player)

    @EventHandler
    private fun onLeftClick(event: PlayerInteractEntityEvent) = this.handleLeftClick(event.player)

    private fun handleLeftClick(player: Player) {
        if (player != this.controllingPlayer) return
        this.calculateTargetViewportCoordinates(player) ?: return

        this.updateInput(Input.INTERACT_PRIMARY, true)
        Bukkit.getScheduler().runTaskLater(this.plugin, Runnable {
            this.updateInput(Input.INTERACT_PRIMARY, false)
        }, 2L)
    }

    // TODO: WASD and Chat

}
