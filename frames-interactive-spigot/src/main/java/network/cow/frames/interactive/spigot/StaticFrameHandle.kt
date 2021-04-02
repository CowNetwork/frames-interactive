package network.cow.frames.interactive.spigot

import com.comphenix.protocol.wrappers.BlockPosition
import com.comphenix.protocol.wrappers.WrappedBlockData
import network.cow.frames.interactive.InteractiveFrame
import network.cow.frames.interactive.State
import network.cow.protocol.wrappers.WrapperPlayServerBlockChange
import network.cow.protocol.wrappers.WrapperPlayServerEntityDestroy
import network.cow.protocol.wrappers.WrapperPlayServerEntityMetadata
import network.cow.protocol.wrappers.WrapperPlayServerSpawnEntity
import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.MapMeta
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.util.BoundingBox
import org.bukkit.util.Vector
import java.awt.Point
import java.awt.Rectangle
import java.awt.geom.Point2D
import java.util.UUID
import java.util.WeakHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.roundToInt

/**
 * @author Benedikt Wüller
 */
abstract class StaticFrameHandle(plugin: JavaPlugin) : FrameHandle(plugin) {

    var topLeftLocation: Vector = Vector()
    var direction: BlockFace = BlockFace.NORTH
    var maxCursorDistance: Double = 5.0
    var renderDistance: Double = 30.0

    companion object {
        private val ENTITY_ID_FIELD = AtomicInteger(420_000)

        fun getNextEntityId(): Int = ENTITY_ID_FIELD.getAndIncrement()
    }

    protected val activePlayers = WeakHashMap<Player, InteractiveFrame>()

    override fun tick() {
        this.activePlayers.forEach { (player, frame) ->
            if (frame.state == State.INACTIVE) return

            frame.tick()

            if (!frame.hasUpdatedSections()) return
            val sections = frame.pullUpdatedSections()

            sections.forEach { this.distributeUpdate(player, frame, it) }
        }
    }

    protected open fun distributeUpdate(player: Player, frame: InteractiveFrame, section: Rectangle) {
        this.sendMapData(frame, section, player)
    }

    protected fun calculateTargetViewportCoordinates(player: Player): Point? {
        val frame = this.getFrame(player)

        val boundingBox = this.getBoundingBox(frame)
        val rayTrace = boundingBox.rayTrace(player.eyeLocation.toVector(), player.location.direction, this.maxCursorDistance) ?: return null
        val position = rayTrace.hitPosition

        val point = when (this.direction) {
            BlockFace.NORTH -> Point2D.Double(boundingBox.maxX - position.x, boundingBox.maxY - position.y)
            BlockFace.SOUTH -> Point2D.Double(position.x - boundingBox.minX, boundingBox.maxY - position.y)
            BlockFace.EAST -> Point2D.Double(boundingBox.maxZ - position.z, boundingBox.maxY - position.y)
            BlockFace.WEST -> Point2D.Double(position.z - boundingBox.minZ, boundingBox.maxY - position.y)
            else -> Point2D.Double()
        }

        val dimensions = this.getFrameCounts(frame)

        return Point(
            (point.x / dimensions.width * frame.viewportDimension.width).roundToInt(),
            (point.y / dimensions.height * frame.viewportDimension.height).roundToInt()
        )
    }

    private fun getBoundingBox(frame: InteractiveFrame): BoundingBox {
        val dimensions = this.getFrameCounts(frame)

        val topLeft = this.topLeftLocation
        val bottomRight = getMapLocation(this.topLeftLocation, this.direction, dimensions.width - 1, dimensions.height - 1)
        val boundingBoxDepth = 0.0625

        return when(this.direction) {
            BlockFace.NORTH -> BoundingBox(bottomRight.x, bottomRight.y, bottomRight.z + 1.0 - boundingBoxDepth, topLeft.x + 1.0, topLeft.y + 1.0, topLeft.z + 1.0)
            BlockFace.EAST -> BoundingBox(bottomRight.x, bottomRight.y, bottomRight.z, topLeft.x + boundingBoxDepth, topLeft.y + 1.0, topLeft.z + 1.0)
            BlockFace.SOUTH -> BoundingBox(topLeft.x, bottomRight.y, topLeft.z, bottomRight.x + 1.0, topLeft.y + 1.0, bottomRight.z + boundingBoxDepth)
            BlockFace.WEST -> BoundingBox(topLeft.x + 1.0 - boundingBoxDepth, bottomRight.y, topLeft.z, bottomRight.x + 1.0, topLeft.y + 1.0, bottomRight.z + 1.0)
            else -> BoundingBox()
        }
    }

