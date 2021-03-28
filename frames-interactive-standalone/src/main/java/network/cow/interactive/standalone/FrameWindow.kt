package network.cow.interactive.standalone

import network.cow.interactive.InteractiveFrame
import network.cow.interactive.Key
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
                    frame.setKeyPressed(Key.MOUSE_LEFT, true)
                } else if (event.button == MouseEvent.BUTTON3) {
                    frame.setKeyPressed(Key.MOUSE_RIGHT, true)
                }
            }

            override fun mouseReleased(event: MouseEvent) {
                if (frame.isPaused) return

                if (event.button == MouseEvent.BUTTON1) {
                    frame.setKeyPressed(Key.MOUSE_LEFT, false)
                } else if (event.button == MouseEvent.BUTTON3) {
                    frame.setKeyPressed(Key.MOUSE_RIGHT, false)
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

    private fun mapKey(keyCode: Int): Key? {
        when (keyCode) {
            KeyEvent.VK_W, KeyEvent.VK_UP -> return Key.KEY_UP
            KeyEvent.VK_A, KeyEvent.VK_LEFT -> return Key.KEY_LEFT
            KeyEvent.VK_S, KeyEvent.VK_DOWN -> return Key.KEY_DOWN
            KeyEvent.VK_D, KeyEvent.VK_RIGHT -> return Key.KEY_RIGHT
            KeyEvent.VK_SPACE -> return Key.KEY_SPACE
            KeyEvent.VK_SHIFT -> return Key.KEY_SHIFT
        }
        return null
    }

    override fun keyPressed(event: KeyEvent) {
        if (event.keyCode == KeyEvent.VK_ESCAPE) {
            this.frame = this.frameProvider()
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
        this.frame.setKeyPressed(key, true)
    }

    override fun keyReleased(event: KeyEvent) {
        val key = this.mapKey(event.keyCode) ?: return
        this.frame.setKeyPressed(key, false)
    }

    override fun keyTyped(event: KeyEvent) = Unit

}
