package network.cow.frames.interactive.spigot

import network.cow.frames.color.MinecraftColorPalette
import network.cow.frames.interactive.InteractiveFrame
import network.cow.frames.interactive.example.ExampleFrame
import network.cow.frames.interactive.spigot.handle.StationaryFrameHandle
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.plugin.java.JavaPlugin
import java.awt.Dimension

/**
 * @author Benedikt Wüller
 */
class InteractiveFramesExample : JavaPlugin(), Listener {

//    private var handles = mutableListOf<FrameHandleOld>()

    private lateinit var handle: StationaryFrameHandle<out InteractiveFrame>

    override fun onEnable() {
        val transformer = MinecraftColorPalette()
        val frameProvider = { ExampleFrame(Dimension(256, 256), transformer) }

//        this.handles.add(FrameHandleOld(this, frameProvider, Location(Bukkit.getWorlds()[0], 244.0, 64.0, -69.0), BlockFace.NORTH))
//        this.handles.add(FrameHandleOld(this, frameProvider, Location(Bukkit.getWorlds()[0], 242.0, 64.0, -70.0), BlockFace.EAST))
//        this.handles.add(FrameHandleOld(this, frameProvider, Location(Bukkit.getWorlds()[0], 243.0, 64.0, -72.0), BlockFace.SOUTH))
//        this.handles.add(FrameHandleOld(this, frameProvider, Location(Bukkit.getWorlds()[0], 245.0, 64.0, -71.0), BlockFace.WEST))
//        this.handles.forEach(FrameHandleOld::spawn)

//        this.handle = SingletonStaticFrameHandle(this, frameProvider())
        this.handle = StationaryFrameHandle(this, frameProvider)

        Bukkit.getPluginManager().registerEvents(this, this)
    }

    override fun onDisable() {
//        this.handles.forEach(FrameHandleOld::despawn)
    }

    @EventHandler
    private fun onPlayerJoin(event: PlayerJoinEvent) {
        Bukkit.getScheduler().runTaskLater(this, Runnable {
            this.handle.direction = event.player.facing.oppositeFace
            this.handle.topLeftLocation = event.player.eyeLocation.block.getRelative(event.player.facing, 2).location.toVector()
            this.handle.spawn(event.player)
        }, 3 * 20L)

        Bukkit.getScheduler().runTaskLater(this, Runnable {
            this.handle.player = event.player
        }, 5 * 20L)

    }

}
