package org.eyesearch.eyebrowse.ui

import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import org.junit.Rule
import org.junit.Test
import org.eyesearch.eyebrowse.customannotations.SmokeTest
import org.eyesearch.eyebrowse.helpers.AppAndSystemHelper.runWithLauncherIntent
import org.eyesearch.eyebrowse.helpers.HomeActivityIntentTestRule
import org.eyesearch.eyebrowse.helpers.TestSetup
import org.eyesearch.eyebrowse.ui.robots.homeScreen

class OnboardingTest : TestSetup() {

    @get:Rule
    val activityTestRule =
        AndroidComposeTestRule(
            HomeActivityIntentTestRule.withDefaultSettingsOverrides(launchActivity = false),
        ) { it.activity }

    // TestRail link: https://testrail.stage.mozaws.net/index.php?/cases/view/2122321
    @Test
    fun verifyFirstOnboardingCardItemsTest() {
        runWithLauncherIntent(activityTestRule) {
            homeScreen {
                verifyFirstOnboardingCard(activityTestRule)
            }
        }
    }

    // TestRail link: https://testrail.stage.mozaws.net/index.php?/cases/view/2122334
    @SmokeTest
    @Test
    fun verifyFirstOnboardingCardItemsFunctionalityTest() {
        runWithLauncherIntent(activityTestRule) {
            homeScreen {
                clickDefaultCardNotNowOnboardingButton(activityTestRule)
                verifySecondOnboardingCard(activityTestRule)
                swipeSecondOnboardingCardToRight()
            }.clickSetAsDefaultBrowserOnboardingButton(activityTestRule) {
                verifyAndroidDefaultAppsMenuAppears()
            }.goBackToOnboardingScreen {
                verifySecondOnboardingCard(activityTestRule)
            }
        }
    }

    // TestRail link: https://testrail.stage.mozaws.net/index.php?/cases/view/2122343
    @Test
    fun verifySecondOnboardingCardItemsTest() {
        runWithLauncherIntent(activityTestRule) {
            homeScreen {
                clickDefaultCardNotNowOnboardingButton(activityTestRule)
                verifySecondOnboardingCard(activityTestRule)
            }
        }
    }

    // TestRail link: https://testrail.stage.mozaws.net/index.php?/cases/view/2122344
    @SmokeTest
    @Test
    fun verifyThirdOnboardingCardSignInFunctionalityTest() {
        runWithLauncherIntent(activityTestRule) {
            homeScreen {
                clickDefaultCardNotNowOnboardingButton(activityTestRule)
                verifySecondOnboardingCard(activityTestRule)
                clickAddSearchWidgetNotNowOnboardingButton(activityTestRule)
                verifyThirdOnboardingCard(activityTestRule)
            }.clickSignInOnboardingButton(activityTestRule) {
                verifyTurnOnSyncMenu()
            }
        }
    }
}
