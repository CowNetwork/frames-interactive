package network.cow.frames.interactive.spigot

import network.cow.frames.interactive.InteractiveFrame
import network.cow.frames.interactive.spigot.handle.StationaryFrameHandle
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.util.Vector

/**
 * @author Benedikt Wüller
 */
class StationaryContextFrameHandle<T : InteractiveFrame>(private val plugin: JavaPlugin, private val provider: () -> T) {

    private val stationaryFrames = mutableMapOf<Player, StationaryFrameHandle<T>>()

    var updateRange: Double = 10.0
        set(value) {
            field = value
            this.stationaryFrames.values.forEach { it.updateRange = value }
        }

    var maxCursorDistance: Double = 5.0
        set(value) {
            field = value
            this.stationaryFrames.values.forEach { it.maxCursorDistance = value }
        }

    /**
     * The location of the top left hand corner when facing the block the item frame will hang on (i.e. facing opposite of [direction]).
     */
    var topLeftLocation: Vector = Vector()
        set(value) {
            field = value
            this.stationaryFrames.forEach {
                it.value.topLeftLocation = value
                it.value.remove(it.key)
                it.value.spawn(it.key)
            }
        }

    /**
     * The [direction] the frame will face in.
     */
    var direction: BlockFace = BlockFace.NORTH
        set(value) {
            field = value
            this.stationaryFrames.forEach {
                it.value.direction = value
                it.value.remove(it.key)
                it.value.spawn(it.key)
            }
        }

    fun spawn(vararg players: Player) {
        players.filterNot {
            this.stationaryFrames.containsKey(it)
        }.forEach {
            val frame = StationaryFrameHandle<T>(this.plugin, this.provider)
            frame.direction = this.direction
            frame.topLeftLocation = this.topLeftLocation
            frame.spawn(it)
            frame.player = it
            this.stationaryFrames[it] = frame
        }
    }

    fun remove(vararg players: Player) {
        players.forEach {
            val frame = this.stationaryFrames.remove(it) ?: return@forEach
            frame.destroy()
        }
    }

    fun reset() {
        this.stationaryFrames.values.forEach { it.reset() }
    }

    fun reset(vararg players: Player) {
        players.forEach { this.stationaryFrames[it]?.reset() }
    }

    fun destroy() {
        this.remove(*this.stationaryFrames.keys.toTypedArray())
    }

    protected fun finalize() {
        this.destroy()
    }

}
