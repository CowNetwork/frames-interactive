package network.cow.frames.interactive.example

import network.cow.frames.alignment.Alignment
import network.cow.frames.color.ColorTransformer
import network.cow.frames.color.DefaultColorTransformer
import network.cow.frames.component.ColorComponent
import network.cow.frames.component.CompoundComponent
import network.cow.frames.component.TextComponent
import network.cow.frames.interactive.InteractiveFrame
import network.cow.frames.interactive.Input
import network.cow.frames.interactive.State
import java.awt.Color
import java.awt.Dimension
import java.awt.Font
import java.awt.Point

/**
 * @author Benedikt Wüller
 */
class ExampleFrame(viewportDimensions: Dimension, transformer: ColorTransformer = DefaultColorTransformer())
    : InteractiveFrame(Dimension(256, 256), viewportDimensions, 50L, transformer) {

    private val fontSmall = Font(Font.SANS_SERIF, Font.BOLD, 12)
    private val fontBig = Font(Font.SANS_SERIF, Font.BOLD, 15)

    private val idleComponent = CompoundComponent(Point(), this.canvasDimensions)

    private val baseComponent = CompoundComponent(Point(), this.canvasDimensions)
    private val cursorPositionText = TextComponent(Point(this.canvasDimensions.width / 2, 10), "0x0", Color.DARK_GRAY, fontSmall, Alignment.TOP_CENTER)
    private val cursorIndicator = ColorComponent(Point(), Dimension(5, 5), Color.BLUE)

    private val keyComponents = mutableMapOf<Input, TextComponent>()

    init {
        this.idleComponent.addComponent(ColorComponent(Point(), this.canvasDimensions, Color.LIGHT_GRAY))
        this.idleComponent.addComponent(TextComponent(Point(this.canvasDimensions.width / 2, this.canvasDimensions.height / 2), "IDLE", Color.DARK_GRAY, fontBig, Alignment.CENTER_CENTER))

        this.baseComponent.addComponent(ColorComponent(Point(), this.canvasDimensions, Color.LIGHT_GRAY))
        this.baseComponent.addComponent(this.cursorPositionText)
        this.baseComponent.addComponent(this.cursorIndicator)

        Input.values().forEachIndexed { index, key ->
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
        this.cursorIndicator.position.move(position.x - 2, position.y - 2)
    }

    override fun onInputActivate(input: Input, currentTime: Long) {
        this.keyComponents[input]?.color = Color.RED
    }

    override fun onInputDeactivate(input: Input, currentTime: Long) {
        this.keyComponents[input]?.color = Color.DARK_GRAY
    }

    override fun getRenderComponent() = if (this.state == State.INACTIVE) idleComponent else baseComponent

}
