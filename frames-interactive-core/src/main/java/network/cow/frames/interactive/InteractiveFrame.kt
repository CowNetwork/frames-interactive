package network.cow.frames.interactive

import network.cow.frames.Frame
import network.cow.frames.color.ColorTransformer
import network.cow.frames.color.DefaultColorTransformer
import java.awt.Dimension
import java.awt.Point
import java.util.LinkedList
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

    private val stateListeners = mutableListOf<(State, State) -> Unit>()

    private val activeInputs = mutableMapOf<Input, Long>()
//    private val nextKeys = mutableSetOf<Input>()

    private val inputActions = LinkedList<Pair<Input, Boolean>>()

    private val cursorPosition = Point()
    private val nextCursorPosition = Point()

    /**
     * While a key is held down, it will trigger the [onInputRepeat] callback every [inputRepeatInterval] milliseconds.
     */
    var inputRepeatInterval: Long = 250

    private var currentState = State.INACTIVE
    var state = currentState
        set(value) {
            if (field == value) return
            this.onStateChange(field, value)
            this.stateListeners.forEach { it(field, value) }
            field = value
        }

    fun addStateListener(listener: (State, State) -> Unit) = this.stateListeners.add(listener)

    fun removeStateListener(listener: (State, State) -> Unit) = this.stateListeners.remove(listener)

    override fun onTick(currentTime: Long, delta: Long) {
        this.handleInputs()
        this.handleCursor()
    }

    override fun update(currentTime: Long, delta: Long): Boolean {
        val stateChanged = this.currentState != this.state
        if (stateChanged) this.currentState = this.state
        return super.update(currentTime, delta) || stateChanged
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
     * Sets whether the [input] [isPressed] and update the state accordingly.
     * Calls [onInputActivate], [onInputRepeat] and [onInputActivate] implicitly at the next tick.
     */
    fun setInputActive(input: Input, isPressed: Boolean) {
        this.inputActions.add(input to isPressed)

//        if (isPressed) {
//            this.nextKeys.add(input)
//        } else {
//            this.nextKeys.remove(input)
//        }
    }

//    private fun performInputUpdate(input: Input, isPressed: Boolean) {
//        synchronized(this.nextKeys) {
//            val wasPressed = this.isInputActive(input)
//            if (isPressed && wasPressed) {
//                this.handleInputRepeat(input)
//            } else if (isPressed && !wasPressed) {
//                this.handleInputActivate(input)
//            } else if (!isPressed && wasPressed) {
//                this.handleInputDeactivate(input)
//            }
//        }
//    }

//    private fun handleInputs() = Input.values().forEach {
//        this.performInputUpdate(it, this.nextKeys.contains(it))
//    }

    private fun handleInputs() {
        synchronized(this.inputActions) {
            while (this.inputActions.isNotEmpty()) {
                val (input, isPressed) = this.inputActions.removeFirst()
                val wasPressed = this.isInputActive(input)

                if (isPressed && !wasPressed) {
                    this.handleInputActivate(input)
                } else if (!isPressed && wasPressed) {
                    this.handleInputDeactivate(input)
                }
            }
        }

        synchronized(this.activeInputs) {
            Input.values().forEach(this::handleInputRepeat)
        }
    }

    protected open fun handleInputActivate(input: Input) {
        if (this.isInputActive(input)) return
        val currentTime = this.totalTime
        this.activeInputs[input] = currentTime
        this.onInputActivate(input, currentTime)
    }

    protected open fun handleInputRepeat(input: Input) {
        val lastPressedAt = this.getInputLastActiveAt(input) ?: return
        val currentTime = this.totalTime
        if (currentTime - lastPressedAt < this.inputRepeatInterval) return
        this.activeInputs[input] = currentTime
        this.onInputRepeat(input, this.totalTime)
    }

    protected open fun handleInputDeactivate(input: Input) {
        this.activeInputs.remove(input) ?: return
        this.onInputDeactivate(input, this.totalTime)
    }

    protected fun isInputActive(input: Input) = this.activeInputs.containsKey(input)

    protected fun getInputLastActiveAt(input: Input) = this.activeInputs[input]

    protected open fun onInputActivate(input: Input, currentTime: Long) = Unit

    protected open fun onInputRepeat(input: Input, currentTime: Long) = Unit

    protected open fun onInputDeactivate(input: Input, currentTime: Long) = Unit

    protected open fun onCursorMove(position: Point, currentTime: Long) = Unit

    protected open fun onStateChange(oldState: State, newState: State) = Unit

}
