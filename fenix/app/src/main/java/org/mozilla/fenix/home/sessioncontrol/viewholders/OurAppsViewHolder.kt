/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.home.sessioncontrol.viewholders

import android.view.View
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.LifecycleOwner
import org.mozilla.fenix.BrowserDirection
import org.mozilla.fenix.HomeActivity
import org.mozilla.fenix.R
import org.mozilla.fenix.compose.ComposeViewHolder

/**
 * Six tiles under the toolbar: our own two marks first, then the services beside them.
 *
 * Row one is Indo Workspace, Sankalp and Zoho. Row two is IRCTC, DigiLocker and Passport Seva.
 * Two of those marks are not ours and are not in the app yet, so those two tiles show their name
 * until the real files are dropped in; a tile with a name is honest, a made-up logo is not.
 *
 * Each tile opens its page in a new tab of this browser.
 */
class OurAppsViewHolder(
    composeView: ComposeView,
    viewLifecycleOwner: LifecycleOwner,
) : ComposeViewHolder(composeView, viewLifecycleOwner) {

    private data class AppTile(
        val label: String,
        val url: String,
        val mark: Int? = null,
    )

    companion object {
        val LAYOUT_ID = View.generateViewId()

        private val FIRST_ROW = listOf(
            AppTile("Indo Workspace", "https://indoworkspace.in/", R.drawable.ic_indo_workspace),
            AppTile("Sankalp", "https://sankalp.indoworkspace.in/", R.drawable.ic_sankalp),
            AppTile("Zoho", "https://www.zoho.com/", R.drawable.ic_zoho),
        )

        private val SECOND_ROW = listOf(
            AppTile("IRCTC", "https://www.irctc.co.in/"),
            AppTile("DigiLocker", "https://www.digilocker.gov.in/", R.drawable.ic_digilocker),
            AppTile("Passport Seva", "https://www.passportindia.gov.in/"),
        )
    }

    init {
        val horizontalPadding =
            composeView.resources.getDimensionPixelSize(R.dimen.home_item_horizontal_margin)
        composeView.setPadding(horizontalPadding, 0, horizontalPadding, 0)
    }

    @Composable
    override fun Content() {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            TileRow(FIRST_ROW)
            TileRow(SECOND_ROW)
        }
    }

    @Composable
    private fun TileRow(tiles: List<AppTile>) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            tiles.forEach { tile ->
                Tile(tile, Modifier.weight(1f))
            }
        }
    }

    @Composable
    private fun Tile(tile: AppTile, modifier: Modifier) {
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
                        searchTermOrURL = tile.url,
                        newTab = true,
                        from = BrowserDirection.FromHome,
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            val mark = tile.mark
            if (mark != null) {
                Image(
                    painter = painterResource(mark),
                    contentDescription = tile.label,
                    modifier = Modifier.size(40.dp),
                    contentScale = ContentScale.Fit,
                )
            } else {
                Text(
                    text = tile.label,
                    color = Color(0xFF1B1B1F),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 6.dp),
                )
            }
        }
    }
}
