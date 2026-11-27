/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.menu

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.FrameLayout
import android.widget.PopupWindow
import androidx.annotation.VisibleForTesting
import androidx.cardview.widget.CardView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.widget.PopupWindowCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import mozilla.components.browser.menu.BrowserMenu.Orientation.DOWN
import mozilla.components.browser.menu.BrowserMenu.Orientation.UP
import mozilla.components.browser.menu.view.DynamicWidthRecyclerView
import mozilla.components.browser.menu.view.ExpandableLayout
import mozilla.components.browser.menu.view.StickyItemPlacement
import mozilla.components.browser.menu.view.StickyItemsLinearLayoutManager
import mozilla.components.concept.menu.MenuStyle
import mozilla.components.support.ktx.android.view.isRTL
import mozilla.components.support.ktx.android.view.onNextGlobalLayout

/**
 * A popup menu composed of BrowserMenuItem objects.
 */
open class BrowserMenu internal constructor(
    internal val adapter: BrowserMenuAdapter,
) : View.OnAttachStateChangeListener {
    protected var currentPopup: PopupWindow? = null

    @VisibleForTesting
    internal var menuList: RecyclerView? = null
    internal var currAnchor: View? = null
    internal var isShown = false

    @VisibleForTesting
    internal lateinit var menuPositioningData: MenuPositioningData
    internal var backgroundColor: Int = Color.RED

    /**
     * @param anchor the view on which to pin the popup window.
     * @param orientation the preferred orientation to show the popup window.
     * @param style Custom styling for this menu.
     * @param endOfMenuAlwaysVisible when is set to true makes sure the bottom of the menu is always visible otherwise,
     *  the top of the menu is always visible.
     */
    @Suppress("InflateParams", "ComplexMethod")
    open fun show(
        anchor: View,
        orientation: Orientation = DOWN,
        style: MenuStyle? = null,
        endOfMenuAlwaysVisible: Boolean = false,
        onDismiss: () -> Unit = {},
    ): PopupWindow {
        var view = LayoutInflater.from(anchor.context).inflate(R.layout.mozac_browser_menu, null)

        adapter.menu = this

        menuList = view.findViewById<DynamicWidthRecyclerView>(R.id.mozac_browser_menu_recyclerView).apply {
            layoutManager = StickyItemsLinearLayoutManager.get<BrowserMenuAdapter>(
                anchor.context,
                StickyItemPlacement.BOTTOM,
                false,
            )

            adapter = this@BrowserMenu.adapter
            minWidth = style?.minWidth ?: resources.getDimensionPixelSize(R.dimen.mozac_browser_menu_width_min)
            maxWidth = style?.maxWidth ?: resources.getDimensionPixelSize(R.dimen.mozac_browser_menu_width_max)
        }

        setColors(view, style)

        menuList?.accessibilityDelegate = object : View.AccessibilityDelegate() {
            override fun onInitializeAccessibilityNodeInfo(
                host: View,
                info: AccessibilityNodeInfo,
            ) {
                super.onInitializeAccessibilityNodeInfo(host, info)
                info.collectionInfo =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        AccessibilityNodeInfo.CollectionInfo(
                            adapter.interactiveCount,
                            0,
                            false,
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        AccessibilityNodeInfo.CollectionInfo.obtain(
                            adapter.interactiveCount,
                            0,
                            false,
                        )
                    }
            }
        }

        // Data needed to infer whether to show a collapsed menu
        // And then to properly place it.
        menuPositioningData = inferMenuPositioningData(
            view as ViewGroup,
            anchor,
            MenuPositioningData(askedOrientation = orientation),
        )

        view = configureExpandableMenu(view, endOfMenuAlwaysVisible)

        if (useSideDrawer) {
            return showAsSideDrawer(anchor, view, onDismiss)
        }

        return getNewPopupWindow(view).apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            isFocusable = true
            elevation = view.resources.getDimension(R.dimen.mozac_browser_menu_elevation)

            setOnDismissListener {
                adapter.menu = null
                currentPopup = null
                isShown = false
                onDismiss()
            }

            displayPopup(menuPositioningData).also {
                anchor.addOnAttachStateChangeListener(this@BrowserMenu)
                currAnchor = anchor
            }
        }.also {
            currentPopup = it
            isShown = true
        }
    }

    /**
     * Eyebrowse: the menu is not a popup under its button any more. It is a full height panel that
     * comes in from the right, edge to edge, top of the screen to the bottom, with the rest of the
     * screen dimmed. Tapping the dimmed part closes it, which is what [PopupWindow] outside touch
     * does for us, and tapping an item does whatever it always did and closes the panel on the way.
     */
    private fun showAsSideDrawer(
        anchor: View,
        menuView: View,
        onDismiss: () -> Unit,
    ): PopupWindow {
        val context = anchor.context
        val screenWidth = context.resources.displayMetrics.widthPixels
        // Eyebrowse: the panel is half the screen and comes in from the left, so the other half is
        // the dimmed area you tap to close.
        val panelWidth = (screenWidth * DRAWER_SCREEN_FRACTION).toInt()

        val container = FrameLayout(context).apply {
            setBackgroundColor(Color.argb(DRAWER_SCRIM_ALPHA, 0, 0, 0))
            // The dimmed area is the popup itself, so a tap on it has to close the drawer. Without
            // this only the back button closes it, which is not what a drawer does.
            isClickable = true
            setOnClickListener { dismiss() }
        }

        // The panel is the menu card, but a card in a drawer is not a card: no compat padding, no
        // rounded corners, no elevation, so it sits flush against the top, the right and the bottom
        // edges with no gap around it.
        (menuView as? CardView)?.apply {
            useCompatPadding = false
            radius = 0f
            cardElevation = 0f
            setContentPadding(0, 0, 0, 0)
        }
        container.addView(
            menuView,
            FrameLayout.LayoutParams(
                panelWidth,
                FrameLayout.LayoutParams.MATCH_PARENT,
                Gravity.START,
            ),
        )

        return getNewPopupWindow(container).apply {
            // A drawer covers the window: the scrim is the popup, the panel is a child of it.
            width = WindowManager.LayoutParams.MATCH_PARENT
            height = WindowManager.LayoutParams.MATCH_PARENT
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            isFocusable = true
            isOutsideTouchable = true
            elevation = menuView.resources.getDimension(R.dimen.mozac_browser_menu_elevation)

            setOnDismissListener {
                adapter.menu = null
                currentPopup = null
                isShown = false
                onDismiss()
            }

            showAtLocation(anchor, Gravity.FILL, 0, 0).also {
                anchor.addOnAttachStateChangeListener(this@BrowserMenu)
                currAnchor = anchor
            }
        }.also {
            currentPopup = it
            isShown = true
        }
    }

    @VisibleForTesting
    internal fun configureExpandableMenu(
        view: ViewGroup,
        endOfMenuAlwaysVisible: Boolean,
    ): ViewGroup {
        // If the menu is placed at the bottom it should start as collapsed.
        if (menuPositioningData.inferredMenuPlacement is BrowserMenuPlacement.AnchoredToBottom.Dropdown ||
            menuPositioningData.inferredMenuPlacement is BrowserMenuPlacement.AnchoredToBottom.ManualAnchoring
        ) {
            val collapsingMenuIndexLimit = adapter.visibleItems.indexOfFirst { it.isCollapsingMenuLimit }
            val stickyFooterPosition = adapter.visibleItems.indexOfLast { it.isSticky }
            if (collapsingMenuIndexLimit > 0) {
                return ExpandableLayout.wrapContentInExpandableView(
                    view,
                    collapsingMenuIndexLimit,
                    stickyFooterPosition,
                ) { dismiss() }
            }
        } else {
            // The menu is by default set as a bottom one. Reconfigure it as a top one.
            menuList?.layoutManager = StickyItemsLinearLayoutManager.get<BrowserMenuAdapter>(
                view.context,
                StickyItemPlacement.TOP,
            )

            // By default the menu is laid out from and scrolled to top - showing the top most items.
            // For the top menu it may be desired to initially show the bottom most items.
            menuList?.let { list ->
                list.setEndOfMenuAlwaysVisibleCompact(
                    endOfMenuAlwaysVisible,
                    list.layoutManager as LinearLayoutManager,
                )
            }
        }

        return view
    }

    @VisibleForTesting
    internal fun getNewPopupWindow(view: ViewGroup): PopupWindow {
        // If the menu is expandable we need to give it all the possible space to expand.
        // Also, by setting MATCH_PARENT, expanding the menu will not expand the Window
        // of the PopupWindow which for a bottom anchored menu means glitchy animations.
        val popupHeight = if (view is ExpandableLayout) {
            WindowManager.LayoutParams.MATCH_PARENT
        } else {
            // Otherwise wrap the menu. Allowing it to be as big as the parent would result in
            // layout issues if the menu is smaller than the available screen estate.
            WindowManager.LayoutParams.WRAP_CONTENT
        }

        return PopupWindow(
            view,
            WindowManager.LayoutParams.WRAP_CONTENT,
            popupHeight,
        )
    }

    private fun RecyclerView.setEndOfMenuAlwaysVisibleCompact(
        endOfMenuAlwaysVisible: Boolean,
        layoutManager: LinearLayoutManager,
    ) {
        // In devices with Android 6 and below stackFromEnd is not working properly,
        // as a result, we have to provided a backwards support.
        // See: https://github.com/mozilla-mobile/android-components/issues/3211
        if (endOfMenuAlwaysVisible && Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) {
            scrollOnceToTheBottom(this)
        } else {
            layoutManager.stackFromEnd = endOfMenuAlwaysVisible
        }
    }

    @VisibleForTesting
    internal fun scrollOnceToTheBottom(recyclerView: RecyclerView) {
        recyclerView.onNextGlobalLayout {
            recyclerView.adapter?.let { recyclerView.scrollToPosition(it.itemCount - 1) }
        }
    }

    fun dismiss() {
        currentPopup?.dismiss()
    }

    fun invalidate() {
        menuList?.let { adapter.invalidate(it) }
    }

    @VisibleForTesting
    internal fun setColors(menuLayout: View, colorState: MenuStyle?) {
        val listParent: CardView = menuLayout.findViewById(R.id.mozac_browser_menu_menuView)
        backgroundColor = colorState?.backgroundColor?.let {
            listParent.setCardBackgroundColor(it)
            it.defaultColor
        } ?: listParent.cardBackgroundColor.defaultColor
    }

    companion object {
        /**
         * Eyebrowse: true means the menu opens as a full height drawer from the right instead of a
         * popup anchored to its button. Set to false to get the old popup back.
         */
        var useSideDrawer: Boolean = true

        private const val DRAWER_SCRIM_ALPHA = 120
        private const val DRAWER_SCREEN_FRACTION = 0.8f

        /**
         * Determines the orientation to be used for a menu based on the positioning of the [parent] in the layout.
         */
        fun determineMenuOrientation(parent: View?): Orientation {
            if (parent == null) {
                return DOWN
            }

            val params = parent.layoutParams
            return if (params is CoordinatorLayout.LayoutParams) {
                if ((params.gravity and Gravity.BOTTOM) == Gravity.BOTTOM) {
                    UP
                } else {
                    DOWN
                }
            } else {
                DOWN
            }
        }
    }

    enum class Orientation(val concept: mozilla.components.concept.menu.Orientation) {
        UP(mozilla.components.concept.menu.Orientation.UP),
        DOWN(mozilla.components.concept.menu.Orientation.DOWN),
    }

    override fun onViewDetachedFromWindow(v: View) {
        currentPopup?.dismiss()
        currAnchor?.removeOnAttachStateChangeListener(this)
    }

    override fun onViewAttachedToWindow(v: View) {
        // no-op
    }
}

