/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.home.sessioncontrol.viewholders

import android.view.View
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LifecycleOwner
import org.mozilla.fenix.BrowserDirection
import org.mozilla.fenix.HomeActivity
import org.mozilla.fenix.R
import org.mozilla.fenix.compose.ComposeViewHolder

/**
 * The two marks of our own house, side by side on the home screen: Indo Workspace first, Sankalp
 * second, in the order they were asked for.
 *
 * Only the marks are shown, no wordmark and no caption, so the row stays quiet and each one opens
 * the page it belongs to in a new tab of this browser.
 */
class OurAppsViewHolder(
    composeView: ComposeView,
    viewLifecycleOwner: LifecycleOwner,
) : ComposeViewHolder(composeView, viewLifecycleOwner) {

    companion object {
        val LAYOUT_ID = View.generateViewId()

        private const val INDO_WORKSPACE_URL = "https://indoworkspace.in/"
        private const val SANKALP_URL = "https://sankalp.indoworkspace.in/"
    }

    init {
        val horizontalPadding =
            composeView.resources.getDimensionPixelSize(R.dimen.home_item_horizontal_margin)
        composeView.setPadding(horizontalPadding, 0, horizontalPadding, 0)
    }

    @Composable
    override fun Content() {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OurAppTile(R.drawable.ic_indo_workspace, INDO_WORKSPACE_URL, Modifier.weight(1f))
            OurAppTile(R.drawable.ic_sankalp, SANKALP_URL, Modifier.weight(1f))
        }
    }

    @Composable
    private fun OurAppTile(@DrawableRes mark: Int, url: String, modifier: Modifier) {
        val context = LocalContext.current
        Box(
            modifier = modifier
                .height(72.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .clickable {
                    // The marks sit on white, so the tile stays white in both themes rather than
                    // turning into a bright square in the middle of a dark home screen.
                    (context as? HomeActivity)?.openToBrowserAndLoad(
                        searchTermOrURL = url,
                        newTab = true,
                        from = BrowserDirection.FromHome,
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(mark),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxHeight(0.72f)
                    .fillMaxWidth(0.72f),
                contentScale = ContentScale.Fit,
            )
        }
    }
}
