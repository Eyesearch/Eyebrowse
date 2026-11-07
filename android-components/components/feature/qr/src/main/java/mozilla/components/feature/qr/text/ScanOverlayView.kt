/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.qr.text

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs

/**
 * Draws a box around every recognised word and turns a tap or a double tap into a word selection.
 *
 * Coordinates come from the captured frame, which is a different size from this view, so the view
 * keeps a scale that maps frame pixels to view pixels. That is why the boxes stay on the words
 * even though the preview and the photo are different resolutions.
 *
 * @param onWordSelected Called with the selected word on a double tap.
 * @param onWordTapped Called with the word under a single tap, used to show the action menu.
 */
class ScanOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
    private val onWordSelected: (ScanWord) -> Unit = {},
    private val onWordTapped: (ScanWord) -> Unit = {},
) : View(context, attrs, defStyle) {

    private val boxPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = Color.WHITE
        isAntiAlias = true
    }

    private val selectedPaint = Paint().apply {
        style = Paint.Style.FILL
        color = Color.argb(90, 255, 255, 255)
    }

    private var words: List<ScanWord> = emptyList()
    private var selected: ScanWord? = null
    private var frameWidth = 0
    private var frameHeight = 0
    private var scale = 1f
    private var offsetX = 0f
    private var offsetY = 0f

    private val gestureDetector = GestureDetector(
        context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                wordAt(e.x, e.y)?.let {
                    selected = it
                    invalidate()
                    onWordTapped(it)
                }
                return true
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                wordAt(e.x, e.y)?.let {
                    selected = it
                    invalidate()
                    onWordSelected(it)
                }
                return true
            }
        },
    )

    /**
     * Publishes a new set of words for the current frame.
     *
     * @param words Words in frame pixel coordinates.
     * @param width Width of the captured frame.
     * @param height Height of the captured frame.
     */
    fun setWords(words: List<ScanWord>, width: Int, height: Int) {
        this.words = words
        this.frameWidth = width
        this.frameHeight = height
        if (selected != null && words.none { it.bounds == selected?.bounds }) {
            selected = null
        }
        updateScale()
        invalidate()
    }

    /**
     * Clears the boxes, used when the frame no longer holds text.
     */
    fun clear() {
        words = emptyList()
        selected = null
        invalidate()
    }

    private fun updateScale() {
        if (frameWidth == 0 || frameHeight == 0 || width == 0 || height == 0) return
        // Fit the frame inside the view, preserving aspect ratio, and centre it.
        scale = minOf(width.toFloat() / frameWidth, height.toFloat() / frameHeight)
        offsetX = (width - frameWidth * scale) / 2f
        offsetY = (height - frameHeight * scale) / 2f
    }

    private fun toView(bounds: Rect): RectF = RectF(
        bounds.left * scale + offsetX,
        bounds.top * scale + offsetY,
        bounds.right * scale + offsetX,
        bounds.bottom * scale + offsetY,
    )

    private fun wordAt(x: Float, y: Float): ScanWord? {
        var best: ScanWord? = null
        var bestDistance = Float.MAX_VALUE
        words.forEach { word ->
            val box = toView(word.bounds)
            // A finger is imprecise: accept a tap anywhere in the box, plus a small margin.
            box.inset(-8f, -8f)
            if (box.contains(x, y)) {
                val distance = abs(box.centerX() - x) + abs(box.centerY() - y)
                if (distance < bestDistance) {
                    bestDistance = distance
                    best = word
                }
            }
        }
        return best
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        words.forEach { word ->
            val box = toView(word.bounds)
            if (word == selected) {
                canvas.drawRect(box, selectedPaint)
            }
            canvas.drawRect(box, boxPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (words.isEmpty()) return false
        return gestureDetector.onTouchEvent(event)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateScale()
    }
}
