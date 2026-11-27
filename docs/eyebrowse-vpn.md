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
It is deliberately not integrated here.

## Usage and actual behavior
1. Settings > Free VPN providers and profiles > choose an official provider.
2. Generate and download a profile under the provider's terms. Import it in
   Eyebrowse. Add up to five valid profiles from authorized servers.
3. Tap VPN. Android may request consent. Subsequent taps reconnect using saved
   profiles; the most recently successful profile in this process is tried first.
4. If a profile never handshakes, try the next saved profile after 40 seconds,
   at most once per attempt. A later stale handshake ends the session; reconnect
   selects another candidate. Consent revocation never auto-restarts the VPN.
5. Use the profile management row to delete saved credentials.

Only Eyebrowse traffic is routed. Require a single peer with a configured
endpoint, DNS and IPv4/IPv6 default routes. Keys are AES-GCM encrypted using
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
