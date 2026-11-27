# Eyebrowse third-party VPN

Eyebrowse will not operate or host VPN servers. The client uses the
WireGuard tunnel library (Apache-2.0), currently pinned to 1.0.20230706 for
SDK 34 compatibility. Provider service and encryption are separate concerns.

## Authorized free providers
- Proton VPN: https://protonvpn.com/support/wireguard-configurations
  A free account can generate a WireGuard profile for a third-party client.
- VPNBook: https://www.vpnbook.com/freevpn/wireguard-vpn
  Free no-account profile generation, subject to provider verification and
  terms. Profiles expire in seven days and are per-device.

Both links open official provider pages. No shared credentials, scraped keys,
undocumented provisioning API, CAPTCHA bypass, paid subscription or company
VPN hosting is used. VPN Gate's public directory permits third-party clients
but uses OpenVPN rather than WireGuard and records connection metadata.
An opt-in read-only directory preview is implemented; OpenVPN connection is not.

## Usage and actual behavior
1. Settings > Free VPN providers and profiles > choose an official provider.
2. Generate and download a profile under the provider's terms. Import it in
   Eyebrowse. Add up to five valid profiles from authorized servers.
3. Tap VPN. Android may request consent. Subsequent taps reconnect using saved
   profiles; the most recently successful profile in this process is tried first.
4. If a profile never handshakes, try the next saved profile after 40 seconds.
   A later stale handshake also starts recovery. Retry at most two rounds of
   saved profiles per outage, with five-second spacing. A successful handshake
   resets the retry budget. Consent revocation/service removal stops recovery.
5. Use the profile management row to delete saved credentials.

Only Eyebrowse traffic is routed. Require a single peer with a configured
endpoint, DNS and full IPv4 routing. IPv6 must be fully routed or entirely
unused; the pinned GoBackend leaves unused IP families blocked while a
single-peer default-route tunnel is active. Verify IPv6 leak tests on device. Keys are AES-GCM encrypted using
Android Keystore on API 23+, with no plaintext persistence on API 21/22.
The provider can observe metadata and unencrypted traffic. A handshake is
not proof of internet reachability. Traffic may use the normal network during
failover, disconnection or service failure. No kill switch or always-on mode.

## Release gate
Build and compile, verify native ABI and merged manifest, test encrypted
storage/restart/clear, invalid and expired profiles, permission denial/revoke,
first-server failure and backup handshake, stop during connection, Wi-Fi/mobile
transitions, DNS/IPv4/IPv6 leakage and browser-only routing. Test provider terms
and quota handling. Do not claim an anonymous zero-setup or no-logs VPN.
An official permitted automatic provisioning API would be needed to remove
first-time enrollment. No such integration has been verified.

## Remaining provider and safety constraints
Zero-setup WireGuard provisioning is not available from the verified free
providers: Proton requires an account and profile, VPNBook requires a generated
profile. VPN Gate publishes a no-account directory but uses OpenVPN and records
connection metadata; do not silently switch the product to that provider.
Android system lockdown changes device-wide connectivity and is not a browser-only
kill switch. Do not advertise leak prevention during tunnel replacement. A failed
stop leaves a distinct STOP_FAILED state with a retry action. Run VpnPolicyTest
and all device checks before release.

## Navigation guard — first safety stage
The optional VPN navigation guard persists across process restarts and blocks
new HTTP(S) Gecko navigation callbacks unless both a recent WireGuard handshake
and Android's active VPN transport are observed. It is off by default, and must
be turned off temporarily for provider enrollment when no tunnel exists. API
21/22 cannot reliably verify the active network with this implementation, so
the enabled guard blocks web navigation there. It does not protect subresources
that bypass navigation callbacks, existing media, native clients, WebRTC or all
packets. It is not a full kill switch. Test both the app-initiated and page-initiated
navigation paths, redirects, error pages, restarts and false positives on a device.

## Zero-setup provider evaluation
Proton's official instructions require a user account and a downloaded profile:
https://protonvpn.com/support/wireguard-configurations
VPNBook requires generation of a unique per-device configuration with seven-day expiry:
https://www.vpnbook.com/freevpn/wireguard-vpn
VPN Gate volunteer relays are a possible no-account option, but use OpenVPN and
have a published connection logging policy: https://www.vpngate.net/en/about_abuse.aspx
An OpenVPN backend would require an audited Android-compatible library, native ABI
and license review, explicit provider consent/privacy disclosure, safe profile parsing
and real connection testing. Do not expose an automatic server selector until a
working protocol backend exists. No company-hosted servers will be introduced.
Android always-on lockdown can impact excluded apps and cannot be silently enabled
as a browser-only kill switch: https://developer.android.com/develop/connectivity/vpn

