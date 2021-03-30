package network.cow.frames.interactive.spigot

import net.minecraft.server.v1_16_R3.PacketPlayOutMap
import network.cow.frames.interactive.InteractiveFrame
import network.cow.frames.interactive.State
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftPlayer
import org.bukkit.entity.EntityType
import org.bukkit.entity.ItemFrame
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.MapMeta
import org.bukkit.map.MapView
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.util.BoundingBox
import java.awt.Point
import java.awt.geom.Point2D
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * @author Benedikt Wüller
 */
class FrameHandle(
    private val plugin: JavaPlugin,
    private val provider: () -> InteractiveFrame,
    private var topLeftLocation: Location,
    private var direction: BlockFace,
    private var maxCursorDistance: Double = 3.0
) : Listener {

    private val itemFrames = mutableListOf<ItemFrame>()
    private val maps = mutableListOf<MapView>()

    private lateinit var frame: InteractiveFrame

    private var columns: Int = 0
    private var rows: Int = 0

    var controllingPlayer: Player? = null
        set(value) {
            if (field == value) return
            field = value

            if (value == null) {
                this.frame.state = State.INACTIVE
            } else {
                this.frame.state = State.ACTIVE
            }
        }

    init {
        val supportedDirections = arrayOf(BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST)
        if (!supportedDirections.contains(this.direction)) {
            throw IllegalArgumentException("The direction must be one of: ${supportedDirections.joinToString(", ")}.")
        }

        Bukkit.getPluginManager().registerEvents(this, this.plugin)
        Bukkit.getScheduler().runTaskTimerAsynchronously(this.plugin, this::tick, 1L, 1L)
    }

    fun spawn() {
        this.despawn()

        this.frame = this.provider()
        this.frame.addStateListener { _, newState ->
            if (newState != State.INACTIVE) return@addStateListener
            this.spawn()
        }

        this.createItemFrames()
        this.controllingPlayer?.vehicle?.eject()
    }

    fun despawn() {
        this.itemFrames.forEach { it.remove() }
        this.itemFrames.clear()
        this.maps.clear()
    }

    fun respawn() = this.spawn()

    @Synchronized
    private fun tick() {
        if (this.frame.state == State.INACTIVE) return

        // Update cursor position.
        this.getTargetViewportCoordinates()?.let(this.frame::setViewportCursorPosition)

        this.frame.tick()

        if (!this.frame.hasUpdatedSections()) return
        val sections = this.frame.pullUpdatedSections()

        sections.forEach { section ->
            this.maps.forEachIndexed { index, map ->
                val bounds = getMapBounds(index % this.columns, index / this.columns)
                val intersection = section.createIntersection(bounds).bounds
                if (intersection.isEmpty) return@forEachIndexed

                val data = createMapData(this.frame.transformer, this.frame.getViewport(), intersection)
                val packet = PacketPlayOutMap(
                    map.id, 0.toByte(), false, false, emptyList(), data,
                    intersection.x % 128, intersection.y % 128, intersection.width, intersection.height
                )

                Bukkit.getOnlinePlayers().forEach { player ->
                    (player as CraftPlayer).handle.playerConnection.sendPacket(packet)
                }
            }
        }
    }

    private fun createItemFrames() {
        // Create item frames.
        this.columns = ceil(this.frame.viewportDimension.width / 128.0).toInt()
        this.rows = ceil(this.frame.viewportDimension.height / 128.0).toInt()

        (0 until rows).forEach { y ->
            (0 until columns).forEach { x ->
                val location = getMapLocation(this.topLeftLocation, this.direction, x, y)

                // Make sure there is a solid block behind the item frame.
                val block = location.block.getRelative(this.direction.oppositeFace)
                println(block)
                val temporaryBlock = block.type == Material.AIR
                if (temporaryBlock) block.type = Material.STONE

                // Spawn invisible and fixed item frame.
                val itemFrame = location.world.spawnEntity(location, EntityType.ITEM_FRAME) as ItemFrame
                itemFrame.isFixed = true
                itemFrame.isVisible = true

                // Remove the temporary block.
                if (temporaryBlock) block.type = Material.AIR

                // Create a map without any renderers (just makes sure the map id is unique).
                val map = Bukkit.createMap(location.world)
                map.renderers.forEach { map.removeRenderer(it) }

                // Create the item stack and set map meta.
                val itemStack = ItemStack(Material.MAP)
                val meta = itemStack.itemMeta as MapMeta
                meta.mapView = map
                itemStack.itemMeta = meta
                itemFrame.setItem(itemStack)

                this.itemFrames.add(itemFrame)
                this.maps.add(map)
            }
        }
    }

    private fun getBoundingBox(): BoundingBox {
        val firstItemFrame = this.itemFrames.first()
        val lastItemFrame = this.itemFrames.last()

        val boundingBox = firstItemFrame.boundingBox.union(lastItemFrame.boundingBox)
        return when (this.direction) {
            BlockFace.NORTH, BlockFace.SOUTH -> boundingBox.expand(0.125, 0.125, 0.0)
            BlockFace.EAST, BlockFace.WEST -> boundingBox.expand(0.0, 0.125, 0.125)
            else -> boundingBox
        }
    }

    private fun getTargetViewportCoordinates(): Point? {
        val player = this.controllingPlayer ?: return null

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
            (point.x / this.columns * this.frame.viewportDimension.width).roundToInt(),
            (point.y / this.rows * this.frame.viewportDimension.height).roundToInt()
        )
    }

    @EventHandler
    private fun onRightClick(event: PlayerInteractEntityEvent) {
        // TODO: right click
    }

    @EventHandler
    private fun onInteract(event: PlayerInteractEvent) {
        // TODO: right click block
        // TODO: left click
    }

}
