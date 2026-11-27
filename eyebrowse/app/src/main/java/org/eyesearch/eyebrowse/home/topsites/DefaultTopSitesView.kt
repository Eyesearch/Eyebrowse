/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.eyesearch.eyebrowse.home.topsites

import mozilla.components.feature.top.sites.TopSite
import mozilla.components.feature.top.sites.view.TopSitesView
import org.eyesearch.eyebrowse.components.AppStore
import org.eyesearch.eyebrowse.components.appstate.AppAction
import org.eyesearch.eyebrowse.ext.sort
import org.eyesearch.eyebrowse.utils.Settings

class DefaultTopSitesView(
    val appStore: AppStore,
    val settings: Settings,
) : TopSitesView {

    override fun displayTopSites(topSites: List<TopSite>) {
        appStore.dispatch(
            AppAction.TopSitesChange(
                if (!settings.showContileFeature) {
                    topSites
                } else {
                    topSites.sort()
                },
            ),
        )
    }
}