    fun spawn(vararg players: Player) {
        val (yaw, pitch, data) = when (this.direction) {
            BlockFace.NORTH -> Triple(180.0f, 0.0f, 2)
            BlockFace.EAST -> Triple(-90.0f, 0.0f, 5)
            BlockFace.SOUTH -> Triple(0.0f, 0.0f, 3)
            BlockFace.WEST -> Triple(90.0f, 0.0f, 4)
            else -> Triple(0.0f, 0.0f, 0)
        }

        players.filterNot { this.activePlayers.containsKey(it) }.forEach { player ->
            val frame = this.getFrame(player)
            val dimensions = this.getFrameCounts(frame)
            val definitions = this.getFrameDefinitions(frame) as List<FrameDefinition>

            (0 until dimensions.height).forEach { y ->
                (0 until dimensions.width).forEach { x ->
                    val index = y * dimensions.width + x
                    val definition = definitions[index]
                    val location = getMapLocation(this.topLeftLocation, this.direction, x, y)

                    val spawnPacket = WrapperPlayServerSpawnEntity()
                    spawnPacket.entityID = definition.entityId
                    spawnPacket.uniqueId = definition.uuid
                    spawnPacket.type = EntityType.ITEM_FRAME
                    spawnPacket.x = location.x
                    spawnPacket.y = location.y
                    spawnPacket.z = location.z
                    spawnPacket.yaw = yaw
                    spawnPacket.pitch = pitch
                    spawnPacket.objectData = data

                    val itemStack = ItemStack(Material.FILLED_MAP)
                    val itemMeta = itemStack.itemMeta as MapMeta
                    itemMeta.mapId = definition.mapId
                    itemStack.itemMeta = itemMeta

                    val watcher = getDataWatcher(EntityType.ITEM_FRAME)
                    watcher.getWatchableObject(7).setValue(itemStack, true)

                    val metaPacket = WrapperPlayServerEntityMetadata()
                    metaPacket.entityID = definition.entityId
                    metaPacket.metadata = watcher.watchableObjects

                    spawnPacket.sendPacket(player)
                    metaPacket.sendPacket(player)
                }
            }

            this.activePlayers[player] = frame
            this.sendMapData(frame, Rectangle(Point(), frame.viewportDimension), player)
        }
    }

    fun despawn(vararg players: Player) {
        players.filter { this.activePlayers.containsKey(it) }.forEach { player ->
            val frame = this.activePlayers[player] ?: return@forEach
            val dimensions = this.getFrameCounts(frame)
            val definitions = this.getFrameDefinitions(frame) as List<FrameDefinition>

            (0 until dimensions.height).forEach { y ->
                (0 until dimensions.width).forEach { x ->
                    val index = y * dimensions.width + x
                    val definition = definitions[index]
                    val location = getMapLocation(this.topLeftLocation, this.direction, x, y)

                    val despawnPacket = WrapperPlayServerEntityDestroy()
                    despawnPacket.setEntityIds(intArrayOf(definition.entityId))
                    despawnPacket.sendPacket(player)
                }
            }

            this.activePlayers.remove(player)
        }
    }

    override fun getFrameDefinitions(frame: InteractiveFrame): List<FrameHandle.FrameDefinition> {
        return this.frameDefinitions.getOrPut(frame) {
            val definitions = mutableListOf<FrameDefinition>()
            val dimensions = this.getFrameCounts(frame)

            for (y in 0 until dimensions.height) {
                for (x in 0 until dimensions.width) {
                    definitions.add(FrameDefinition(getNextEntityId(), UUID.randomUUID(), getNextMapId()))
                }
            }

            return@getOrPut definitions
        }
    }

    private class FrameDefinition(val entityId: Int, val uuid: UUID, mapId: Int) : FrameHandle.FrameDefinition(mapId)

}
