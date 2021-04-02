package network.cow.frames.interactive.spigot

import com.comphenix.protocol.wrappers.BlockPosition
import com.comphenix.protocol.wrappers.WrappedBlockData
import network.cow.frames.interactive.Input
import network.cow.frames.interactive.InteractiveFrame
import network.cow.frames.interactive.State
import network.cow.protocol.wrappers.WrapperPlayServerBlockChange
import network.cow.protocol.wrappers.WrapperPlayServerEntityDestroy
import network.cow.protocol.wrappers.WrapperPlayServerEntityMetadata
import network.cow.protocol.wrappers.WrapperPlayServerMap
import network.cow.protocol.wrappers.WrapperPlayServerSpawnEntity
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.block.BlockFace
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.event.Listener
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.MapMeta
import org.bukkit.util.BoundingBox
import org.bukkit.util.Vector
import java.awt.Point
import java.awt.Rectangle
import java.awt.geom.Point2D
import java.util.UUID
import java.util.WeakHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * @author Benedikt Wüller
 */
class StaticFrameHandle(
    val frame: InteractiveFrame,
    val world: World,
    var topLeftLocation: Vector = Vector(),
    var direction: BlockFace = BlockFace.NORTH,
    var maxCursorDistance: Double = 3.0
) : Listener {

    companion object {
        private val ENTITY_ID_FIELD = AtomicInteger(420_000)
        private val MAP_ID_FIELD = AtomicInteger(420_000)

        fun getNextEntityId(): Int = ENTITY_ID_FIELD.getAndIncrement()
        fun getNextMapId(): Int = MAP_ID_FIELD.getAndIncrement()
    }

    private val frames = mutableListOf<Frame>()
    private val columns: Int = ceil(this.frame.viewportDimension.width / 128.0).toInt()
    private val rows: Int = ceil(this.frame.viewportDimension.height / 128.0).toInt()

    private val activePlayers = WeakHashMap<Player, Boolean>()

    init {
        for (y in 0 until rows) {
            for (x in 0 until columns) {
                this.frames.add(Frame(getNextEntityId(), UUID.randomUUID(), getNextMapId()))
            }
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

        val activePlayers = players.filterNot { this.activePlayers.containsKey(it) }

        (0 until rows).forEach { y ->
            (0 until columns).forEach { x ->
                val index = y * this.columns + x
                val frame = this.frames[index]
                val location = getMapLocation(this.topLeftLocation, this.direction, x, y)

                val spawnPacket = WrapperPlayServerSpawnEntity()
                spawnPacket.entityID = frame.entityId
                spawnPacket.uniqueId = frame.uuid
                spawnPacket.type = EntityType.ITEM_FRAME
                spawnPacket.x = location.x
                spawnPacket.y = location.y
                spawnPacket.z = location.z
                spawnPacket.yaw = yaw
                spawnPacket.pitch = pitch
                spawnPacket.objectData = data

                val itemStack = ItemStack(Material.FILLED_MAP)
                val itemMeta = itemStack.itemMeta as MapMeta
                itemMeta.mapId = frame.mapId
                itemStack.itemMeta = itemMeta

                val watcher = getDataWatcher(EntityType.ITEM_FRAME)
                watcher.getWatchableObject(7).setValue(itemStack, true)

                val metaPacket = WrapperPlayServerEntityMetadata()
                metaPacket.entityID = frame.entityId
                metaPacket.metadata = watcher.watchableObjects

                val block = this.world.getBlockAt(location.x.toInt(), location.y.toInt(), location.z.toInt()).getRelative(this.direction.oppositeFace)
                val blockPacket = if (!block.type.isAir) {
                    val packet = WrapperPlayServerBlockChange()
                    packet.location = BlockPosition(block.location.toVector())
                    packet.blockData = WrappedBlockData.createData(Material.BARRIER)
                    packet
                } else {
                    null
                }

                activePlayers.forEach {
                    this.activePlayers[it] = true
                    blockPacket?.sendPacket(it)
                    spawnPacket.sendPacket(it)
                    metaPacket.sendPacket(it)
                }
            }
        }

        this.sendMapData(Rectangle(Point(), this.frame.viewportDimension), *players)
    }

    fun despawn(vararg players: Player) {
        (0 until rows).forEach { y ->
            (0 until columns).forEach { x ->
                val index = y * this.columns + x
                val frame = this.frames[index]
                val location = getMapLocation(this.topLeftLocation, this.direction, x, y)

                val despawnPacket = WrapperPlayServerEntityDestroy()
                despawnPacket.setEntityIds(intArrayOf(frame.entityId))

                val block = this.world.getBlockAt(location.x.toInt(), location.y.toInt(), location.z.toInt()).getRelative(this.direction.oppositeFace)
                val blockPacket = if (block.type.isAir) {
                    val packet = WrapperPlayServerBlockChange()
                    packet.location = BlockPosition(block.location.toVector())
                    packet.blockData = WrappedBlockData.createData(Material.AIR)
                    packet
                } else {
                    null
                }

                players.forEach {
                    blockPacket?.sendPacket(it)
                    despawnPacket.sendPacket(it)
                }
            }
        }

        players.forEach(this.activePlayers::remove)
    }

    private fun getBoundingBox(): BoundingBox {
        val topLeft = this.topLeftLocation
        val bottomRight = getMapLocation(this.topLeftLocation, this.direction, this.columns - 1, this.rows - 1)
        val boundingBoxDepth = 0.0625

        return when(this.direction) {
            BlockFace.NORTH -> BoundingBox(bottomRight.x, bottomRight.y, bottomRight.z + 1.0 - boundingBoxDepth, topLeft.x + 1.0, topLeft.y + 1.0, topLeft.z + 1.0)
            BlockFace.EAST -> BoundingBox(bottomRight.x, bottomRight.y, bottomRight.z, topLeft.x + boundingBoxDepth, topLeft.y + 1.0, topLeft.z + 1.0)
            BlockFace.SOUTH -> BoundingBox(topLeft.x, bottomRight.y, topLeft.z, bottomRight.x + 1.0, topLeft.y + 1.0, bottomRight.z + boundingBoxDepth)
            BlockFace.WEST -> BoundingBox(topLeft.x + 1.0 - boundingBoxDepth, bottomRight.y, topLeft.z, bottomRight.x + 1.0, topLeft.y + 1.0, bottomRight.z + 1.0)
            else -> BoundingBox()
        }
    }

    @Synchronized
    fun updateTargetViewportCoordinates(player: Player) {
        if (this.frame.state == State.INACTIVE) return

        val boundingBox = this.getBoundingBox()
        val rayTrace = boundingBox.rayTrace(player.eyeLocation.toVector(), player.location.direction, this.maxCursorDistance) ?: return
        val position = rayTrace.hitPosition

        val point = when (this.direction) {
            BlockFace.NORTH -> Point2D.Double(boundingBox.maxX - position.x, boundingBox.maxY - position.y)
            BlockFace.SOUTH -> Point2D.Double(position.x - boundingBox.minX, boundingBox.maxY - position.y)
            BlockFace.EAST -> Point2D.Double(boundingBox.maxZ - position.z, boundingBox.maxY - position.y)
            BlockFace.WEST -> Point2D.Double(position.z - boundingBox.minZ, boundingBox.maxY - position.y)
            else -> Point2D.Double()
        }

        val target = Point(
            (point.x / this.columns * this.frame.viewportDimension.width).roundToInt(),
            (point.y / this.rows * this.frame.viewportDimension.height).roundToInt()
        )

        this.frame.setViewportCursorPosition(target)
    }

    @Synchronized
    fun updateInput(input: Input, isActive: Boolean) {
        if (this.frame.state == State.INACTIVE) return
        this.frame.setInputActive(input, isActive)
    }

    @Synchronized
    fun tick() {
        if (this.frame.state == State.INACTIVE) return

        this.frame.tick()

        if (!this.frame.hasUpdatedSections()) return
        val sections = this.frame.pullUpdatedSections()

        sections.forEach { this.sendMapData(it, *this.activePlayers.keys.toTypedArray()) }
    }

    private fun sendMapData(section: Rectangle, vararg players: Player) {
        this.frames.forEachIndexed { index, frame ->
            val bounds = getMapBounds(index % this.columns, index / this.columns)
            val intersection = section.createIntersection(bounds).bounds
            if (intersection.isEmpty) return@forEachIndexed

            // Why the fuck are columns/rows and x/y flipped?
            val packet = WrapperPlayServerMap()
            packet.itemDamage = frame.mapId
            packet.scale = 0
            packet.trackingPosition = false
            packet.isLocked = false
            packet.columns = intersection.x % 128
            packet.rows = intersection.y % 128
            packet.x = intersection.width
            packet.z = intersection.height
            packet.data = createMapData(this.frame.transformer, this.frame.getViewport(), intersection)

            players.forEach(packet::sendPacket)
        }
    }

    private data class Frame(val entityId: Int, val uuid: UUID, val mapId: Int)

}
