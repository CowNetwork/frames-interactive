package network.cow.frames.interactive.spigot.handle

import network.cow.frames.interactive.Input
import network.cow.frames.interactive.InteractiveFrame
import network.cow.frames.interactive.State
import network.cow.frames.interactive.spigot.helper.deepCopy
import network.cow.frames.interactive.spigot.helper.getDataWatcher
import network.cow.frames.interactive.spigot.helper.getMapBounds
import network.cow.frames.interactive.spigot.helper.getMapLocation
import network.cow.frames.interactive.spigot.helper.getNextEntityId
import network.cow.frames.interactive.spigot.helper.getNextMapId
import network.cow.frames.interactive.spigot.helper.sendMapData
import network.cow.protocol.wrappers.WrapperPlayServerEntityDestroy
import network.cow.protocol.wrappers.WrapperPlayServerEntityMetadata
import network.cow.protocol.wrappers.WrapperPlayServerSpawnEntity
import org.bukkit.Bukkit
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
import java.awt.image.BufferedImage
import java.util.UUID
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * @author Benedikt Wüller
 */
class StationaryFrameHandle<T : InteractiveFrame>(plugin: JavaPlugin, frameProvider: () -> T) : FrameHandle<T>(plugin, frameProvider) {

    companion object {
        private const val BOUNDING_BOX_PADDING = 0.125
    }

    private val activePlayers = mutableSetOf<Player>()
    private val playersInRange = mutableSetOf<Player>()
    private val initialFrame: BufferedImage

    private val columns = ceil(this.frame.viewportDimension.width / 128.0).toInt()
    private val rows = ceil(this.frame.viewportDimension.height / 128.0).toInt()

    private val itemFrames = Array(this.columns * this.rows) { ItemFrame(it % this.columns, it / this.columns) }

    var updateRange: Double = 10.0

    var maxCursorDistance: Double = 5.0

    /**
     * The location of the top left hand corner when facing the block the item frame will hang on (i.e. facing opposite of [direction]).
     */
    var topLeftLocation: Vector = Vector()

    /**
     * The [direction] the frame will face in.
     */
    var direction: BlockFace = BlockFace.NORTH

    /**
     * The [player] controlling the inputs for the [frame].
     */
    var player: Player? = null
        set(value) {
            if (field == value) return

            if (value == null) {
                // If the player is unset, create a new frame.
                this.reset()
            } else {
                // Otherwise, set the frame state to active.
                this.frame.state = State.ACTIVE
            }

            field = value
        }

    init {
        this.frame.tick(true)
        this.initialFrame = this.frame.getViewport().deepCopy()
    }

    @Synchronized
    override fun tick() {
        val playersInRange = this.activePlayers.filter(this::isInRange).toMutableSet()

        for (player in playersInRange) {
            if (this.playersInRange.contains(player)) continue
            // Player just got into range. Send full frame update.
            this.itemFrames.forEach { frame ->
                sendMapData(frame.mapId, this.frame.transformer, this.frame.getViewport(), Rectangle(frame.x * 128, frame.y * 128, 128, 128), player)
            }
        }

        for (player in this.playersInRange) {
            if (playersInRange.contains(player)) continue
            // Player just left the range. Send initial frame.
            this.itemFrames.forEach { frame ->
                sendMapData(frame.mapId, this.frame.transformer, this.initialFrame, Rectangle(frame.x * 128, frame.y * 128, 128, 128), player)
            }
        }

        this.playersInRange.clear()
        this.playersInRange.addAll(playersInRange)

        super.tick()
    }

    override fun renderSections(sections: List<Rectangle>) {
        val players = this.activePlayers.filter(this::isInRange).toTypedArray()

        sections.forEach { section ->
            this.itemFrames.forEach inner@{ frame ->
                val bounds = getMapBounds(frame.x, frame.y)
                val intersection = section.createIntersection(bounds).bounds
                if (intersection.isEmpty) return@inner

                sendMapData(frame.mapId, this.frame.transformer, this.frame.getViewport(), intersection, *players)
            }
        }
    }

    override fun updateCursor(frame: InteractiveFrame) {
        if (frame.state == State.INACTIVE) return
        val player = this.player ?: return
        val position = this.calculateTargetViewportCoordinates(player) ?: return
        frame.setViewportCursorPosition(position)
    }

    private fun calculateTargetViewportCoordinates(player: Player): Point? {
        val boundingBox = this.getBoundingBox()
        val rayTrace = boundingBox.rayTrace(player.eyeLocation.toVector(), player.location.direction, this.maxCursorDistance) ?: return null
        val position = rayTrace.hitPosition

        val point = when (this.direction) {
            BlockFace.NORTH -> Point2D.Double(boundingBox.maxX - position.x, boundingBox.maxY - position.y)
            BlockFace.SOUTH -> Point2D.Double(position.x - boundingBox.minX, boundingBox.maxY - position.y)
            BlockFace.EAST -> Point2D.Double(boundingBox.maxZ - position.z, boundingBox.maxY - position.y)
            BlockFace.WEST -> Point2D.Double(position.z - boundingBox.minZ, boundingBox.maxY - position.y)
            else -> Point2D.Double()
        }

        return Point(
            ((point.x - BOUNDING_BOX_PADDING) / this.columns * frame.viewportDimension.width).roundToInt(),
            ((point.y - BOUNDING_BOX_PADDING) / this.rows * frame.viewportDimension.height).roundToInt()
        )
    }

