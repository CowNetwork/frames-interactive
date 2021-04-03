package network.cow.frames.interactive

import network.cow.frames.color.ColorTransformer
import network.cow.frames.color.DefaultColorTransformer
import java.awt.Dimension

/**
 * @author Benedikt Wüller
 */
abstract class HandheldInteractiveFrame(
    canvasDimensions: Dimension,
    initialUpdateInterval: Long = 1L,
    transformer: ColorTransformer = DefaultColorTransformer()
) : InteractiveFrame(canvasDimensions, Dimension(128, 128), initialUpdateInterval, transformer)
