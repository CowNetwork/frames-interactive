package network.cow.frames.interactive.spigot

import network.cow.frames.color.MinecraftColorPalette
import network.cow.frames.interactive.example.ExampleFrame
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.block.BlockFace
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.plugin.java.JavaPlugin
import java.awt.Dimension

/**
 * @author Benedikt Wüller
 */
class InteractiveFramesExample : JavaPlugin(), Listener {

    private var handles = mutableListOf<FrameHandle>()

    override fun onEnable() {
        val transformer = MinecraftColorPalette()
        val frameProvider = { ExampleFrame(Dimension(256, 256), transformer) }

        this.handles.add(FrameHandle(this, frameProvider, Location(Bukkit.getWorlds()[0], 244.0, 64.0, -69.0), BlockFace.NORTH))
        this.handles.add(FrameHandle(this, frameProvider, Location(Bukkit.getWorlds()[0], 242.0, 64.0, -70.0), BlockFace.EAST))
        this.handles.add(FrameHandle(this, frameProvider, Location(Bukkit.getWorlds()[0], 243.0, 64.0, -72.0), BlockFace.SOUTH))
        this.handles.add(FrameHandle(this, frameProvider, Location(Bukkit.getWorlds()[0], 245.0, 64.0, -71.0), BlockFace.WEST))
        this.handles.forEach(FrameHandle::spawn)

        Bukkit.getPluginManager().registerEvents(this, this)
    }

    override fun onDisable() {
        this.handles.forEach(FrameHandle::despawn)
    }

    @EventHandler
    private fun onPlayerJoin(event: PlayerJoinEvent) {
        Bukkit.getScheduler().runTaskLater(this, Runnable {
            this.handles.forEach { it.controllingPlayer = event.player }
        }, 10L)
    }

}