## VPN Gate discovery implementation
Official source: https://www.vpngate.net/api/iphone/ (VPN Gate explicitly permits
CSV clients: https://www.vpngate.net/en/). Fetch only after a user opens the
provider and accepts its logging disclosure. Use HTTPS, no redirects, bounded
response size/timeouts, exact schema checks, metadata sanitization and a capped
ranked list. Do not download separate files, apply remote configuration, persist
server records, or change connection status. The listed score and ping are
provider-reported. The directory is not a server selection/connect control.

OpenVPN for Android's embedded code is GPLv2 with additional derivative-work
clarifications: https://github.com/schwabe/ics-openvpn/blob/master/doc/LICENSE.txt
Its external AIDL sample is Apache-2.0, but requires a separate app and does not
by itself guarantee Eyebrowse-only routing. OpenVPN 3 requires a reviewed native
Android integration: https://github.com/OpenVPN/openvpn3. No native backend or
companion app is bundled. Before enabling connection, resolve distribution
licensing, pin supported versions, test ABI and Android foreground service,
validate relay config and certificate verification, DNS and routing, explicit
provider consent, and actual connection/failover. Keep WireGuard independent.

## OpenVPN profile and backend investigation
Added a standalone conservative OpenVPN profile grammar gate and regression
fixtures. It rejects scripts, plugins, external file references, duplicate
options, mismatched endpoints, unsupported options, compression and unsafe
crypto settings. It requires an inline CA, explicit server certificate role
verification and TLS 1.2+. This validates syntax/policy only, not certificate
chains, server identity or reachability. Do not use it to silently rewrite
VPN Gate profiles. It is not connected to the directory or native engine yet;
legacy VPN Gate profiles that do not meet the policy must be rejected.

Candidate evaluation:
- Official OpenVPN 3 C++ client: https://github.com/OpenVPN/openvpn3 . Native
  Android JNI, TUN/socket protection, certificate verification and per-app
  routing implementation must be reviewed before enabling it.
- Maven Central io.github.tim06:vpnprotocols-openvpn:3.0.4 exists, but source
  repo was unavailable during review and the API, native dependencies, and
  per-app isolation were not verified. It depends on Kotlin 2.2.21; do not
  blindly add it to the older Fenix toolchain.
- OpenVPN for Android external AIDL supports inline profile start, but reviewed
  startVPNwithExtras only exposes allow-vpn-bypass, not a per-app allowlist.
  Do not start device-wide VPN from this browser without explicit scope change.
No embedded engine or dependency was added. Actual tunnel, automatic first-time
connection, full packet kill switch and release tests remain blocked on an
approved, verifiable backend and native build/device testing.

## Experimental native adapter (local debug only)
An opt-in direct ics-openvpn backend is now implemented against the pinned
source-built Mysterium AAR, source revision 435fd97a51a9b1fb71148c0a697da04f32fc10f1:
https://github.com/mysteriumnetwork/openvpn_dart/tree/435fd97a51a9b1fb71148c0a697da04f32fc10f1/android/localmaven
Use the upstream provenance, patch and corresponding source to review the AAR;
expected SHA-256 3540c8579b0056baac517cb6c930d9d9fb19cbbdf6216e4353fce5513aee50f6.
Local developers may opt in with -PeyebrowseOpenVpnAar=/absolute/path/to/verified.aar
and Android SDK 35. The binary is not committed, downloaded, or bundled in default
builds. Dependency and adapter are debug-only. This is not a distribution license
approval; resolve all GPL/third-party obligations before shipping any binary.

The experimental flow validates directory profiles, obtains explicit consent,
selects the highest-ranked compatible candidate, launches the native service,
sets a package-only allowlist, and reads UUID-matched engine callbacks. It keeps
TUN across reconnects where supported, blocks unused families and disables app
bypass; no OS-level kill switch is claimed. WireGuard and experimental OpenVPN
are mutually exclusive by controller state. Strict policy may reject every live
VPN Gate profile; do not weaken it just to connect. Verify actual native config,
certificate trust, routing, callback ordering, service/manifest and SDK/toolchain
compatibility, stop and process recreation, and all ABIs before enabling release.
No APK or native compile has been run. Default VPN Gate directory remains read-only.
