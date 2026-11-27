/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.home.sessioncontrol.viewholders

import android.view.View
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.LifecycleOwner
import org.mozilla.fenix.BrowserDirection
import org.mozilla.fenix.HomeActivity
import org.mozilla.fenix.R
import org.mozilla.fenix.compose.ComposeViewHolder

/**
 * Six tiles under the toolbar, built like a pinned top site: the mark on top, the name under it,
 * a hairline edge and a soft shadow, so each one reads as a button and not as an empty white box.
 *
 * Row one is Indo Workspace, Sankalp and Zoho. Row two is IRCTC, DigiLocker and Passport Seva.
 * Each tile opens its page in a new tab of this browser.
 */
class OurAppsViewHolder(
    composeView: ComposeView,
    viewLifecycleOwner: LifecycleOwner,
) : ComposeViewHolder(composeView, viewLifecycleOwner) {

    private data class AppTile(
        val label: String,
        val url: String,
        val mark: Int,
    )

    companion object {
        val LAYOUT_ID = View.generateViewId()

        private val TILE_SHAPE = RoundedCornerShape(12.dp)
        private val TILE_EDGE = Color(0xFFE3E3E8)
        private val LABEL_COLOUR = Color(0xFF1B1B1F)

        private val FIRST_ROW = listOf(
            AppTile("Indo Workspace", "https://indoworkspace.in/", R.drawable.ic_indo_workspace),
            AppTile("Sankalp", "https://sankalp.indoworkspace.in/", R.drawable.ic_sankalp),
            AppTile("Zoho", "https://www.zoho.com/", R.drawable.ic_zoho),
        )

        private val SECOND_ROW = listOf(
            AppTile("IRCTC", "https://www.irctc.co.in/", R.drawable.ic_irctc),
            AppTile("DigiLocker", "https://www.digilocker.gov.in/", R.drawable.ic_digilocker),
            AppTile("Passport Seva", "https://www.passportindia.gov.in/", R.drawable.ic_passport_seva),
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
        Column(
            modifier = modifier
                .height(86.dp)
                .shadow(elevation = 1.dp, shape = TILE_SHAPE)
                .clip(TILE_SHAPE)
                .background(Color.White)
                .border(width = 1.dp, color = TILE_EDGE, shape = TILE_SHAPE)
                .clickable {
                    // The marks sit on white, so the tile stays white in both themes rather than
                    // turning into a bright square in the middle of a dark home screen.
                    (context as? HomeActivity)?.openToBrowserAndLoad(
                        searchTermOrURL = tile.url,
                        newTab = true,
                        from = BrowserDirection.FromHome,
                    )
                }
                .padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Image(
                painter = painterResource(tile.mark),
                contentDescription = tile.label,
                modifier = Modifier.size(36.dp),
                contentScale = ContentScale.Fit,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = tile.label,
                color = LABEL_COLOUR,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}
