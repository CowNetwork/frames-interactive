package network.cow.frames.interactive.spigot

import network.cow.frames.color.ColorTransformer
import network.cow.frames.color.PaletteColorTransformer
import org.bukkit.Location
import org.bukkit.block.BlockFace
import java.awt.Rectangle
import java.awt.image.BufferedImage

/**
 * @author Benedikt Wüller
 */

fun getMapBounds(x: Int, y: Int): Rectangle = Rectangle(x * 128, y * 128, 128, 128)

fun getMapLocation(location: Location, face: BlockFace, column: Int, row: Int): Location {
    val result = location.clone()
    when (face) {
        BlockFace.NORTH -> result.add(-column.toDouble(), -row.toDouble(), 0.0)
        BlockFace.EAST -> result.add(0.0, -row.toDouble(), -column.toDouble())
        BlockFace.SOUTH -> result.add(column.toDouble(), -row.toDouble(), 0.0)
        BlockFace.WEST -> result.add(0.0, -row.toDouble(), column.toDouble())
        else -> Unit
    }
    return result
}

fun createMapData(colorTransformer: ColorTransformer, image: BufferedImage, bounds: Rectangle): ByteArray {
    if (colorTransformer !is PaletteColorTransformer) {
        throw IllegalStateException("Spigot requires PaletteColorTransformers for FrameGames. ${colorTransformer.javaClass.name} given.")
    }

    val data = ByteArray(128 * 128)
    val colors = colorTransformer.matchColorIndices(image, bounds)

    for (relativeY in 0 until bounds.height) {
        for (relativeX in 0 until bounds.width) {
            val x = bounds.x % 128 + relativeX
            val y = bounds.y % 128 + relativeY
            data[y * 128 + x] = colors[relativeY * bounds.width + relativeX].toByte()
        }
    }

    return data
}
