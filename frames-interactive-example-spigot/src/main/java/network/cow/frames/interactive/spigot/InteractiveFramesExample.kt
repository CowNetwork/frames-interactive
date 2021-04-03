package network.cow.frames.interactive.spigot

import network.cow.frames.color.MinecraftColorPalette
import network.cow.frames.interactive.HandheldInteractiveFrame
import network.cow.frames.interactive.InteractiveFrame
import network.cow.frames.interactive.example.ExampleFrame
import network.cow.frames.interactive.spigot.handle.HandheldFrameHandle
import network.cow.frames.interactive.spigot.handle.StationaryFrameHandle
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.plugin.java.JavaPlugin
import java.awt.Dimension

/**
 * @author Benedikt Wüller
 */
class InteractiveFramesExample : JavaPlugin(), Listener {

    private lateinit var handle: StationaryFrameHandle<out InteractiveFrame>
    private val transformer = MinecraftColorPalette()
    private val frameProvider = { ExampleFrame(transformer) }

    override fun onEnable() {
        this.handle = StationaryFrameHandle(this, frameProvider)

        Bukkit.getPluginManager().registerEvents(this, this)
    }

    @EventHandler
    private fun onPlayerJoin(event: PlayerJoinEvent) {
        Bukkit.getScheduler().runTaskLater(this, Runnable {
            this.handle.direction = event.player.facing.oppositeFace
            this.handle.topLeftLocation = event.player.eyeLocation.block.getRelative(event.player.facing, 2).location.toVector()
            this.handle.spawn(event.player)

            val handheldHandle = HandheldFrameHandle(this, this.frameProvider, event.player, "§6Cock", "§7Ass")
            event.player.inventory.addItem(handheldHandle.itemStack)
        }, 3 * 20L)

        Bukkit.getScheduler().runTaskLater(this, Runnable {
            this.handle.player = event.player
        }, 5 * 20L)
    }

}
