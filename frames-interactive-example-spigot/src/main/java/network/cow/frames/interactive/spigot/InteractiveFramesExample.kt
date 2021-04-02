package network.cow.frames.interactive.spigot

import network.cow.frames.color.MinecraftColorPalette
import network.cow.frames.interactive.State
import network.cow.frames.interactive.example.ExampleFrame
import org.bukkit.Bukkit
import org.bukkit.entity.Player
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

    private var player: Player? = null
    private lateinit var handle: StaticFrameHandle

    override fun onEnable() {
        val transformer = MinecraftColorPalette()
        val frameProvider = { ExampleFrame(Dimension(256, 256), transformer) }

//        this.handles.add(FrameHandleOld(this, frameProvider, Location(Bukkit.getWorlds()[0], 244.0, 64.0, -69.0), BlockFace.NORTH))
//        this.handles.add(FrameHandleOld(this, frameProvider, Location(Bukkit.getWorlds()[0], 242.0, 64.0, -70.0), BlockFace.EAST))
//        this.handles.add(FrameHandleOld(this, frameProvider, Location(Bukkit.getWorlds()[0], 243.0, 64.0, -72.0), BlockFace.SOUTH))
//        this.handles.add(FrameHandleOld(this, frameProvider, Location(Bukkit.getWorlds()[0], 245.0, 64.0, -71.0), BlockFace.WEST))
//        this.handles.forEach(FrameHandleOld::spawn)

        this.handle = StaticFrameHandle(frameProvider(), Bukkit.getWorlds().first())

        Bukkit.getScheduler().runTaskTimerAsynchronously(this, Runnable {
            this.player?.let { this.handle.updateTargetViewportCoordinates(it) }
            this.handle.tick()
        }, 1L, 1L)

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
            this.player = event.player
            this.handle.frame.state = State.ACTIVE
//            this.handles.forEach { it.controllingPlayer = event.player }
        }, 3 * 20L)
    }

}