@VisibleForTesting
internal fun PopupWindow.displayPopup(currentData: MenuPositioningData) {
    // Try to use the preferred orientation, if doesn't fit fallback to the best fit.
    when (currentData.inferredMenuPlacement) {
        is BrowserMenuPlacement.AnchoredToTop.Dropdown -> showPopupWithDownOrientation(currentData)
        is BrowserMenuPlacement.AnchoredToBottom.Dropdown -> showPopupWithUpOrientation(currentData)

        is BrowserMenuPlacement.AnchoredToTop.ManualAnchoring,
        is BrowserMenuPlacement.AnchoredToBottom.ManualAnchoring,
        -> showAtAnchorLocation(currentData)
        else -> {
            // no-op
        }
    }
}

@VisibleForTesting
internal fun PopupWindow.showPopupWithUpOrientation(menuPositioningData: MenuPositioningData) {
    val anchor = menuPositioningData.inferredMenuPlacement!!.anchor
    val xOffset = if (anchor.isRTL) -anchor.width else 0
    animationStyle = menuPositioningData.inferredMenuPlacement.animation

    // Positioning the menu above and overlapping the anchor.
    val yOffset = if (menuPositioningData.availableHeightToBottom < 0) {
        // The anchor is partially below of the bottom of the screen, let's make the menu completely visible.
        menuPositioningData.availableHeightToBottom - menuPositioningData.containerViewHeight
    } else {
        -menuPositioningData.containerViewHeight
    }
    showAsDropDown(anchor, xOffset, yOffset)
}

private fun PopupWindow.showPopupWithDownOrientation(menuPositioningData: MenuPositioningData) {
    val anchor = menuPositioningData.inferredMenuPlacement!!.anchor
    val xOffset = if (anchor.isRTL) -anchor.width else 0
    animationStyle = menuPositioningData.inferredMenuPlacement.animation
    // Menu should overlay the anchor.
    showAsDropDown(anchor, xOffset, -anchor.height)
}

private fun PopupWindow.showAtAnchorLocation(menuPositioningData: MenuPositioningData) {
    val anchor = menuPositioningData.inferredMenuPlacement!!.anchor
    val anchorPosition = IntArray(2)
    animationStyle = menuPositioningData.inferredMenuPlacement.animation

    anchor.getLocationOnScreen(anchorPosition)
    val (x, y) = anchorPosition
    PopupWindowCompat.setOverlapAnchor(this, true)
    showAtLocation(anchor, Gravity.START or Gravity.TOP, x, y)
}
