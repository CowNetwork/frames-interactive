package network.cow.interactive

import network.cow.frames.Frame
import network.cow.frames.color.ColorTransformer
import network.cow.frames.color.DefaultColorTransformer
import java.awt.Dimension
import java.awt.Point
import kotlin.math.floor

/**
 * @author Benedikt Wüller
 */
abstract class InteractiveFrame(
    canvasDimensions: Dimension,
    viewportDimension: Dimension = canvasDimensions,
    initialUpdateInterval: Long = 1L,
    transformer: ColorTransformer = DefaultColorTransformer()
) : Frame(canvasDimensions, viewportDimension, initialUpdateInterval, transformer) {

    private val pressedKeys = mutableMapOf<Key, Long>()
    private val nextKeys = mutableSetOf<Key>()

    private val cursorPosition = Point()
    private val nextCursorPosition = Point()

    /**
     * While a key is held down, it will trigger the [onKeyRepeat] callback every [keyRepeatInterval] milliseconds.
     */
    var keyRepeatInterval: Long = 250

    override fun onTick(currentTime: Long, delta: Long) {
        synchronized(this.nextKeys) {
            this.handleKeys()
        }

        synchronized(this.nextCursorPosition) {
            this.handleCursor()
        }
    }

    /**
     * Sets the [position] of the cursor based on the [canvasDimensions].
     * Calls [onCursorMove] implicitly at the next tick.
     */
    fun setCanvasCursorPosition(position: Point) {
        this.nextCursorPosition.setLocation(
            maxOf(minOf(position.x, this.canvasDimensions.width - 1), 0),
            maxOf(minOf(position.y, this.canvasDimensions.height - 1), 0)
        )
    }

    /**
     * Sets the [position] of the cursor based on the [viewportDimension].
     * Calls [onCursorMove] implicitly at the next tick.
     */
    fun setViewportCursorPosition(position: Point) {
        this.setCanvasCursorPosition(Point(
            floor(position.x * (this.canvasDimensions.width.toDouble() / this.viewportDimension.width)).toInt(),
            floor(position.y * (this.canvasDimensions.height.toDouble() / this.viewportDimension.height)).toInt()
        ))
    }

    protected open fun performCursorMove(position: Point) {
        synchronized(this.nextCursorPosition) {
            if (this.cursorPosition == position) return
            this.cursorPosition.setLocation(position.x, position.y)
            this.onCursorMove(position, this.totalTime)
        }
    }

    private fun handleCursor() = this.performCursorMove(this.nextCursorPosition)

    /**
     * Sets whether the [key] [isPressed] and update the state accordingly.
     * Calls [onKeyDown], [onKeyRepeat] and [onKeyDown] implicitly at the next tick.
     */
    fun setKeyPressed(key: Key, isPressed: Boolean) {
        if (isPressed) {
            this.nextKeys.add(key)
        } else {
            this.nextKeys.remove(key)
        }
    }

    private fun performKeyPressed(key: Key, isPressed: Boolean) {
        synchronized(this.nextKeys) {
            val wasPressed = this.isKeyPressed(key)
            if (isPressed && wasPressed) {
                this.handleKeyRepeat(key)
            } else if (isPressed && !wasPressed) {
                this.handleKeyDown(key)
            } else if (!isPressed && wasPressed) {
                this.handleKeyUp(key)
            }
        }
    }

    private fun handleKeys() = Key.values().forEach {
        this.performKeyPressed(it, this.nextKeys.contains(it))
    }

    protected open fun handleKeyDown(key: Key) {
        if (this.isKeyPressed(key)) return
        val currentTime = this.totalTime
        this.pressedKeys[key] = currentTime
        this.onKeyDown(key, currentTime)
    }

    protected open fun handleKeyRepeat(key: Key) {
        val lastPressedAt = this.getKeyLastPressedAt(key) ?: return
        val currentTime = this.totalTime
        if (currentTime - lastPressedAt < this.keyRepeatInterval) return
        this.pressedKeys[key] = currentTime
        this.onKeyRepeat(key, this.totalTime)
    }

    protected open fun handleKeyUp(key: Key) {
        this.pressedKeys.remove(key) ?: return
        this.onKeyUp(key, this.totalTime)
    }

    protected fun isKeyPressed(key: Key) = this.pressedKeys.containsKey(key)

    protected fun getKeyLastPressedAt(key: Key) = this.pressedKeys[key]

    protected open fun onKeyDown(key: Key, currentTime: Long) = Unit

    protected open fun onKeyRepeat(key: Key, currentTime: Long) = Unit

    protected open fun onKeyUp(key: Key, currentTime: Long) = Unit

    protected open fun onCursorMove(position: Point, currentTime: Long) = Unit

}
