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
