# Eyebrowse VPN

The former route-less VpnService has been removed. This is an import-based
WireGuard client, not a hosted VPN service. The library is pinned to
1.0.20230706, whose upstream Android SDK 34 and min SDK 21 align with this
repository. Review a newer compatible release during the Android SDK upgrade.
The library is Apache-2.0 licensed; retain upstream notices and source/license
information when distributing the app.

## Usage
Settings > VPN > Import profile. Select a trusted WireGuard configuration
from a provider or your own server, then accept Android VPN consent.
Require exactly one peer with an endpoint, DNS servers, and both IPv4 and IPv6
default routes. Scope the interface to the installed Eyebrowse package only.
Profiles are not persisted or logged. The server handshake status is based on
WireGuard statistics, not a locally created interface. A handshake is not
proof that the server forwards public internet traffic.

## Limitations and release checks
No Eyebrowse-operated VPN endpoint, profile provisioning, automatic reconnect,
always-on mode or kill switch is provided. Traffic may return to the normal
network if the tunnel drops; disclose this clearly. Other apps are not routed.
Check dependency resolution, merged manifest, native ABIs and release build.
Test invalid/oversized profiles, consent cancellation, offline server,
handshake, IPv4/IPv6 public IP and DNS with a trusted test endpoint, browser-only
scope, backgrounding, network transitions, disconnect, revoke and process
death. Do not ship a hosted VPN claim until server infrastructure and security
testing are complete.
