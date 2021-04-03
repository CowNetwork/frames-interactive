package network.cow.frames.interactive.spigot

import network.cow.frames.interactive.HandheldInteractiveFrame
import network.cow.frames.interactive.InteractiveFrame
import network.cow.frames.interactive.spigot.handle.HandheldFrameHandle
import network.cow.frames.interactive.spigot.handle.StationaryFrameHandle
import org.bukkit.Bukkit
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.util.Vector

/**
 * @author Benedikt Wüller
 */
class InteractiveFramePlugin : JavaPlugin(), Listener {

    companion object {
        private lateinit var plugin: InteractiveFramePlugin
        @JvmStatic fun getInstance() = this.plugin
    }

    private val globalFrames = mutableSetOf<StationaryFrameHandle<out InteractiveFrame>>()
    private val globalContextFrames = mutableSetOf<StationaryContextFrameHandle<out InteractiveFrame>>()
    private val handheldFrames = mutableSetOf<HandheldFrameHandle<out HandheldInteractiveFrame>>()

    override fun onEnable() {
        plugin = this
        Bukkit.getPluginManager().registerEvents(this, this)
    }

    override fun onDisable() {
        this.globalFrames.forEach { it.destroy() }
        this.globalFrames.clear()

        this.handheldFrames.forEach { it.destroy() }
        this.handheldFrames.clear()
    }

    fun <T : InteractiveFrame> addGlobalFrame(handle: StationaryFrameHandle<T>) {
        this.globalFrames.add(handle)
        handle.spawn(*Bukkit.getOnlinePlayers().toTypedArray())
    }

    fun <T : InteractiveFrame> removeGlobalFrame(handle: StationaryFrameHandle<T>) {
        this.globalFrames.remove(handle)
        handle.destroy()
    }

    fun <T : InteractiveFrame> spawnGlobalFrame(topLeftLocation: Vector, direction: BlockFace, provider: () -> T) : StationaryFrameHandle<T> {
        val handle = StationaryFrameHandle(this, provider)
        handle.topLeftLocation = topLeftLocation
        handle.direction = direction
        this.addGlobalFrame(handle)
        return handle
    }

    fun <T : InteractiveFrame> addGlobalContextFrame(handle: StationaryContextFrameHandle<T>) {
        this.globalContextFrames.add(handle)
        handle.spawn(*Bukkit.getOnlinePlayers().toTypedArray())
    }

    fun <T : InteractiveFrame> removeGlobalContextFrame(handle: StationaryContextFrameHandle<T>) {
        this.globalContextFrames.remove(handle)
        handle.destroy()
    }

    fun <T : InteractiveFrame> spawnGlobalContextFrame(topLeftLocation: Vector, direction: BlockFace, provider: () -> T) : StationaryContextFrameHandle<T> {
        val handle = StationaryContextFrameHandle(this, provider)
        handle.topLeftLocation = topLeftLocation
        handle.direction = direction
        this.addGlobalContextFrame(handle)
        return handle
    }

    fun <T : HandheldInteractiveFrame> addHandheldFrame(handle: HandheldFrameHandle<T>) = this.handheldFrames.add(handle)

    fun <T : HandheldInteractiveFrame> removeHandheldFrame(handle: HandheldFrameHandle<T>) {
        this.handheldFrames.remove(handle)
        handle.destroy()
    }

    fun <T : HandheldInteractiveFrame> createHandheldFrame(player: Player, name: String, lore: Array<String>, provider: () -> T) : HandheldFrameHandle<T> {
        val handle = HandheldFrameHandle(this, provider, player, name, *lore)
        this.addHandheldFrame(handle)
        return handle
    }

    @EventHandler
    private fun onPlayerJoin(event: PlayerJoinEvent) {
        this.globalFrames.forEach { it.spawn(event.player) }
        this.globalContextFrames.forEach { it.spawn(event.player) }
    }

    @EventHandler
    private fun onPlayerLeave(event: PlayerQuitEvent) {
        val player = event.player

        // Remove the player from any global frames he is controlling.
        this.globalFrames.forEach {
            if (it.player != player) return@forEach
            it.player = null
        }

        // Destroy handheld frames assigned to this player.
        this.handheldFrames.removeIf { frame ->
            if (frame.player != player) return@removeIf false
            frame.destroy()
            return@removeIf true
        }

        // Remove player from all global context frames.
        this.globalContextFrames.forEach {
            it.remove(event.player)
        }
    }

}
