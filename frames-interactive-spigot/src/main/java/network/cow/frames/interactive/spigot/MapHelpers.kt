package network.cow.frames.interactive.spigot

import network.cow.frames.color.ColorTransformer
import network.cow.frames.color.PaletteColorTransformer
import org.bukkit.block.BlockFace
import org.bukkit.util.Vector
import java.awt.Rectangle
import java.awt.image.BufferedImage

/**
 * @author Benedikt Wüller
 */

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
