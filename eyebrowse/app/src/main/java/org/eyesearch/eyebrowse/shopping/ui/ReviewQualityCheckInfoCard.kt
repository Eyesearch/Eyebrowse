/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.eyesearch.eyebrowse.shopping.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import org.eyesearch.eyebrowse.R
import org.eyesearch.eyebrowse.compose.LinkText
import org.eyesearch.eyebrowse.compose.LinkTextState
import org.eyesearch.eyebrowse.compose.annotation.LightDarkPreview
import org.eyesearch.eyebrowse.compose.button.PrimaryButton
import org.eyesearch.eyebrowse.shopping.ui.ext.headingResource
import org.eyesearch.eyebrowse.theme.EyebrowseTheme

/**
 * Review Quality Check Info Card UI.
 *
 * @param title The primary text of the info message.
 * @param type The [ReviewQualityCheckInfoType] of message to display.
 * @param modifier Modifier to be applied to the card.
 * @param description The optional secondary piece of text.
 * @param footer An optional piece of text with a clickable link.
 * @param buttonText The text to show in the optional button.
 */
@Composable
fun ReviewQualityCheckInfoCard(
    title: String,
    type: ReviewQualityCheckInfoType,
    modifier: Modifier = Modifier,
    description: String? = null,
    footer: Pair<String, LinkTextState>? = null,
    buttonText: InfoCardButtonText? = null,
) {
    ReviewQualityCheckCard(
        modifier = modifier,
        backgroundColor = type.cardBackgroundColor,
        contentPadding = PaddingValues(
            horizontal = 12.dp,
            vertical = 8.dp,
        ),
        elevation = 0.dp,
    ) {
        val titleContentDescription = headingResource(title)

        Row {
            when (type) {
                ReviewQualityCheckInfoType.Warning -> {
                    InfoCardIcon(iconId = R.drawable.mozac_ic_warning_fill_24)
                }

                ReviewQualityCheckInfoType.Confirmation -> {
                    InfoCardIcon(iconId = R.drawable.mozac_ic_checkmark_24)
                }

                ReviewQualityCheckInfoType.Error,
                ReviewQualityCheckInfoType.Info,
                ReviewQualityCheckInfoType.AnalysisUpdate,
                -> {
                    InfoCardIcon(iconId = R.drawable.mozac_ic_information_fill_24)
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = title,
                    color = EyebrowseTheme.colors.textPrimary,
                    style = EyebrowseTheme.typography.headline8,
                    modifier = Modifier.semantics {
                        heading()
                        contentDescription = titleContentDescription
                    },
                )

                description?.let {
                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = description,
                        color = EyebrowseTheme.colors.textPrimary,
                        style = EyebrowseTheme.typography.body2,
                    )
                }

                footer?.let {
                    Spacer(modifier = Modifier.height(4.dp))

                    LinkText(
                        text = it.first,
                        linkTextStates = listOf(it.second),
                        style = EyebrowseTheme.typography.body2.copy(
                            color = EyebrowseTheme.colors.textPrimary,
                        ),
                        linkTextColor = EyebrowseTheme.colors.textPrimary,
                        linkTextDecoration = TextDecoration.Underline,
                    )
                }
            }
        }

        buttonText?.let {
            Spacer(modifier = Modifier.height(8.dp))

            PrimaryButton(
                text = it.text,
                textColor = type.buttonTextColor,
                backgroundColor = type.buttonBackgroundColor,
                onClick = it.onClick,
            )
        }
    }
}

@Composable
private fun InfoCardIcon(
    iconId: Int,
    modifier: Modifier = Modifier,
) {
    Icon(
        painter = painterResource(id = iconId),
        contentDescription = null,
        tint = EyebrowseTheme.colors.iconPrimary,
        modifier = modifier,
    )
}

/**
 * The possible types of a [ReviewQualityCheckInfoCard].
 */
enum class ReviewQualityCheckInfoType {

    Warning,
    Confirmation,
    Error,
    Info,
    AnalysisUpdate,
    ;

    val cardBackgroundColor: Color
        @Composable
        get() = when (this) {
            Warning -> EyebrowseTheme.colors.layerWarning
            Confirmation -> EyebrowseTheme.colors.layerConfirmation
            Error -> EyebrowseTheme.colors.layerError
            Info -> EyebrowseTheme.colors.layerInfo
            AnalysisUpdate -> Color.Transparent
        }

    val buttonBackgroundColor: Color
        @Composable
        get() = when (this) {
            Warning -> EyebrowseTheme.colors.actionWarning
            Confirmation -> EyebrowseTheme.colors.actionConfirmation
            Error -> EyebrowseTheme.colors.actionError
            Info -> EyebrowseTheme.colors.actionInfo
            AnalysisUpdate -> EyebrowseTheme.colors.actionSecondary
        }

    val buttonTextColor: Color
        @Composable
        get() = when {
            this == Info && !isSystemInDarkTheme() -> EyebrowseTheme.colors.textOnColorPrimary
            this == AnalysisUpdate -> EyebrowseTheme.colors.textActionSecondary
            else -> EyebrowseTheme.colors.textPrimary
        }
}

/**
 * Model for the optional button in a [ReviewQualityCheckInfoCard].
 *
 * @property text The text to show in the button.
 * @property onClick The callback to invoke when the button is clicked.
 */
data class InfoCardButtonText(
    val text: String,
    val onClick: () -> Unit,
)

private class PreviewModelParameterProvider : PreviewParameterProvider<ReviewQualityCheckInfoType> {

    override val values = enumValues<ReviewQualityCheckInfoType>().asSequence()
}

@LightDarkPreview
@Composable
private fun InfoCardPreview(
    @PreviewParameter(PreviewModelParameterProvider::class) type: ReviewQualityCheckInfoType,
) {
    EyebrowseTheme {
        Box(
            modifier = Modifier
                .background(color = EyebrowseTheme.colors.layer1)
                .padding(16.dp),
        ) {
            ReviewQualityCheckInfoCard(
                title = "Title text",
                type = type,
                modifier = Modifier.fillMaxWidth(),
                description = "Description text",
                footer = "Primary link text with an underlined hyperlink." to LinkTextState(
                    text = "underlined hyperlink",
                    url = "https://www.mozilla.org",
                    onClick = {},
                ),
                buttonText = InfoCardButtonText(
                    text = "Button text",
                    onClick = {},
                ),
            )
        }
    }
}
