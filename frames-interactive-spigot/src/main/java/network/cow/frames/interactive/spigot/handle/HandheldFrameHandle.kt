package network.cow.frames.interactive.spigot.handle

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import network.cow.frames.interactive.HandheldInteractiveFrame
import network.cow.frames.interactive.Input
import network.cow.frames.interactive.InteractiveFrame
import network.cow.frames.interactive.State
import network.cow.frames.interactive.spigot.helper.allocateMapView
import network.cow.frames.interactive.spigot.helper.freeMapView
import network.cow.frames.interactive.spigot.helper.getMapBounds
import network.cow.frames.interactive.spigot.helper.sendMapData
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerItemHeldEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.MapMeta
import org.bukkit.map.MapView
import org.bukkit.plugin.java.JavaPlugin
import java.awt.Point
import java.awt.Rectangle
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * @author Benedikt Wüller
 */
class HandheldFrameHandle<T : HandheldInteractiveFrame>(
    plugin: JavaPlugin,
    frameProvider: () -> T,
    val player: Player,
    private val itemName: String,
    private vararg val itemLore: String
) : FrameHandle<T>(plugin, frameProvider), Listener {

    companion object {
        private const val MAX_DELTA_YAW = 45.0

        private const val MIN_PITCH = 45.0
        private const val MAX_PITCH = 90.0

        private const val PADDING_DEGREES = 5.0
    }

    private val mapView: MapView = allocateMapView()

    private var initialYaw: Float = 0.0F

    private val activePlayers = mutableSetOf<Player>()

    val itemStack: ItemStack

    init {
        val itemStack = ItemStack(Material.FILLED_MAP)
        val meta = itemStack.itemMeta as MapMeta
        meta.mapView = this.mapView
        meta.lore(this.itemLore.map { LegacyComponentSerializer.legacySection().deserialize(it) })
        meta.displayName(LegacyComponentSerializer.legacySection().deserialize(this.itemName))
        itemStack.itemMeta = meta
        this.itemStack = itemStack

        Bukkit.getPluginManager().registerEvents(this, this.plugin)
    }

    override fun updateCursor(frame: InteractiveFrame) {
        val position = this.getCanvasCursorPosition() ?: return
        this.frame.setCanvasCursorPosition(position)
    }

    private fun getCanvasCursorPosition(): Point? {
        infix fun Double.floorMod(other: Double) = this - floor(this / other) * other

        val yawDelta = ((this.player.location.yaw - this.initialYaw + 180.0) floorMod 360.0) - 180.0
        if (yawDelta < -0.5 * MAX_DELTA_YAW - PADDING_DEGREES || yawDelta > 0.5 * MAX_DELTA_YAW + PADDING_DEGREES) return null

        val pitch = this.player.location.pitch
        if (pitch > MAX_PITCH + PADDING_DEGREES || pitch < MIN_PITCH - PADDING_DEGREES) return null

        val x = yawDelta.scale(-0.5 * MAX_DELTA_YAW, 0.5 * MAX_DELTA_YAW, 0.0, 127.0)
        val y = pitch.toDouble().scale(MIN_PITCH, MAX_PITCH, 0.0, 127.0)

        return Point(x.roundToInt(), y.roundToInt())
    }

    private fun Double.scale(minFrom: Double, maxFrom: Double, minTo: Double, maxTo: Double): Double {
        val scaledValue = ((this - minFrom) / (maxFrom - minFrom)) * (maxTo - minTo) + minTo
        return minOf(maxOf(scaledValue, minTo), maxTo)
    }

    override fun renderSections(sections: List<Rectangle>) {
        val players = this.activePlayers.toTypedArray()

        sections.forEach { section ->
            val intersection = section.createIntersection(Rectangle(0, 0, 128, 128)).bounds
            if (intersection.isEmpty) return@forEach
            sendMapData(this.mapView.id, this.frame.transformer, this.frame.getViewport(), intersection, *players)
        }
    }

    override fun handleLeftClick(player: Player) {
        if (this.player != player) return
        this.getCanvasCursorPosition() ?: return

        this.frame.setInputActive(Input.INTERACT_PRIMARY, true)
        Bukkit.getScheduler().runTaskLater(this.plugin, Runnable {
            this.frame.setInputActive(Input.INTERACT_PRIMARY, false)
        }, 2L)
    }

    override fun shouldUpdate(): Boolean = this.activePlayers.contains(this.player)

    @EventHandler
    private fun onChangeItem(event: PlayerItemHeldEvent) {
        val player = event.player
        val newItem = player.inventory.getItem(event.newSlot)

        if (this.itemStack.isSimilar(newItem)) {
            this.activePlayers.add(player)

            if (player != this.player) return
            this.frame.state = State.ACTIVE
            this.initialYaw = this.player.location.yaw
        } else {
            this.activePlayers.remove(player)

            if (player != this.player) return
            this.reset()
        }
    }

    override fun destroy() {
        super.destroy()
        HandlerList.unregisterAll(this)
        freeMapView(this.mapView)
        this.activePlayers.clear()
    }

    override fun invalidate(player: Player) {
        this.activePlayers.remove(player)
    }

}
