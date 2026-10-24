package com.sasayaki.service.bubble

import android.annotation.SuppressLint
import android.content.Context
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.FrameLayout
import kotlin.math.hypot

/**
 * Lets taps reach the Compose content and takes over once a finger moves past the touch
 * slop, reporting the drag in raw screen coordinates. Local coordinates would shift as
 * the window follows the finger and feed back into the drag.
 */
@SuppressLint("ViewConstructor")
internal class DraggableOverlayLayout(
    context: Context,
    private val onDragStart: () -> Unit,
    private val onDrag: (dx: Float, dy: Float) -> Unit,
    private val onDragEnd: () -> Unit
) : FrameLayout(context) {
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var downX = 0f
    private var downY = 0f
    private var dragging = false

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.rawX
                downY = event.rawY
                dragging = false
            }
            MotionEvent.ACTION_MOVE -> if (!dragging && hypot(event.rawX - downX, event.rawY - downY) > touchSlop) {
                dragging = true
                parent?.requestDisallowInterceptTouchEvent(true)
                onDragStart()
                return true
            }
        }
        return dragging
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.rawX
                downY = event.rawY
            }
            MotionEvent.ACTION_MOVE -> {
                if (!dragging && hypot(event.rawX - downX, event.rawY - downY) > touchSlop) {
                    dragging = true
                    onDragStart()
                }
                if (dragging) onDrag(event.rawX - downX, event.rawY - downY)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (dragging) onDragEnd()
                dragging = false
            }
        }
        return true
    }
}
