# EyeBlock regression checks

EyeBlock bundles uBlock Origin as an unpacked built-in GeckoView extension. Do not
report the preference as proof of filtering: the Settings summary reports the
extension's actual enable/private-permission state, not filter readiness.

## Release gate (manual device checks until automated instrumentation is available)

- Fresh install: launch a normal tab and check the EyeBlock status in Settings.
- Upgrade from the old XPI-based APK without clearing the profile: check the
  extension is updated, enabled, and normal browsing still works.
- Open a local controlled test page containing a known ad-filter match and a
  benign same-origin resource; verify the matched request is blocked, the benign
  request succeeds and the ad element is hidden.
- Repeat in private tabs, after force-stop and restart, with multiple tabs, and
  immediately after launching an external HTTPS link in Eyebrowse.
- Toggle EyeBlock off/on rapidly; reload the test page and verify the final state.
- Check startup offline, then reconnect and verify the upstream filter updates.
- Test ordinary YouTube videos, Shorts, seeking, fullscreen and playback after
  toggling EyeBlock. Confirm videos actually play, not just that ads disappear.
- Confirm legitimate sites do not break. Test Standard and Strict tracking
  protection independently; do not turn engine protection off just because
  EyeBlock is requested.
- Inspect adb logcat with tag EyeblockSupport for install/enable errors.
- Do not claim all YouTube advertisements can be blocked: dynamic delivery,
  upstream filter freshness and GeckoView compatibility must be tested.

The pinned engine is GeckoView 125 (March 2024). Upgrade it with matching Android
Components as a separate security and compatibility task. The extension is
pinned to a verified upstream release digest; do not bypass manifest compatibility
or downgrade protections to make a newer extension install.

## Automated source-level regression coverage

- EyeBlock install failure followed by successful retry.
- Latest switch choice wins during asynchronous installation.
- Private permission failure is not reported as fully enabled.
- Build preparation rejects upstream minimum Gecko major versions newer than the pinned engine.

Run the targeted tests before a release APK is approved. They are not a substitute
for the controlled network and playback checks above. The first navigation may
still race filter initialization; verify cold-start external links on a device.

## Optional device-wide DNS setup (Android 9+)

The new Settings row opens Android network settings or copies the public AdGuard
DNS hostname. It does not enable DNS silently, claim an active filter, replace the
existing browser VPN, or inspect other apps' traffic. DNS filtering cannot block
same-domain ads, including many ads in the official YouTube app. Test device
settings navigation, copy, cancellation, DNS activation/reversal, ordinary apps,
VPN coexistence and network failures on a real device. No device test completed.
Provider documentation: https://adguard-dns.io/en/public-dns.html

## YouTube website investigation

Settings > EyeBlock filter settings opens the installed extension's own options
URL, obtained from Gecko metadata rather than guessing an extension UUID.
The user can keep default lists enabled, update filters (including Quick fixes),
and check whether youtube.com or m.youtube.com is trusted. Close existing
YouTube tabs and retest in a fresh tab. Test the dashboard navigation and
filter-update result, normal/private playback, pre-roll/mid-roll and Shorts.
This change exposes maintenance controls; it does not establish that any
particular YouTube ad is blocked. GeckoView 125 remains a compatibility and
security upgrade dependency. Do not inject ad-skipping scripts, replace the
player, or disable engine security as a substitute for verified filtering.

## Failure recovery audit

The standard Android Components WebExtensionSupport bridge is now initialized
before built-in extension installation, to register per-session tab and action
handlers and receive lifecycle events. Additional extension update permissions
are denied rather than silently granted. EyeBlock retries known install or
private-permission errors on foreground return, honors a changed switch after
an install error, and avoids granting private access while switched off. The
settings XML default matches the app preference (enabled on fresh installs).
New unit tests exercise retry and changed switch behavior. Test fresh startup,
restored tabs, dashboard navigation, extension process failure and YouTube
playback on a physical device. An enabled extension is still not proof of
ready filter lists or actual filtering; a cold-start race remains possible.

## Offline filter integrity and media recovery

The build now checks that each default upstream filter registry entry has a
nonempty packaged local fallback, including EasyList, EasyPrivacy and Quick
fixes, and that the actual options page is present. This catches incomplete
bundles before APK assembly. The previously supplied debug APK was inspected
and contains these assets; missing files were not established as its failure.

Media service foreground state is set only after Android accepts startForeground.
If Android rejects the transition, the next playing-state update can attempt
promotion again; no forced playback or website visibility spoofing is added.
The existing component regression test asserts that rejection leaves the flag
false. Eyebrowse's app build disables Android Components unit test tasks, so
run that component test separately before declaring it verified. Screen-lock,
background playback, PiP and actual YouTube filtering still require device tests.

## Restored-tab startup and permission race

Restoring saved tabs now waits up to three seconds for EyeBlock extension
reconciliation if enabled. Failure or timeout allows normal restore; this is
not a filter-ready signal and does not gate direct external navigation. A new
test covers enabling EyeBlock while a disable callback is pending, ensuring
private access is reconciled before the final enabled state is reported. On a
device verify that restore does not lose external links or hang when Gecko
fails, and that actual filter initialization and playback still work.

## Menu and private dashboard accuracy

The browser menu labels its count as Gecko tracking-protection log entries with
blocks, not uBlock requests. Loaded-only entries are excluded; errors show
unavailable. The site exception affects Gecko tracking protection only. The
filter dashboard opens in the current browsing mode if permitted, rather than
silently creating a normal tab from private mode. Device testing is required.

## On-demand EyeBlock (supersedes the old bundled/default-on design above)

The switch and filter dashboard now appear immediately after VPN settings. A new preference
key defaults to OFF, including migration from the prior default-on version. The APK no
longer packages uBlock Origin; the build still validates the pinned signed release archive
and its filter contents. On explicit opt-in, Gecko downloads the pinned HTTPS XPI and
validates its signature before install. The managed permission callback approves only the
pending uBlock ID and pinned version. The switch stays off during download, install and
enable, and displays actual phase text (no fabricated progress percentages). The extension
can be reused on later toggles. Old built-in copies are disabled on upgrade and replaced
on subsequent opt-in; replacement failure is visible. A failed download can be retried
explicitly. Test offline failure, real download, signature and permission callback, upgrade
from a built-in copy, app restart, filter dashboard, and actual normal/private ad filtering.
Engine enablement is not evidence that YouTube ads are blocked or lists have finished updating.


## Engine migration — verification only (2026-11-28)

The previous GeckoView 125 engine predates the Mozilla add-on signing root certificate renewal. Mozilla documents expiry on 2025-03-14 (https://support.mozilla.org/en-US/kb/root-certificate-expiration). This is a candidate explanation, not a confirmed device diagnosis. Upgrade target: stable `geckoview-omni:161.0.20261118180411` (published 2026-11-24; https://mvnrepository.com/artifact/org.mozilla.geckoview/geckoview-omni/161.0.20261118180411). Do not disable extension signature checks. This is a major engine migration, not a drop-in compatibility guarantee. Resolve all dependency, Android SDK/AGP/Kotlin and engine adapter incompatibilities against the actual compiler output; review transitive dependencies/security advisories. Do not suppress tests or relax signing. Preserve application ID, opt-in, pinned XPI and error details. The `fix/eyeblock-signed-install` branch runs compile and app unit tests only; debug and release APK assembly are explicitly disabled even for dispatch until separately approved. No merging or APK release before green verification. A separate later approved device build must test actual signed XPI installation, offline error/retry, fresh install and upgrade, disabled state, normal/private filtering, VPN and playback. CI alone does not establish device end-to-end success.
