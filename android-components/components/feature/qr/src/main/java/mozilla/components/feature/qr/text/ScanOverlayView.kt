/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.qr.text

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.math.hypot

/**
 * Highlights the words the user selects, the way a scanner is expected to look.
 *
 * Nothing is drawn over the picture until the user touches it: a long press highlights the word
 * under the finger in blue and puts a handle at each end. Dragging a handle grows or shrinks the
 * highlight word by word, and lifting the finger opens the action menu for everything selected. A
 * plain tap is still the quick path: it selects one word and opens the menu for it straight away.
 *
 * Coordinates come from the captured frame, which is a different size from this view, so the view
 * keeps a scale that maps frame pixels to view pixels. That is why the highlight stays on the words
 * even though the preview and the photo are different resolutions.
 *
 * While a selection is on screen the words are frozen: [setWords] ignores new frames, so the
 * highlight does not move under the user's finger while they are dragging a handle.
 *
 * @param onSelectionConfirmed Called with the selection when the user lifts a handle, or taps a
 * word, which is when the action menu should appear.
 * @param onSelectionCleared Called when the selection goes away, so the menu can go with it.
 */
class ScanOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
    private val onSelectionConfirmed: (ScanSelection) -> Unit = {},
    private val onSelectionCleared: () -> Unit = {},
) : View(context, attrs, defStyle) {

    private enum class Handle { START, END }

    private val density = resources.displayMetrics.density

    /** Radius of the drawn handle circle. */
    private val handleRadius = 10f * density

    /** How far from a handle centre a finger may land and still grab it. */
    private val handleTouchRadius = 30f * density

    /** A finger is imprecise, so a tap counts when it is slightly outside the word box too. */
    private val tapMargin = 8f * density

    /** The blue of a text highlight, light enough to read the words through it. */
    private val highlightPaint = Paint().apply {
        style = Paint.Style.FILL
        color = Color.argb(120, 66, 133, 244)
        isAntiAlias = true
    }

    private val handlePaint = Paint().apply {
        style = Paint.Style.FILL
        color = Color.rgb(26, 115, 232)
        isAntiAlias = true
    }

    private val handleOutlinePaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f * density
        color = Color.WHITE
        isAntiAlias = true
    }

    /** Corner radius of the highlight, which is what makes it read as a marker and not a box. */
    private val highlightRadius = 8f * density

    private var words: List<ScanWord> = emptyList()
    private var selection: ScanSelection? = null
    private var draggingHandle: Handle? = null
    private var frameWidth = 0
    private var frameHeight = 0
    private var scale = 1f
    private var offsetX = 0f
    private var offsetY = 0f

    private val gestureDetector = GestureDetector(
        context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean = true

            override fun onLongPress(e: MotionEvent) {
                val index = wordIndexAt(e.x, e.y) ?: return
                selection = ScanSelection.at(words, index)
                invalidate()
            }

            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                val index = wordIndexAt(e.x, e.y)
                if (index == null) {
                    clearSelection()
                    return true
                }

                // Tapping inside a selection that is already there acts on all of it, so a drag
                // that ended up slightly short still gives the user the whole selection.
                val current = selection
                if (current != null && current.contains(index)) {
                    onSelectionConfirmed(current)
                    return true
                }

                val tapped = ScanSelection.at(words, index) ?: return true
                selection = tapped
                invalidate()
                onSelectionConfirmed(tapped)
                return true
            }
        },
    )

    /**
     * Publishes a new set of words for the current frame.
     *
     * Ignored while a selection is on screen, so recognition cannot move the boxes out from under
     * a finger that is dragging a handle.
     *
     * @param words Words in frame pixel coordinates.
     * @param width Width of the captured frame.
     * @param height Height of the captured frame.
     */
    fun setWords(words: List<ScanWord>, width: Int, height: Int) {
        if (selection != null) return
        this.words = words
        this.frameWidth = width
        this.frameHeight = height
        updateScale()
        invalidate()
    }

    /**
     * @return true while the user has words selected, which is when the frame must not change.
     */
    fun hasSelection(): Boolean = selection != null

    /**
     * Drops the selection and lets new frames in again, used once the action menu is answered.
     */
    fun clearSelection() {
        if (selection == null) return
        selection = null
        invalidate()
        onSelectionCleared()
    }

    /**
     * Clears the boxes, used when the frame no longer holds text.
     */
    fun clear() {
        words = emptyList()
        selection = null
        draggingHandle = null
        invalidate()
    }

    /**
     * @return where the selection sits on screen, so the action menu can be placed next to it,
     * or null when nothing is selected.
     */
    fun selectionBoundsInView(): RectF? = selection?.let { toView(it.bounds) }

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

    /**
     * @return index of the word under a point, or null when the point is on no word.
     */
    private fun wordIndexAt(x: Float, y: Float): Int? {
        var best: Int? = null
        var bestDistance = Float.MAX_VALUE
        words.forEachIndexed { index, word ->
            val box = RectF(toView(word.bounds))
            box.inset(-tapMargin, -tapMargin)
            if (box.contains(x, y)) {
                val distance = abs(box.centerX() - x) + abs(box.centerY() - y)
                if (distance < bestDistance) {
                    bestDistance = distance
                    best = index
                }
            }
        }
        return best
    }

    /**
     * @return index of the word closest to a point, used while a handle is dragged past the text.
     */
    private fun nearestWordIndex(x: Float, y: Float): Int? {
        var best: Int? = null
        var bestDistance = Float.MAX_VALUE
        words.forEachIndexed { index, word ->
            val box = toView(word.bounds)
            val distance = hypot(box.centerX() - x, box.centerY() - y)
            if (distance < bestDistance) {
                bestDistance = distance
                best = index
            }
        }
        return best
    }

    private fun handleCenter(word: ScanWord, isStart: Boolean): PointF {
        val box = toView(word.bounds)
        return PointF(if (isStart) box.left else box.right, box.centerY())
    }

    private fun handleAt(x: Float, y: Float): Handle? {
        val current = selection ?: return null
        val start = handleCenter(current.selectedWords.first(), isStart = true)
        val end = handleCenter(current.selectedWords.last(), isStart = false)
        val toStart = hypot(start.x - x, start.y - y)
        val toEnd = hypot(end.x - x, end.y - y)
        return when {
            toStart <= handleTouchRadius && toStart <= toEnd -> Handle.START
            toEnd <= handleTouchRadius -> Handle.END
            else -> null
        }
    }

    private fun extendSelection(handle: Handle, x: Float, y: Float) {
        val current = selection ?: return
        val index = nearestWordIndex(x, y) ?: return
        selection = when (handle) {
            Handle.START -> current.withStart(index)
            Handle.END -> current.withEnd(index)
        }
        invalidate()
    }

    private fun confirmSelection() {
        selection?.let(onSelectionConfirmed)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // Nothing is drawn until the user selects something: the picture is the point, and boxes
        // over every word make a page of text unreadable.
        val current = selection ?: return
        drawHighlight(canvas, current)
        drawHandle(canvas, handleCenter(current.selectedWords.first(), isStart = true))
        drawHandle(canvas, handleCenter(current.selectedWords.last(), isStart = false))
    }

    /**
     * Paints the selection one line at a time as a rounded blue marker.
     *
     * A single rectangle around everything would box the whole block the words happen to sit in,
     * which reads as a frame that has nothing to do with the text the user picked.
     */
    private fun drawHighlight(canvas: Canvas, selection: ScanSelection) {
        selection.selectedWords
            .groupBy { it.lineIndex }
            .values
            .forEach { lineWords ->
                val line = lineWords
                    .map { it.bounds }
                    .reduce { acc, rect -> Rect(acc).apply { union(rect) } }
                canvas.drawRoundRect(toView(line), highlightRadius, highlightRadius, highlightPaint)
            }
    }

    private fun drawHandle(canvas: Canvas, centre: PointF) {
        canvas.drawCircle(centre.x, centre.y, handleRadius, handlePaint)
        canvas.drawCircle(centre.x, centre.y, handleRadius, handleOutlinePaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (words.isEmpty()) return false

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val handle = handleAt(event.x, event.y)
                if (handle != null) {
                    draggingHandle = handle
                    parent?.requestDisallowInterceptTouchEvent(true)
                    return true
                }
                gestureDetector.onTouchEvent(event)
            }

            MotionEvent.ACTION_MOVE -> {
                val handle = draggingHandle
                if (handle != null) {
                    extendSelection(handle, event.x, event.y)
                    return true
                }
                gestureDetector.onTouchEvent(event)
            }

            MotionEvent.ACTION_UP -> {
                val handle = draggingHandle
                if (handle != null) {
                    draggingHandle = null
                    extendSelection(handle, event.x, event.y)
                    parent?.requestDisallowInterceptTouchEvent(false)
                    confirmSelection()
                    return true
                }
                gestureDetector.onTouchEvent(event)
            }

            MotionEvent.ACTION_CANCEL -> {
                draggingHandle = null
                parent?.requestDisallowInterceptTouchEvent(false)
                gestureDetector.onTouchEvent(event)
            }
        }

        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateScale()
    }
}
