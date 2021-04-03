package network.cow.frames.interactive.spigot.helper

import java.awt.image.BufferedImage

/**
 * @author Benedikt Wüller
 */

fun BufferedImage.deepCopy(): BufferedImage {
    val cm = this.colorModel
    val isAlphaPremultiplied = cm.isAlphaPremultiplied
    val raster = this.copyData(null)
    return BufferedImage(cm, raster, isAlphaPremultiplied, null)
}
