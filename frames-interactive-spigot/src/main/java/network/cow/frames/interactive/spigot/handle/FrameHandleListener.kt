package network.cow.frames.interactive.spigot.handle

import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerInteractEvent

/**
 * @author Benedikt Wüller
 */
class FrameHandleListener(private val handleLeftClick: (Player) -> Unit) : Listener {

    @EventHandler
    private fun onLeftClick(event: PlayerInteractEvent) = this.handleLeftClick(event.player)

    @EventHandler
    private fun onLeftClick(event: PlayerInteractEntityEvent) = this.handleLeftClick(event.player)

}
