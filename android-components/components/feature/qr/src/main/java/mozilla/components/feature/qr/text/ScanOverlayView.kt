/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.feature.qr.text

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewConfiguration
import kotlin.math.abs
import kotlin.math.hypot

/**
 * Shows the captured frame and everything the user does with its text.
 *
 * The frame is drawn here rather than in a second view, so the picture, the markers and the
 * selection are all placed by one transform and can never drift apart. It fills the screen the way
 * a scanner shows a capture, and it can be pinched to zoom and dragged to pan, which is what makes
 * small text readable before selecting it.
 *
 * Recognised lines carry a light marker so the user can see what is readable. A long press selects
 * the word under the finger and puts a handle at each end of the selection; dragging a handle grows
 * or shrinks it word by word, and lifting the finger opens the action menu for everything selected.
 * A plain tap is the quick path: it selects one word and opens the menu for it straight away.
 *
 * @param onSelectionConfirmed Called with the selection when the user lifts a handle, or taps a
 * word, which is when the action menu should appear.
 * @param onSelectionCleared Called when the selection goes away, so the menu can go with it.
 * @param onSelectionMoved Called when the picture is zoomed or panned, so anything anchored to
 * the selection can follow it.
 */
class ScanOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
    private val onSelectionConfirmed: (ScanSelection) -> Unit = {},
    private val onSelectionCleared: () -> Unit = {},
    private val onSelectionMoved: () -> Unit = {},
) : View(context, attrs, defStyle) {

    private enum class Handle { START, END }

    private val density = resources.displayMetrics.density

    /** Radius of the drawn handle circle. */
    private val handleRadius = 10f * density

    /** How far from a handle centre a finger may land and still grab it. */
    private val handleTouchRadius = 30f * density

    /** A finger is imprecise, so a tap counts when it is slightly outside the word box too. */
    private val tapMargin = 8f * density

    /** Corner radius of a marker, which is what makes it read as a marker and not a box. */
    private val markerRadius = 8f * density

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop.toFloat()

    /** Backdrop behind a capture that does not fill the view, so the live preview never shows. */
    private val backdropPaint = Paint().apply {
        color = Color.BLACK
    }

    /** Light marker over every recognised line, so readable text is visible before a selection. */
    private val linePaint = Paint().apply {
        style = Paint.Style.FILL
        color = Color.argb(60, 255, 255, 255)
        isAntiAlias = true
    }

    /** The blue of a text selection, light enough to read the words through it. */
    private val selectionPaint = Paint().apply {
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

    private var frame: Bitmap? = null

    /**
     * The rectangle on screen the photo is shown in: the preview's own rectangle. A capture is
     * then exactly as big, and in exactly the same place, as the preview was a moment before.
     */
    private val target = RectF()
    private var words: List<ScanWord> = emptyList()
    private var selection: ScanSelection? = null
    private var draggingHandle: Handle? = null

    /** Scale that makes the frame cover the view, before any zoom the user applies. */
    private var baseScale = 1f
    private var zoom = 1f
    private var panX = 0f
    private var panY = 0f

    private var panning = false
    private var lastTouchX = 0f
    private var lastTouchY = 0f

    private val drawMatrix = Matrix()

    private val scaleDetector = ScaleGestureDetector(
        context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val current = frame ?: return false
                val previousScale = currentScale()
                if (previousScale <= 0f) return false

                // Keep the point under the fingers where it is while the zoom changes.
                val focusX = detector.focusX
                val focusY = detector.focusY
                val offset = offsets()
                val frameX = (focusX - offset.x) / previousScale
                val frameY = (focusY - offset.y) / previousScale

                zoom = (zoom * detector.scaleFactor).coerceIn(MIN_ZOOM, MAX_ZOOM)
                val scale = currentScale()
                panX = focusX - frameX * scale - (target.centerX() - current.width * scale / 2f)
                panY = focusY - frameY * scale - (target.centerY() - current.height * scale / 2f)
                clampPan()
                invalidate()
                notifySelectionMoved()
                return true
            }
        },
    )

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
     * Shows a captured frame where the preview was, with the zoom reset for a new capture.
     *
     * @param bitmap The frame the words were read from. The caller keeps ownership and may recycle
     * it after [clearFrame] or after another frame has been set.
     * @param bounds The rectangle the preview occupied on screen, which is where the photo is
     * shown. The whole view is used when it is null or empty.
     */
    fun setFrame(bitmap: Bitmap, bounds: RectF? = null) {
        frame = bitmap
        target.set(
            bounds?.takeIf { !it.isEmpty } ?: RectF(0f, 0f, width.toFloat(), height.toFloat()),
        )
        zoom = MIN_ZOOM
        panX = 0f
        panY = 0f
        updateBaseScale()
        invalidate()
    }

    /**
     * Drops the frame and everything on it, so the live preview shows through again.
     */
    fun clearFrame() {
        frame = null
        words = emptyList()
        selection = null
        draggingHandle = null
        zoom = MIN_ZOOM
        panX = 0f
        panY = 0f
        invalidate()
    }

    /**
     * Publishes the words found in the current frame.
     */
    fun setWords(words: List<ScanWord>) {
        this.words = words
        if (selection != null && words.isEmpty()) {
            selection = null
            onSelectionCleared()
        }
        invalidate()
    }

    /**
     * Drops the selection, keeping the frame and the words.
     */
    fun clearSelection() {
        if (selection == null) return
        selection = null
        invalidate()
        onSelectionCleared()
    }

    /**
     * @return where the selection sits on screen, so the action menu can be placed next to it,
     * or null when nothing is selected.
     */
    fun selectionBoundsInView(): RectF? = selection?.let { toView(it.bounds) }

    private fun updateBaseScale() {
        val current = frame ?: return
        if (target.isEmpty || current.width == 0 || current.height == 0) return
        // Cover the preview's rectangle. The capture is several megapixels, so this is a shrink
        // and never a stretch, and the picture looks exactly like the preview did.
        baseScale = maxOf(target.width() / current.width, target.height() / current.height)
    }

    private fun currentScale(): Float = baseScale * zoom

    private fun offsets(): PointF {
        val current = frame ?: return PointF()
        val scale = currentScale()
        return PointF(
            target.centerX() - current.width * scale / 2f + panX,
            target.centerY() - current.height * scale / 2f + panY,
        )
    }

    private fun clampPan() {
        val current = frame ?: return
        val scale = currentScale()
        val maxX = ((current.width * scale - target.width()) / 2f).coerceAtLeast(0f)
        val maxY = ((current.height * scale - target.height()) / 2f).coerceAtLeast(0f)
        panX = panX.coerceIn(-maxX, maxX)
        panY = panY.coerceIn(-maxY, maxY)
    }

    private fun toView(bounds: Rect): RectF {
        val scale = currentScale()
        val offset = offsets()
        return RectF(
            bounds.left * scale + offset.x,
            bounds.top * scale + offset.y,
            bounds.right * scale + offset.x,
            bounds.bottom * scale + offset.y,
        )
    }

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

    /**
     * Tells the screen that the selection has moved on the display, so anything anchored to it,
     * such as the action bar, can follow.
     */
    private fun notifySelectionMoved() {
        if (selection != null) onSelectionMoved()
    }

    private fun confirmSelection() {
        selection?.let(onSelectionConfirmed)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val current = frame ?: return
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backdropPaint)
        val scale = currentScale()
        val offset = offsets()
        drawMatrix.setScale(scale, scale)
        drawMatrix.postTranslate(offset.x, offset.y)
        canvas.drawBitmap(current, drawMatrix, null)

        // Every recognised line gets a light marker, so the user can see what is readable and
        // where to press without a wall of boxes.
        words.groupBy { it.lineIndex }.values.forEach { lineWords ->
            canvas.drawRoundRect(toView(unionOf(lineWords)), markerRadius, markerRadius, linePaint)
        }

        val selected = selection ?: return
        selected.selectedWords.groupBy { it.lineIndex }.values.forEach { lineWords ->
            canvas.drawRoundRect(toView(unionOf(lineWords)), markerRadius, markerRadius, selectionPaint)
        }
        drawHandle(canvas, handleCenter(selected.selectedWords.first(), isStart = true))
        drawHandle(canvas, handleCenter(selected.selectedWords.last(), isStart = false))
    }

    private fun unionOf(lineWords: List<ScanWord>): Rect =
        lineWords.map { it.bounds }.reduce { acc, rect -> Rect(acc).apply { union(rect) } }

    private fun drawHandle(canvas: Canvas, centre: PointF) {
        canvas.drawCircle(centre.x, centre.y, handleRadius, handlePaint)
        canvas.drawCircle(centre.x, centre.y, handleRadius, handleOutlinePaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (frame == null) return false

        scaleDetector.onTouchEvent(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val handle = handleAt(event.x, event.y)
                if (handle != null) {
                    draggingHandle = handle
                    parent?.requestDisallowInterceptTouchEvent(true)
                    return true
                }
                panning = false
                lastTouchX = event.x
                lastTouchY = event.y
                gestureDetector.onTouchEvent(event)
            }

            MotionEvent.ACTION_MOVE -> {
                val handle = draggingHandle
                if (handle != null) {
                    extendSelection(handle, event.x, event.y)
                    return true
                }
                if (!scaleDetector.isInProgress && zoom > MIN_ZOOM) {
                    val dx = event.x - lastTouchX
                    val dy = event.y - lastTouchY
                    if (!panning && hypot(dx, dy) > touchSlop) panning = true
                    if (panning) {
                        panX += dx
                        panY += dy
                        lastTouchX = event.x
                        lastTouchY = event.y
                        clampPan()
                        invalidate()
                        notifySelectionMoved()
                        return true
                    }
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
                if (!panning) gestureDetector.onTouchEvent(event)
                panning = false
            }

            MotionEvent.ACTION_CANCEL -> {
                draggingHandle = null
                panning = false
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
        updateBaseScale()
        clampPan()
    }

    private companion object {
        const val MIN_ZOOM = 1f
        const val MAX_ZOOM = 8f
    }
}
