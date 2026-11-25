/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.util.Log
import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import androidx.activity.result.contract.ActivityResultContracts
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.widget.Toast
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AlertDialog
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDirections
import androidx.navigation.findNavController
import androidx.navigation.fragment.navArgs
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mozilla.components.browser.state.state.selectedOrDefaultSearchEngine
import mozilla.components.concept.sync.AccountObserver
import mozilla.components.concept.sync.AuthType
import mozilla.components.concept.sync.OAuthAccount
import mozilla.components.concept.sync.Profile
import mozilla.components.service.glean.private.NoExtras
import mozilla.components.support.ktx.android.view.showKeyboard
import mozilla.components.ui.widgets.withCenterAlignedButtons
import org.eyesearch.eyebrowse.core.EyeblockSupport
import org.mozilla.fenix.BrowserDirection
import org.mozilla.fenix.Config
import org.mozilla.fenix.FeatureFlags
import org.mozilla.fenix.GleanMetrics.Addons
import org.mozilla.fenix.GleanMetrics.CookieBanners
import org.mozilla.fenix.GleanMetrics.Events
import org.mozilla.fenix.GleanMetrics.TrackingProtection
import org.mozilla.fenix.HomeActivity
import org.eyesearch.eyebrowse.account.IndoAccountClient
import org.eyesearch.eyebrowse.account.IndoAccountEntry
import org.eyesearch.eyebrowse.account.IndoAccountError
import org.eyesearch.eyebrowse.account.IndoIdentity
import org.eyesearch.eyebrowse.account.IndoAccountStorage
import org.eyesearch.eyebrowse.vpn.EyebrowseVpnService
import org.eyesearch.eyebrowse.vpn.VpnController
import org.eyesearch.eyebrowse.sync.SyncClient
import org.eyesearch.eyebrowse.sync.SyncFactory
import org.eyesearch.eyebrowse.sync.SyncType
import org.mozilla.fenix.R
import org.mozilla.fenix.components.accounts.FenixFxAEntryPoint
import org.mozilla.fenix.ext.application
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.getPreferenceKey
import org.mozilla.fenix.ext.navigateToNotificationsSettings
import org.mozilla.fenix.ext.openChromeAppInfo
import org.mozilla.fenix.ext.openSetDefaultBrowserOption
import org.mozilla.fenix.ext.requireComponents
import org.mozilla.fenix.ext.settings
import org.mozilla.fenix.ext.showToolbar
import org.mozilla.fenix.nimbus.FxNimbus
import org.mozilla.fenix.perf.ProfilerViewModel
import org.mozilla.fenix.utils.Settings
import kotlin.system.exitProcess
import org.mozilla.fenix.GleanMetrics.Settings as SettingsMetrics

@Suppress("LargeClass", "TooManyFunctions")
class SettingsFragment : PreferenceFragmentCompat() {

    private val args by navArgs<SettingsFragmentArgs>()
    private val profilerViewModel: ProfilerViewModel by activityViewModels()

    // A flag used to track if we're going through the onCreate->onStart->onResume lifecycle chain.
    // If it's set to `true`, code in `onResume` can assume that `onCreate` executed a moment prior.
    // This flag is set to `false` at the end of `onResume`.
    private var creatingFragment = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        val booleanPreferenceTelemetryAllowList = listOf(
            requireContext().getString(R.string.pref_key_show_search_suggestions),
            requireContext().getString(R.string.pref_key_remote_debugging),
            requireContext().getString(R.string.pref_key_telemetry),
            requireContext().getString(R.string.pref_key_tracking_protection),
            requireContext().getString(R.string.pref_key_search_bookmarks),
            requireContext().getString(R.string.pref_key_search_browsing_history),
            requireContext().getString(R.string.pref_key_show_clipboard_suggestions),
            requireContext().getString(R.string.pref_key_show_search_engine_shortcuts),
            requireContext().getString(R.string.pref_key_open_links_in_a_private_tab),
            requireContext().getString(R.string.pref_key_sync_logins),
            requireContext().getString(R.string.pref_key_eyebrowse_sync_bookmarks),
            requireContext().getString(R.string.pref_key_sync_history),
            requireContext().getString(R.string.pref_key_show_voice_search),
            requireContext().getString(R.string.pref_key_show_search_suggestions_in_private),
        )

