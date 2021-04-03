package network.cow.frames.interactive.spigot

import network.cow.frames.interactive.InteractiveFrame
import network.cow.frames.interactive.spigot.helper.createMapData
import network.cow.frames.interactive.spigot.helper.getMapBounds
import network.cow.protocol.wrappers.WrapperPlayServerMap
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.awt.Dimension
import java.awt.Rectangle
import java.util.WeakHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.ceil

/**
 * @author Benedikt Wüller
 */
abstract class FrameHandleOld(protected val plugin: JavaPlugin) {

    companion object {
        private val MAP_ID_FIELD = AtomicInteger(420_000)

        fun getNextMapId(): Int = MAP_ID_FIELD.getAndIncrement()
    }

    protected val frameDefinitions = WeakHashMap<InteractiveFrame, List<FrameDefinition>>()

    init {
        Bukkit.getScheduler().runTaskTimerAsynchronously(this.plugin, Runnable(this::tick), 1L, 1L)
    }

    protected abstract fun tick()

    protected abstract fun getFrame(player: Player) : InteractiveFrame

    protected fun getFrameCounts(frame: InteractiveFrame) : Dimension {
        return Dimension(
            ceil(frame.viewportDimension.width / 128.0).toInt(),
            ceil(frame.viewportDimension.height / 128.0).toInt()
        )
    }

    protected open fun getFrameDefinitions(frame: InteractiveFrame) : List<FrameDefinition> {
        return this.frameDefinitions.getOrPut(frame) {
            val definitions = mutableListOf<FrameDefinition>()
            val dimensions = this.getFrameCounts(frame)

            for (y in 0 until dimensions.height) {
                for (x in 0 until dimensions.width) {
                    definitions.add(FrameDefinition(getNextMapId()))
                }
            }

            return@getOrPut definitions
        }
    }

    protected fun sendMapData(frame: InteractiveFrame, section: Rectangle, vararg players: Player) {
        val dimensions = this.getFrameCounts(frame)
        val definitions = this.getFrameDefinitions(frame)

        for (y in 0 until dimensions.height) {
            for (x in 0 until dimensions.width) {
                val bounds = getMapBounds(x, y)
                val intersection = section.createIntersection(bounds).bounds
                if (intersection.isEmpty) continue

                val definition = definitions[y * dimensions.width + x]

                // Why the fuck are columns/rows and x/y flipped?
                val packet = WrapperPlayServerMap()
                packet.itemDamage = definition.mapId
                packet.scale = 0
                packet.trackingPosition = false
                packet.isLocked = false
                packet.columns = intersection.x % 128
                packet.rows = intersection.y % 128
                packet.x = intersection.width
                packet.z = intersection.height
                packet.data = createMapData(frame.transformer, frame.getViewport(), intersection)

                players.forEach(packet::sendPacket)
            }
        }
    }



    protected open class FrameDefinition(val mapId: Int)

}
