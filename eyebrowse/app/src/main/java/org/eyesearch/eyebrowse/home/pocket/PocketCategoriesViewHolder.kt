/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.eyesearch.eyebrowse.home.pocket

import android.view.View
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import mozilla.components.lib.state.ext.observeAsComposableState
import org.eyesearch.eyebrowse.R
import org.eyesearch.eyebrowse.components.components
import org.eyesearch.eyebrowse.compose.ComposeViewHolder
import org.eyesearch.eyebrowse.compose.SelectableChipColors
import org.eyesearch.eyebrowse.compose.annotation.LightDarkPreview
import org.eyesearch.eyebrowse.compose.home.HomeSectionHeader
import org.eyesearch.eyebrowse.theme.EyebrowseTheme
import org.eyesearch.eyebrowse.wallpapers.WallpaperState

internal const val POCKET_CATEGORIES_SELECTED_AT_A_TIME_COUNT = 8

/**
 * [RecyclerView.ViewHolder] for displaying the list of [PocketRecommendedStoriesCategory]s from
 * [AppStore].
 *
 * @param composeView [ComposeView] which will be populated with Jetpack Compose UI content.
 * @param viewLifecycleOwner [LifecycleOwner] to which this Composable will be tied to.
 * @param interactor [PocketStoriesInteractor] callback for user interaction.
 */
class PocketCategoriesViewHolder(
    composeView: ComposeView,
    viewLifecycleOwner: LifecycleOwner,
    private val interactor: PocketStoriesInteractor,
) : ComposeViewHolder(composeView, viewLifecycleOwner) {

    @Composable
    override fun Content() {
        val horizontalPadding =
            composeView.resources.getDimensionPixelSize(R.dimen.home_item_horizontal_margin)
        composeView.setPadding(horizontalPadding, 0, horizontalPadding, 0)

        val homeScreenReady = components.appStore
            .observeAsComposableState { state -> state.firstFrameDrawn }.value ?: false

        val categories = components.appStore
            .observeAsComposableState { state -> state.pocketStoriesCategories }.value
        val categoriesSelections = components.appStore
            .observeAsComposableState { state -> state.pocketStoriesCategoriesSelections }.value

        val wallpaperState = components.appStore
            .observeAsComposableState { state -> state.wallpaperState }.value ?: WallpaperState.default

        var (selectedBackgroundColor, unselectedBackgroundColor, selectedTextColor, unselectedTextColor) =
            SelectableChipColors.buildColors()
        wallpaperState.composeRunIfWallpaperCardColorsAreAvailable { cardColorLight, cardColorDark ->
            if (isSystemInDarkTheme()) {
                selectedBackgroundColor = cardColorDark
                unselectedBackgroundColor = cardColorLight
                selectedTextColor = EyebrowseTheme.colors.textActionPrimary
                unselectedTextColor = EyebrowseTheme.colors.textActionSecondary
            } else {
                selectedBackgroundColor = cardColorLight
                unselectedBackgroundColor = cardColorDark
                selectedTextColor = EyebrowseTheme.colors.textActionSecondary
                unselectedTextColor = EyebrowseTheme.colors.textActionPrimary
            }
        }

        val categoryColors = SelectableChipColors(
            selectedTextColor = selectedTextColor,
            unselectedTextColor = unselectedTextColor,
            selectedBackgroundColor = selectedBackgroundColor,
            unselectedBackgroundColor = unselectedBackgroundColor,
        )

        // See the detailed comment in PocketStoriesViewHolder for reasoning behind this change.
        if (!homeScreenReady) return
        Column {
            Spacer(Modifier.height(24.dp))

            PocketTopics(
                categoryColors = categoryColors,
                categories = categories ?: emptyList(),
                categoriesSelections = categoriesSelections ?: emptyList(),
                onCategoryClick = interactor::onCategoryClicked,
            )
        }
    }

    companion object {
        val LAYOUT_ID = View.generateViewId()
    }
}

@Composable
private fun PocketTopics(
    categories: List<PocketRecommendedStoriesCategory> = emptyList(),
    categoriesSelections: List<PocketRecommendedStoriesSelectedCategory> = emptyList(),
    categoryColors: SelectableChipColors = SelectableChipColors.buildColors(),
    onCategoryClick: (PocketRecommendedStoriesCategory) -> Unit,
) {
    Column {
        HomeSectionHeader(
            headerText = stringResource(R.string.pocket_stories_categories_header),
        )

        Spacer(Modifier.height(16.dp))

        PocketStoriesCategories(
            categories = categories,
            selections = categoriesSelections,
            modifier = Modifier.fillMaxWidth(),
            categoryColors = categoryColors,
            onCategoryClick = onCategoryClick,
        )
    }
}

@Composable
@LightDarkPreview
private fun PocketCategoriesViewHolderPreview() {
    EyebrowseTheme {
        PocketTopics(
            categories = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor"
                .split(" ")
                .map { PocketRecommendedStoriesCategory(it) },
            categoriesSelections = emptyList(),
            onCategoryClick = {},
        )
    }
}