        preferenceManager?.sharedPreferences
            ?.registerOnSharedPreferenceChangeListener(this) { sharedPreferences, key ->
                try {
                    if (key in booleanPreferenceTelemetryAllowList) {
                        val enabled = sharedPreferences.getBoolean(key, false)
                        Events.preferenceToggled.record(Events.PreferenceToggledExtra(enabled, key))
                    }
                } catch (e: ClassCastException) {
                    // The setting is not a boolean, not tracked
                }
            }

        profilerViewModel.getProfilerState().observe(
            this,
            Observer<Boolean> {
                updateProfilerUI(it)
            },
        )
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }

    @SuppressLint("RestrictedApi")
    override fun onResume() {
        super.onResume()

        // Use nimbus to set the title, and a trivial addition
        val nimbusValidation = FxNimbus.features.nimbusValidation.value()

        val title = nimbusValidation.settingsTitle
        val suffix = nimbusValidation.settingsPunctuation

        showToolbar("$title$suffix")

        // Account UI state is updated as part of `onCreate`. To not do it twice in a row, we only
        // update it here if we're not going through the `onCreate->onStart->onResume` lifecycle chain.
        update()

        requireView().findViewById<RecyclerView>(R.id.recycler_view)
            ?.hideInitialScrollBar(viewLifecycleOwner.lifecycleScope)

        args.preferenceToScrollTo?.let {
            scrollToPreference(it)
        }

        // Consider finish of `onResume` to be the point at which we consider this fragment as 'created'.
        creatingFragment = false
    }

    override fun onStart() {
        super.onStart()
        // Eyebrowse: this screen no longer follows Mozilla's account, so there is nothing to
        // register for here. Indo Account state is read when the screen is built.
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }

    private fun update() {
        val settings = requireContext().settings()

        val aboutPreference = requirePreference<Preference>(R.string.pref_key_about)
        val appName = getString(R.string.app_name)
        aboutPreference.title = getString(R.string.preferences_about, appName)

        val deleteBrowsingDataPreference =
            requirePreference<Preference>(R.string.pref_key_delete_browsing_data_on_quit_preference)
        deleteBrowsingDataPreference.summary = if (settings.shouldDeleteBrowsingDataOnQuit) {
            getString(R.string.delete_browsing_data_quit_on)
        } else {
            getString(R.string.delete_browsing_data_quit_off)
        }

        val tabSettingsPreference =
            requirePreference<Preference>(R.string.pref_key_tabs)
        tabSettingsPreference.summary = context?.settings()?.getTabTimeoutString()

        val autofillPreference = requirePreference<Preference>(R.string.pref_key_credit_cards)
        autofillPreference.title = if (settings.addressFeature) {
            getString(R.string.preferences_autofill)
        } else {
            getString(R.string.preferences_credit_cards_2)
        }

        val openLinksInAppsSettingsPreference =
            requirePreference<Preference>(R.string.pref_key_open_links_in_apps)
        openLinksInAppsSettingsPreference.summary = context?.settings()?.getOpenLinksInAppsString()

        setupPreferences()

    }

    @SuppressLint("InflateParams")
    @Suppress("ComplexMethod", "LongMethod")
    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        // Hide the scrollbar so the animation looks smoother
        val recyclerView = requireView().findViewById<RecyclerView>(R.id.recycler_view)
        recyclerView.isVerticalScrollBarEnabled = false

        val directions: NavDirections? = when (preference.key) {
            /* Top level account preferences.
            Note: Only ONE of these preferences is visible at a time. */
            resources.getString(R.string.pref_key_sign_in) -> {
                // Eyebrowse: this card is ours. Signed in it opens the account panel at Home, with a
                // fresh short lived panel token, so there is no sign-in page and no blank wait.
                val storage = IndoAccountStorage(requireContext())
                val token = storage.token()
                if (token == null) {
                    IndoAccountEntry.startSignIn(requireContext())
                } else {
                    viewLifecycleOwner.lifecycleScope.launch {
                        val client = IndoAccountClient(requireContext().components.core.client)
                        val panelToken = try {
                            client.panelSession(token = token, device = Build.MODEL.orEmpty())
                        } catch (error: IndoAccountError) {
                            if (error.status == 401) {
                                // The app session is over: clear it, redraw the card, sign in again.
                                storage.signOut()
                                setupIndoAccountPreferences()
                                IndoAccountEntry.startSignIn(requireContext())
                                return@launch
                            }
                            // Offline or a server fault is not a sign-out: open the panel anyway.
                            null
                        } catch (error: Exception) {
                            null
                        }

                        IndoAccountEntry.openManagement(requireContext(), panelToken)
                    }
                }
                return true
            }

            resources.getString(R.string.pref_key_account) -> {
                SettingsFragmentDirections.actionSettingsFragmentToAccountSettingsFragment()
            }

            resources.getString(R.string.pref_key_account_auth_error) -> {
                SettingsFragmentDirections.actionSettingsFragmentToAccountProblemFragment(
                    entrypoint = FenixFxAEntryPoint.SettingsMenu,
                )
            }

            /* General preferences */
            resources.getString(R.string.pref_key_search_settings) -> {
                SettingsFragmentDirections.actionSettingsFragmentToSearchEngineFragment()
            }

            resources.getString(R.string.pref_key_tabs) -> {
                SettingsFragmentDirections.actionSettingsFragmentToTabsSettingsFragment()
            }

            resources.getString(R.string.pref_key_home) -> {
                SettingsFragmentDirections.actionSettingsFragmentToHomeSettingsFragment()
            }

            resources.getString(R.string.pref_key_customize) -> {
                SettingsFragmentDirections.actionSettingsFragmentToCustomizationFragment()
            }

            resources.getString(R.string.pref_key_passwords) -> {
                SettingsMetrics.passwords.record()
                SettingsFragmentDirections.actionSettingsFragmentToSavedLoginsAuthFragment()
            }

            resources.getString(R.string.pref_key_credit_cards) -> {
                SettingsMetrics.autofill.record()
                SettingsFragmentDirections.actionSettingsFragmentToAutofillSettingFragment()
            }

            resources.getString(R.string.pref_key_accessibility) -> {
                SettingsFragmentDirections.actionSettingsFragmentToAccessibilityFragment()
            }

            resources.getString(R.string.pref_key_language) -> {
                SettingsFragmentDirections.actionSettingsFragmentToLocaleSettingsFragment()
            }

            /* Privacy and security preferences */
            resources.getString(R.string.pref_key_private_browsing) -> {
                SettingsFragmentDirections.actionSettingsFragmentToPrivateBrowsingFragment()
            }

            resources.getString(R.string.pref_key_https_only_settings) -> {
                SettingsFragmentDirections.actionSettingsFragmentToHttpsOnlyFragment()
            }

            resources.getString(R.string.pref_key_tracking_protection_settings) -> {
                TrackingProtection.etpSettings.record(NoExtras())
                SettingsFragmentDirections.actionSettingsFragmentToTrackingProtectionFragment()
            }

            resources.getString(R.string.pref_key_site_permissions) -> {
                SettingsFragmentDirections.actionSettingsFragmentToSitePermissionsFragment()
            }

            resources.getString(R.string.pref_key_delete_browsing_data) -> {
                SettingsFragmentDirections.actionSettingsFragmentToDeleteBrowsingDataFragment()
            }

            resources.getString(R.string.pref_key_delete_browsing_data_on_quit_preference) -> {
                SettingsFragmentDirections.actionSettingsFragmentToDeleteBrowsingDataOnQuitFragment()
            }

            resources.getString(R.string.pref_key_notifications) -> {
                context?.navigateToNotificationsSettings {}
                null
            }

            resources.getString(R.string.pref_key_data_choices) -> {
                SettingsFragmentDirections.actionSettingsFragmentToDataChoicesFragment()
            }

            /* Advanced preferences */
            resources.getString(R.string.pref_key_disable_chrome) -> {
                context?.openChromeAppInfo()
                null
            }

            resources.getString(R.string.pref_key_open_links_in_apps) -> {
                SettingsFragmentDirections.actionSettingsFragmentToOpenLinksInAppsFragment()
            }

            resources.getString(R.string.pref_key_sync_debug) -> {
                SettingsFragmentDirections.actionSettingsFragmentToSyncDebugFragment()
            }

            /* About preferences */
            resources.getString(R.string.pref_key_rate) -> {
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(SupportUtils.RATE_APP_URL)))
                } catch (e: ActivityNotFoundException) {
                    // Device without the play store installed.
                    // Opening the play store website.
                    (activity as HomeActivity).openToBrowserAndLoad(
                        searchTermOrURL = SupportUtils.FENIX_PLAY_STORE_URL,
                        newTab = true,
                        from = BrowserDirection.FromSettings,
                    )
                }
                null
            }

            resources.getString(R.string.pref_key_about) -> {
                SettingsFragmentDirections.actionSettingsFragmentToAboutFragment()
            }

            // Only displayed when secret settings are enabled
            resources.getString(R.string.pref_key_debug_settings) -> {
                SettingsFragmentDirections.actionSettingsFragmentToSecretSettingsFragment()
            }

            // Only displayed when secret settings are enabled
            resources.getString(R.string.pref_key_secret_debug_info) -> {
                SettingsFragmentDirections.actionSettingsFragmentToSecretInfoSettingsFragment()
            }

            // Only displayed when secret settings are enabled
            resources.getString(R.string.pref_key_nimbus_experiments) -> {
                SettingsFragmentDirections.actionSettingsFragmentToNimbusExperimentsFragment()
            }

            // Only displayed when secret settings are enabled
            resources.getString(R.string.pref_key_start_profiler) -> {
                if (profilerViewModel.getProfilerState().value == true) {
                    SettingsFragmentDirections.actionSettingsFragmentToStopProfilerDialog()
                } else {
                    SettingsFragmentDirections.actionSettingsFragmentToStartProfilerDialog()
                }
            }

            else -> null
        }
        directions?.let { navigateFromSettings(directions) }
        return super.onPreferenceTreeClick(preference)
    }

    /**
     * EyeBlock, our own ad blocker. The switch only stores the choice; the blocking itself is
     * applied when a page is created or reloaded, so a page already open keeps its policy until it
     * is loaded again.
     */
    private fun setupEyeblockPreference() {
        val preference = findPreference<SwitchPreference>(getPreferenceKey(R.string.pref_key_eyeblock))
            ?: return
        val settings = context?.settings() ?: return

        preference.isChecked = settings.eyeblockEnabled
        preference.setOnPreferenceChangeListener { _, newValue ->
            val enabled = newValue == true
            settings.eyeblockEnabled = enabled
            context?.components?.core?.engine?.let { engine ->
                EyeblockSupport.setEnabled(engine, enabled)
            }
            true
        }
    }

    /**
     * Indo Account: one row. Signed out it starts the sign-in; signed in it shows the live Indo ID and
     * display name and opens the web panel, where every setting actually lives. The profile is read
     * from the account on each open and is never copied into local storage.
     */
    private fun setupIndoAccountPreferences() {
        val context = context ?: return
        val storage = IndoAccountStorage(context)
        val accountPreference =
            findPreference<Preference>(getPreferenceKey(R.string.pref_key_sign_in)) ?: return
        val signOutPreference =
            findPreference<Preference>(getPreferenceKey(R.string.pref_key_indo_account_sign_out)) ?: return

        signOutPreference.isVisible = storage.isSignedIn()

        val token = storage.token()

        if (token == null) {
            // Signed out: the card is the way in, and it carries no summary.
            accountPreference.title = getString(R.string.preferences_indo_account)
            accountPreference.summary = null
        } else {
            // Signed in: the card is drawn from what we already hold, on the first frame, so it
            // never flickers through the signed-out wording while the network answers.
            accountPreference.title = getString(R.string.preferences_indo_account_manage)
            val storedIdentity = storage.identity()
            accountPreference.summary = storedIdentity?.let { identity -> identitySummary(identity) }

            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val fresh = IndoAccountClient(requireContext().components.core.client)
                        .identity(token)
                    if (fresh != storedIdentity) {
                        storage.saveIdentity(fresh)
                        accountPreference.summary = identitySummary(fresh)
                    }
                } catch (error: IndoAccountError) {
                    if (error.status == 401) {
                        // The token is no longer good: drop it and go back to the entry point.
                        storage.signOut()
                        signOutPreference.isVisible = false
                        accountPreference.title = getString(R.string.preferences_indo_account)
                        accountPreference.summary = null
                    }
                    // Any other failure keeps what we hold: no clearing, no signed-out wording.
                } catch (error: Exception) {
                    // Offline is not a sign-out.
                }
            }
        }

        signOutPreference.setOnPreferenceClickListener {
            storage.signOut()
            signOutPreference.isVisible = false
            accountPreference.title = getString(R.string.preferences_indo_account)
            accountPreference.summary = null
            Toast.makeText(requireContext(), R.string.indo_account_signed_out_toast, Toast.LENGTH_SHORT)
                .show()
            true
        }
    }

    /**
     * VPN, step one: the switch, the consent Android insists on, and the service. The tunnel has no
     * exit yet, and the summary says so rather than pretending.
     */
    private fun setupVpnPreference() {
        val preference = findPreference<SwitchPreference>(getPreferenceKey(R.string.pref_key_vpn))
            ?: return
        val context = context ?: return

        preference.summary = getString(R.string.preferences_vpn_off)
        preference.setOnPreferenceChangeListener { _, newValue ->
            if (newValue == true) {
                val consent = VpnController.consentIntent(context)
                if (consent != null) {
                    vpnConsentLauncher.launch(consent)
                    // The switch flips when the consent comes back, not now.
                    return@setOnPreferenceChangeListener false
                }
                startVpn(context)
            } else {
                VpnController.stop(context)
                preference.summary = getString(R.string.preferences_vpn_off)
            }
            true
        }
    }

    private fun startVpn(context: Context) {
        context.getSystemService(NotificationManager::class.java)?.let { manager ->
            EyebrowseVpnService.ensureChannel(manager)
        }
        VpnController.start(context)
        findPreference<SwitchPreference>(getPreferenceKey(R.string.pref_key_vpn))?.let {
            it.isChecked = true
            it.summary = getString(R.string.preferences_vpn_preparing)
        }
    }

    /**
     * One switch, one type: bookmarks. Turning it on tells the server this account wants that type,
     * then runs a sync immediately so the user sees a result instead of a promise.
     */
    private fun setupSyncPreference() {
        val preference = findPreference<SwitchPreference>(getPreferenceKey(R.string.pref_key_eyebrowse_sync_bookmarks))
            ?: return
        val context = context ?: return
        val account = IndoAccountStorage(context)

        if (!account.isSignedIn()) {
            preference.isChecked = false
            preference.isEnabled = false
            preference.summary = getString(R.string.preferences_eyebrowse_sync_bookmarks_signed_out)
            return
        }

        preference.summary = getString(R.string.preferences_eyebrowse_sync_bookmarks_off)
        preference.setOnPreferenceChangeListener { _, newValue ->
            val enabled = newValue == true
            preference.summary = getString(
                if (enabled) R.string.preferences_eyebrowse_sync_bookmarks_on else R.string.preferences_eyebrowse_sync_bookmarks_off,
            )
            viewLifecycleOwner.lifecycleScope.launch {
                val client = SyncFactory.client(context)
                when (client.setEnabled(SyncType.BOOKMARKS, enabled)) {
                    is SyncClient.Outcome.Ok -> if (enabled) runSyncOnce(context)
                    else -> preference.summary = getString(R.string.preferences_eyebrowse_sync_bookmarks_off)
                }
            }
            true
        }
    }

    /**
     * EyeShield had no switch, by my design, and that hid a rendering bug from us for two rounds.
     * A thing that blocks page requests must be something the user can turn off.
     */
    private fun setupShieldPreference() {
        val preference = findPreference<SwitchPreference>(getPreferenceKey(R.string.pref_key_eyebrowse_shield))
            ?: return
        val engine = context?.components?.core?.engine ?: return

        preference.isChecked = EyeblockSupport.isShieldEnabled(preference.context)
        preference.summary = getString(
            if (preference.isChecked) R.string.preferences_eyebrowse_shield_on
            else R.string.preferences_eyebrowse_shield_off,
        )

        preference.setOnPreferenceChangeListener { _, newValue ->
            val enabled = newValue == true
            EyeblockSupport.setShieldEnabled(preference.context, enabled)
            EyeblockSupport.setShieldRunning(engine, enabled)
            preference.summary = getString(
                if (enabled) R.string.preferences_eyebrowse_shield_on
                else R.string.preferences_eyebrowse_shield_off,
            )
            true
        }
    }

    /** The same shape as bookmarks, for saved passwords. */
    private fun setupSyncLoginsPreference() {
        val preference = findPreference<SwitchPreference>(getPreferenceKey(R.string.pref_key_eyebrowse_sync_logins))
            ?: return
        val context = context ?: return
        val account = IndoAccountStorage(context)

        if (!account.isSignedIn()) {
            preference.isChecked = false
            preference.isEnabled = false
            preference.summary = getString(R.string.preferences_eyebrowse_sync_bookmarks_signed_out)
            return
        }

        preference.summary = getString(R.string.preferences_eyebrowse_sync_logins_off)
        preference.setOnPreferenceChangeListener { _, newValue ->
            val enabled = newValue == true
            preference.summary = getString(
                if (enabled) R.string.preferences_eyebrowse_sync_logins_on
                else R.string.preferences_eyebrowse_sync_logins_off,
            )
            viewLifecycleOwner.lifecycleScope.launch {
                val client = SyncFactory.client(context)
                when (client.setEnabled(SyncType.LOGINS, enabled)) {
                    is SyncClient.Outcome.Ok -> if (enabled) {
                        val engine = SyncFactory.engine(
                            context,
                            context.components.core.bookmarksStorage,
                            context.components.core.passwordsStorage,
                        )
                        val report = engine.runLogins()
                        Log.i(
                            "EyebrowseSync",
                            "logins: recorded ${report.recorded}, uploaded ${report.uploaded}, " +
                                "downloaded ${report.downloaded}, applied ${report.applied}, " +
                                "stopped by ${report.stoppedBy ?: "nothing"}",
                        )
                    }
                    else -> preference.summary = getString(R.string.preferences_eyebrowse_sync_logins_off)
                }
            }
            true
        }
    }

    /** One run, in the background, so the switch does not freeze while the network answers. */
    private suspend fun runSyncOnce(context: Context) {
        val bookmarksStorage = context.components.core.bookmarksStorage
        val report = SyncFactory.engine(context, bookmarksStorage).runBookmarks()
        Log.i(
            "EyebrowseSync",
            "bookmarks: recorded ${report.recorded}, removed ${report.tombstones}, " +
                "uploaded ${report.uploaded}, downloaded ${report.downloaded}, applied ${report.applied}, " +
                "stopped by ${report.stoppedBy ?: "nothing"}",
        )
    }

    /** Android's consent dialog, which is the only thing that can grant a tunnel. */
    private val vpnConsentLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val context = context ?: return@registerForActivityResult
        if (result.resultCode == Activity.RESULT_OK) {
            startVpn(context)
        } else {
            findPreference<SwitchPreference>(getPreferenceKey(R.string.pref_key_vpn))?.let {
                it.isChecked = false
                it.summary = getString(R.string.preferences_vpn_off)
            }
        }
    }

    /** The name and the Indo ID, or the ID alone when there is no name. */
    private fun identitySummary(identity: IndoIdentity): String = if (identity.fullName.isBlank()) {
        identity.indoId
    } else {
        getString(R.string.indo_account_profile_line, identity.fullName, identity.indoId)
    }

    private fun setupPreferences() {
        val leakKey = getPreferenceKey(R.string.pref_key_leakcanary)
        val debuggingKey = getPreferenceKey(R.string.pref_key_remote_debugging)
        val preferenceLeakCanary = findPreference<Preference>(leakKey)
        val preferenceRemoteDebugging = findPreference<Preference>(debuggingKey)
        val preferenceMakeDefaultBrowser =
            requirePreference<DefaultBrowserPreference>(R.string.pref_key_make_default_browser)

        if (!Config.channel.isReleased) {
            preferenceLeakCanary?.setOnPreferenceChangeListener { _, newValue ->
                val isEnabled = newValue == true
                context?.application?.updateLeakCanaryState(isEnabled)
                true
            }
        }

        preferenceRemoteDebugging?.isVisible = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
        preferenceRemoteDebugging?.setOnPreferenceChangeListener<Boolean> { preference, newValue ->
            preference.context.settings().preferences.edit()
                .putBoolean(preference.key, newValue).apply()
            requireComponents.core.engine.settings.remoteDebuggingEnabled = newValue
            true
        }

        preferenceMakeDefaultBrowser.apply {
            updateSwitch()
            onPreferenceClickListener =
                getClickListenerForMakeDefaultBrowser()
        }

        val preferenceStartProfiler =
            findPreference<Preference>(getPreferenceKey(R.string.pref_key_start_profiler))

        with(requireContext().settings()) {
            findPreference<Preference>(
                getPreferenceKey(R.string.pref_key_nimbus_experiments),
            )?.isVisible = showSecretDebugMenuThisSession
            findPreference<Preference>(
                getPreferenceKey(R.string.pref_key_debug_settings),
            )?.isVisible = showSecretDebugMenuThisSession
            findPreference<Preference>(
                getPreferenceKey(R.string.pref_key_secret_debug_info),
            )?.isVisible = showSecretDebugMenuThisSession
            findPreference<Preference>(
                getPreferenceKey(R.string.pref_key_sync_debug),
            )?.isVisible = showSecretDebugMenuThisSession
            preferenceStartProfiler?.isVisible = showSecretDebugMenuThisSession &&
                (requireContext().components.core.engine.profiler?.isProfilerActive() != null)
        }
        setupEyeblockPreference()
        setupIndoAccountPreferences()
        setupVpnPreference()
        setupSyncPreference()
        setupSyncLoginsPreference()
        setupShieldPreference()
        setupCookieBannerPreference()
        setupGeckoLogsPreference(requireContext().settings())
        setupAllowDomesticChinaFxaServerPreference()
        setupHttpsOnlyPreferences()
        setupNotificationPreference()
        setupSearchPreference()
        setupHomepagePreference()
        setupTrackingProtectionPreference()
    }

    /**
     * For >=Q -> Use new RoleManager API to show in-app browser switching dialog.
     * For <Q && >=N -> Navigate user to Android Default Apps Settings.
     * For <N -> Open sumo page to show user how to change default app.
     */
    private fun getClickListenerForMakeDefaultBrowser(): Preference.OnPreferenceClickListener {
        return Preference.OnPreferenceClickListener {
            activity?.openSetDefaultBrowserOption()
            true
        }
    }

    private fun navigateFromSettings(directions: NavDirections) {
        view?.findNavController()?.let { navController ->
            if (navController.currentDestination?.id == R.id.settingsFragment) {
                navController.navigate(directions)
            }
        }
    }

    // Extension function for hiding the scroll bar on initial loading. We must do this so the
    // animation to the next screen doesn't animate the initial scroll bar (it ignores
    // isVerticalScrollBarEnabled being set to false).
    private fun RecyclerView.hideInitialScrollBar(scope: CoroutineScope) {
        scope.launch {
            val originalSize = scrollBarSize
            scrollBarSize = 0
            delay(SCROLL_INDICATOR_DELAY)
            scrollBarSize = originalSize
        }
    }

    private fun updateFxAAllowDomesticChinaServerMenu() {
        val settings = requireContext().settings()
        val preferenceAllowDomesticChinaServer =
            findPreference<SwitchPreference>(getPreferenceKey(R.string.pref_key_allow_domestic_china_fxa_server))
        // Only enable changes to these prefs when the user isn't connected to an account.
        val enabled =
            requireComponents.backgroundServices.accountManager.authenticatedAccount() == null
        val checked = settings.allowDomesticChinaFxaServer
        val visible = Config.channel.isMozillaOnline
        preferenceAllowDomesticChinaServer?.apply {
            isEnabled = enabled
            isChecked = checked
            isVisible = visible
        }
    }


    @VisibleForTesting
    internal fun setupGeckoLogsPreference(settings: Settings) {
        val preferenceEnabledGeckoLogs =
            findPreference<Preference>(getPreferenceKey(R.string.pref_key_enable_gecko_logs))

        val show = settings.showSecretDebugMenuThisSession
        preferenceEnabledGeckoLogs?.isVisible = show

        preferenceEnabledGeckoLogs?.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, newValue ->
                context?.settings()?.enableGeckoLogs = newValue as Boolean
                Toast.makeText(
                    context,
                    getString(R.string.quit_application),
                    Toast.LENGTH_LONG,
                ).show()
                Handler(Looper.getMainLooper()).postDelayed(
                    {
                        exitProcess(0)
                    },
                    FXA_SYNC_OVERRIDE_EXIT_DELAY,
                )
                true
            }
    }

    private fun setupAllowDomesticChinaFxaServerPreference() {
        val allowDomesticChinaFxAServer = getPreferenceKey(R.string.pref_key_allow_domestic_china_fxa_server)
        val preferenceAllowDomesticChinaFxAServer = findPreference<SwitchPreference>(allowDomesticChinaFxAServer)
        val visible = Config.channel.isMozillaOnline

        preferenceAllowDomesticChinaFxAServer?.apply {
            isVisible = visible
        }

        if (visible) {
            preferenceAllowDomesticChinaFxAServer?.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { preference, newValue ->
                    preference.context.settings().preferences.edit()
                        .putBoolean(preference.key, newValue as Boolean).apply()
                    updateFxAAllowDomesticChinaServerMenu()
                    Toast.makeText(
                        context,
                        getString(R.string.toast_override_account_sync_server_done),
                        Toast.LENGTH_LONG,
                    ).show()
                    Handler(Looper.getMainLooper()).postDelayed(
                        {
                            exitProcess(0)
                        },
                        FXA_SYNC_OVERRIDE_EXIT_DELAY,
                    )
                }
        }
    }

    @VisibleForTesting
    internal fun setupNotificationPreference() {
        with(requirePreference<Preference>(R.string.pref_key_notifications)) {
            summary = if (NotificationManagerCompat.from(requireContext()).areNotificationsEnabled()) {
                getString(R.string.notifications_allowed_summary)
            } else {
                getString(R.string.notifications_not_allowed_summary)
            }
        }
    }

    @VisibleForTesting
    internal fun setupHomepagePreference() {
        with(requirePreference<Preference>(R.string.pref_key_home)) {
            summary = when {
                context.settings().alwaysOpenTheHomepageWhenOpeningTheApp ->
                    getString(R.string.opening_screen_homepage_summary)

                context.settings().openHomepageAfterFourHoursOfInactivity ->
                    getString(R.string.opening_screen_after_four_hours_of_inactivity_summary)

                context.settings().alwaysOpenTheLastTabWhenOpeningTheApp ->
                    getString(R.string.opening_screen_last_tab_summary)

                else -> null
            }
        }
    }

    @VisibleForTesting
    internal fun setupSearchPreference() {
        with(requirePreference<Preference>(R.string.pref_key_search_settings)) {
            summary =
                requireContext().components.core.store.state.search.selectedOrDefaultSearchEngine?.name
        }
    }

    @VisibleForTesting
    internal fun setupTrackingProtectionPreference() {
        with(requirePreference<Preference>(R.string.pref_key_tracking_protection_settings)) {
            summary = when {
                !context.settings().shouldUseTrackingProtection -> getString(R.string.tracking_protection_off)
                context.settings().useStandardTrackingProtection -> getString(R.string.tracking_protection_standard)
                context.settings().useStrictTrackingProtection -> getString(R.string.tracking_protection_strict)
                context.settings().useCustomTrackingProtection -> getString(R.string.tracking_protection_custom)
                else -> null
            }
        }
    }

    @VisibleForTesting
    internal fun setupCookieBannerPreference() {
        FxNimbus.features.cookieBanners.recordExposure()
        if (context?.settings()?.shouldShowCookieBannerUI == false) return
        with(requirePreference<SwitchPreference>(R.string.pref_key_cookie_banner_private_mode)) {
            isVisible = context.settings().shouldShowCookieBannerUI

            onPreferenceChangeListener = object : SharedPreferenceUpdater() {
                override fun onPreferenceChange(
                    preference: Preference,
                    newValue: Any?,
                ): Boolean {
                    val metricTag = if (newValue == true) {
                        "reject_all"
                    } else {
                        "disabled"
                    }
                    val engineSettings = requireContext().components.core.engine.settings
                    val settings = requireContext().settings()
                    settings.shouldUseCookieBannerPrivateMode = newValue as Boolean
                    val mode = settings.getCookieBannerHandlingPrivateMode()
                    engineSettings.cookieBannerHandlingModePrivateBrowsing = mode
                    CookieBanners.settingChangedPmb.record(CookieBanners.SettingChangedPmbExtra(metricTag))
                    requireContext().components.useCases.sessionUseCases.reload()
                    return super.onPreferenceChange(preference, newValue)
                }
            }
        }
    }


    @VisibleForTesting
    internal fun setupHttpsOnlyPreferences() {
        val httpsOnlyPreference =
            requirePreference<Preference>(R.string.pref_key_https_only_settings)
        httpsOnlyPreference.summary = context?.let {
            when {
                !it.settings().shouldUseHttpsOnly -> getString(R.string.preferences_https_only_off)
                it.settings().shouldUseHttpsOnlyInAllTabs -> getString(R.string.preferences_https_only_on_all)
                it.settings().shouldUseHttpsOnlyInPrivateTabsOnly ->
                    getString(R.string.preferences_https_only_on_private)
                else -> null
            }
        }
    }

    private fun updateProfilerUI(profilerStatus: Boolean) {
        if (profilerStatus) {
            findPreference<Preference>(getPreferenceKey(R.string.pref_key_start_profiler))?.title =
                resources.getString(R.string.profiler_stop)
            findPreference<Preference>(getPreferenceKey(R.string.pref_key_start_profiler))?.summary =
                resources.getString(R.string.profiler_running)
        } else {
            findPreference<Preference>(getPreferenceKey(R.string.pref_key_start_profiler))?.title =
                resources.getString(R.string.preferences_start_profiler)
            findPreference<Preference>(getPreferenceKey(R.string.pref_key_start_profiler))?.summary = ""
        }
    }

    companion object {
        private const val SCROLL_INDICATOR_DELAY = 10L
        private const val FXA_SYNC_OVERRIDE_EXIT_DELAY = 2000L
        private const val AMO_COLLECTION_OVERRIDE_EXIT_DELAY = 3000L
    }
}
