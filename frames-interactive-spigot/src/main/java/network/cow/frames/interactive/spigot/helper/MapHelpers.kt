package network.cow.frames.interactive.spigot.helper

import network.cow.frames.color.ColorTransformer
import network.cow.frames.color.PaletteColorTransformer
import network.cow.frames.interactive.InteractiveFrame
import network.cow.protocol.wrappers.WrapperPlayServerMap
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import java.awt.Rectangle
import java.awt.image.BufferedImage
import java.util.concurrent.atomic.AtomicInteger

/**
 * @author Benedikt Wüller
 */

private val MAP_ID_FIELD = AtomicInteger(420_000)
private val ENTITY_ID_FIELD = AtomicInteger(420_000)

fun getNextEntityId(): Int = ENTITY_ID_FIELD.getAndIncrement()

fun getNextMapId(): Int = MAP_ID_FIELD.getAndIncrement()

fun getMapBounds(x: Int, y: Int): Rectangle = Rectangle(x * 128, y * 128, 128, 128)

fun getMapLocation(location: Vector, face: BlockFace, column: Int, row: Int): Vector {
    val result = location.clone()
    result.add(when (face) {
        BlockFace.NORTH -> Vector(-column.toDouble(), -row.toDouble(), 0.0)
        BlockFace.EAST -> Vector(0.0, -row.toDouble(), -column.toDouble())
        BlockFace.SOUTH -> Vector(column.toDouble(), -row.toDouble(), 0.0)
        BlockFace.WEST -> Vector(0.0, -row.toDouble(), column.toDouble())
        else -> Vector()
    })
    return result
}

fun createMapData(colorTransformer: ColorTransformer, image: BufferedImage, bounds: Rectangle): ByteArray {
    if (colorTransformer !is PaletteColorTransformer) {
        throw IllegalStateException("Spigot requires PaletteColorTransformers for FrameGames. ${colorTransformer.javaClass.name} given.")
    }

    return colorTransformer.matchColorIndices(image, bounds).map { it.toByte() }.toByteArray()
}

fun sendMapData(id: Int, transformer: ColorTransformer, viewport: BufferedImage, section: Rectangle, vararg players: Player) {
    // Why the fuck are columns/rows and x/y flipped?
    val packet = WrapperPlayServerMap()
    packet.itemDamage = id
    packet.scale = 0
    packet.trackingPosition = false
    packet.isLocked = false
    packet.columns = section.x % 128
    packet.rows = section.y % 128
    packet.x = section.width
    packet.z = section.height
    packet.data = createMapData(transformer, viewport, section)

    players.forEach(packet::sendPacket)
}
