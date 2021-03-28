package network.cow.interactive.example

import network.cow.frames.alignment.Alignment
import network.cow.frames.component.ColorComponent
import network.cow.frames.component.CompoundComponent
import network.cow.frames.component.TextComponent
import network.cow.interactive.InteractiveFrame
import network.cow.interactive.Key
import java.awt.Color
import java.awt.Dimension
import java.awt.Font
import java.awt.Point

/**
 * @author Benedikt Wüller
 */
class ExampleFrame(viewportDimensions: Dimension) : InteractiveFrame(Dimension(256, 256), viewportDimensions, 50L) {

    private val fontSmall = Font(Font.SANS_SERIF, Font.BOLD, 12)
    private val fontBig = Font(Font.SANS_SERIF, Font.BOLD, 15)

    private val baseComponent = CompoundComponent(Point(), this.canvasDimensions)
    private val cursorPositionText = TextComponent(Point(this.canvasDimensions.width / 2, 10), "0x0", Color.DARK_GRAY, fontSmall, Alignment.TOP_CENTER)

    private val keyComponents = mutableMapOf<Key, TextComponent>()

    init {
        this.baseComponent.addComponent(ColorComponent(Point(), this.canvasDimensions, Color.LIGHT_GRAY))
        this.baseComponent.addComponent(cursorPositionText)

        Key.values().forEachIndexed { index, key ->
            val component = TextComponent(Point(this.canvasDimensions.width / 2, 30 + 20 * (index + 1)), key.name, Color.DARK_GRAY, fontBig, Alignment.TOP_CENTER)
            this.keyComponents[key] = component
            this.baseComponent.addComponent(component)
        }
    }

    override fun onUpdate(currentTime: Long, delta: Long): Boolean {
        return true
    }

    override fun onCursorMove(position: Point, currentTime: Long) {
        this.cursorPositionText.text = "${position.x} x ${position.y}"
    }

    override fun onKeyDown(key: Key, currentTime: Long) {
        this.keyComponents[key]?.color = Color.RED
    }

    override fun onKeyUp(key: Key, currentTime: Long) {
        this.keyComponents[key]?.color = Color.DARK_GRAY
    }

    override fun getRenderComponent() = baseComponent

}
