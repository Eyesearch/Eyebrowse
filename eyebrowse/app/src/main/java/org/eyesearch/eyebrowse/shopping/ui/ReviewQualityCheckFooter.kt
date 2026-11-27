/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.eyesearch.eyebrowse.shopping.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.eyesearch.eyebrowse.R
import org.eyesearch.eyebrowse.compose.LinkText
import org.eyesearch.eyebrowse.compose.LinkTextState
import org.eyesearch.eyebrowse.compose.annotation.LightDarkPreview
import org.eyesearch.eyebrowse.theme.EyebrowseTheme

/**
 * Review Quality Check footer with an embedded link to navigate to Fakespot.com.
 *
 * @param onLinkClick Invoked when the user clicks on the embedded link.
 */
@Composable
fun ReviewQualityCheckFooter(
    onLinkClick: () -> Unit,
) {
    val poweredByLinkText = stringResource(
        id = R.string.review_quality_check_powered_by_link,
        stringResource(id = R.string.shopping_product_name),
    )

    LinkText(
        text = stringResource(
            id = R.string.review_quality_check_powered_by_2,
            poweredByLinkText,
        ),
        linkTextStates = listOf(
            LinkTextState(
                text = poweredByLinkText,
                url = "",
                onClick = {
                    onLinkClick()
                },
            ),
        ),
        style = EyebrowseTheme.typography.body2.copy(
            color = EyebrowseTheme.colors.textSecondary,
        ),
        linkTextColor = EyebrowseTheme.colors.textAccent,
    )
}

@LightDarkPreview
@Composable
private fun ReviewQualityCheckFooterPreview() {
    EyebrowseTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(color = EyebrowseTheme.colors.layer1)
                .padding(all = 16.dp),
        ) {
            ReviewQualityCheckFooter(
                onLinkClick = {},
            )
        }
    }
}
