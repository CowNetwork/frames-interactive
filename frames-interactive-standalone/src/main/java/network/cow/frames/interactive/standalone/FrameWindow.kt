package network.cow.frames.interactive.standalone

import network.cow.frames.interactive.InteractiveFrame
import network.cow.frames.interactive.Input
import network.cow.frames.interactive.State
import java.awt.BorderLayout
import java.awt.Point
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.JCheckBox
import javax.swing.JFrame
import kotlin.system.exitProcess

/**
 * @author Benedikt Wüller
 */
class FrameWindow(private val frameProvider: () -> InteractiveFrame, title: String) : KeyListener {

    private var isPaused: Boolean = false

    private var frame = this.frameProvider()
    private val window = JFrame(title)
    private val viewport = FrameViewport(this.window)

    init {
        this.window.layout = BorderLayout()
        this.window.isResizable = false
        this.window.add(this.viewport, BorderLayout.CENTER)

        val debugCheckbox = JCheckBox("Debug Mode", this.viewport.debug)
        debugCheckbox.addChangeListener {
            this.viewport.debug = debugCheckbox.isSelected
            this.viewport.requestFocus()
        }
        this.window.add(debugCheckbox, BorderLayout.PAGE_END)

        this.viewport.addKeyListener(this)
        this.viewport.debug = false
        this.viewport.requestFocus()

        this.viewport.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(event: MouseEvent) {
                if (frame.isPaused) return

                if (event.button == MouseEvent.BUTTON1) {
                    frame.setInputActive(Input.INTERACT_PRIMARY, true)
                }
            }

            override fun mouseReleased(event: MouseEvent) {
                if (frame.isPaused) return

                if (event.button == MouseEvent.BUTTON1) {
                    frame.setInputActive(Input.INTERACT_PRIMARY, false)
                }
            }
        })

        this.viewport.addMouseMotionListener(object : MouseAdapter() {
            override fun mouseMoved(event: MouseEvent) {
                if (frame.isPaused) return
                frame.setViewportCursorPosition(Point(event.x, event.y))
            }

            override fun mouseDragged(event: MouseEvent) {
                if (frame.isPaused) return
                frame.setViewportCursorPosition(Point(event.x, event.y))
            }
        })

        this.reset()
    }

    fun reset() {
        this.frame = this.frameProvider()
        this.frame.addStateListener { _, newState ->
            if (newState != State.INACTIVE) return@addStateListener
            this.reset()
        }
    }

    fun start() {
        val loop = Thread {
            while (!Thread.currentThread().isInterrupted) {
                if (this.isPaused) continue
                this.tick()
            }
        }
        loop.isDaemon = true
        loop.start()

        this.window.addWindowListener(object : WindowAdapter() {
            override fun windowClosing(event: WindowEvent) {
                super.windowClosing(event)
                loop.interrupt()
                exitProcess(0)
            }
        })
        this.window.isVisible = true
        this.window.pack()
    }

    private fun tick() {
        this.frame.tick()
        if (!this.frame.hasUpdatedSections()) return

        this.viewport.sections.clear()
        this.viewport.sections.addAll(this.frame.pullUpdatedSections())

        this.viewport.currentImage = this.frame.getViewport()
        this.viewport.paintImmediately(0, 0, this.viewport.width, this.viewport.height)
    }

    private fun mapKey(keyCode: Int): Input? {
        when (keyCode) {
            KeyEvent.VK_W, KeyEvent.VK_UP -> return Input.KEY_UP
            KeyEvent.VK_A, KeyEvent.VK_LEFT -> return Input.KEY_LEFT
            KeyEvent.VK_S, KeyEvent.VK_DOWN -> return Input.KEY_DOWN
            KeyEvent.VK_D, KeyEvent.VK_RIGHT -> return Input.KEY_RIGHT
            KeyEvent.VK_SPACE -> return Input.KEY_SPACE
            KeyEvent.VK_SHIFT -> return Input.KEY_SHIFT
        }
        return null
    }

    override fun keyPressed(event: KeyEvent) {
        if (event.keyCode == KeyEvent.VK_ESCAPE) {
            this.reset()
            return
        }

        if (this.frame.state == State.INACTIVE) {
            this.frame.state = State.ACTIVE
            return
        }

        if (event.keyCode == KeyEvent.VK_P) {
            if (this.frame.isPaused) {
                this.frame.resume()
            } else {
                this.frame.pause()
            }
            return
        }

        val key = this.mapKey(event.keyCode) ?: return
        this.frame.setInputActive(key, true)
    }

    override fun keyReleased(event: KeyEvent) {
        val key = this.mapKey(event.keyCode) ?: return
        this.frame.setInputActive(key, false)
    }

    override fun keyTyped(event: KeyEvent) = Unit

}
