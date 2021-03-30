package network.cow.frames.interactive.standalone.example

import network.cow.frames.interactive.example.ExampleFrame
import network.cow.frames.interactive.standalone.FrameWindow
import java.awt.Dimension

/**
 * @author Benedikt Wüller
 */

fun main() {
    val window = FrameWindow({ ExampleFrame(Dimension(512, 512)) }, "ExampleFrame")
    window.start()
}