    private fun getBoundingBox(): BoundingBox {
        val topLeft = this.topLeftLocation
        val bottomRight = getMapLocation(this.topLeftLocation, this.direction, this.columns - 1, this.rows - 1)
        val boundingBoxDepth = 0.07

        return when(this.direction) {
            BlockFace.NORTH -> BoundingBox(
                bottomRight.x - BOUNDING_BOX_PADDING, bottomRight.y - BOUNDING_BOX_PADDING, bottomRight.z + 1.0 - boundingBoxDepth,
                topLeft.x + 1.0 + BOUNDING_BOX_PADDING, topLeft.y + 1.0 + BOUNDING_BOX_PADDING, topLeft.z + 1.0
            )
            BlockFace.EAST -> BoundingBox(
                bottomRight.x, bottomRight.y - BOUNDING_BOX_PADDING, bottomRight.z - BOUNDING_BOX_PADDING,
                topLeft.x + boundingBoxDepth, topLeft.y + 1.0 + BOUNDING_BOX_PADDING, topLeft.z + 1.0 + BOUNDING_BOX_PADDING
            )
            BlockFace.SOUTH -> BoundingBox(
                topLeft.x - BOUNDING_BOX_PADDING, bottomRight.y - BOUNDING_BOX_PADDING, topLeft.z,
                bottomRight.x + 1.0 + BOUNDING_BOX_PADDING, topLeft.y + 1.0 + BOUNDING_BOX_PADDING, bottomRight.z + boundingBoxDepth
            )
            BlockFace.WEST -> BoundingBox(
                topLeft.x + 1.0 - boundingBoxDepth, bottomRight.y - BOUNDING_BOX_PADDING, topLeft.z - BOUNDING_BOX_PADDING,
                bottomRight.x + 1.0, topLeft.y + 1.0 + BOUNDING_BOX_PADDING, bottomRight.z + 1.0 + BOUNDING_BOX_PADDING
            )
            else -> BoundingBox()
        }
    }

    /**
     * Spawns the given frame for the given [players] from the given [topLeftLocation], facing the given [direction].
     * Map updates will only be sent to players within [updateRange], for which the frame has been spawned.
     */
    fun spawn(vararg players: Player) {
        val targetPlayers = players.filterNot { this.activePlayers.contains(it) }.toTypedArray()

        val (yaw, pitch, data) = when (this.direction) {
            BlockFace.NORTH -> Triple(180.0f, 0.0f, 2)
            BlockFace.EAST -> Triple(-90.0f, 0.0f, 5)
            BlockFace.SOUTH -> Triple(0.0f, 0.0f, 3)
            BlockFace.WEST -> Triple(90.0f, 0.0f, 4)
            else -> Triple(0.0f, 0.0f, 0)
        }

        this.itemFrames.forEach { frame ->
            val location = getMapLocation(this.topLeftLocation, this.direction, frame.x, frame.y)

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

            targetPlayers.forEach {
                spawnPacket.sendPacket(it)
                metaPacket.sendPacket(it)
                this.activePlayers.add(it)

                // Send initial frame or the current viewport if the player is in range.
                val initialViewport = if (this.isInRange(it)) this.frame.getViewport() else this.initialFrame
                sendMapData(frame.mapId, this.frame.transformer, initialViewport, Rectangle(frame.x * 128, frame.y * 128, 128, 128), it)
            }
        }
    }

    /**
     * Removes the frame for the given [players] (i.e. sends delete packets).
     */
    fun remove(vararg players: Player) {
        this.itemFrames.forEach { frame ->
            val packet = WrapperPlayServerEntityDestroy()
            packet.setEntityIds(intArrayOf(frame.entityId))

            players.forEach {
                packet.sendPacket(player)
                this.activePlayers.remove(it)
                this.playersInRange.remove(it)
            }
        }

        if (this.player == player) {
            this.player = null
        }
    }

    private fun isInRange(player: Player) = player.location.toVector().distance(this.topLeftLocation) <= this.updateRange

    override fun handleLeftClick(player: Player) {
        if (player != this.player) return

        // Check if player is looking at frame.
        this.calculateTargetViewportCoordinates(player) ?: return

        this.frame.setInputActive(Input.INTERACT_PRIMARY, true)
        Bukkit.getScheduler().runTaskLater(this.plugin, Runnable {
            this.frame.setInputActive(Input.INTERACT_PRIMARY, false)
        }, 2L)
    }

    override fun invalidate(player: Player) {
        this.activePlayers.remove(player)
    }

    private data class ItemFrame(val x: Int, val y: Int, val entityId: Int = getNextEntityId(), val uuid: UUID = UUID.randomUUID(), val mapId: Int = getNextMapId())

}
