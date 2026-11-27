# Mozilla connections

Every address in this app that belongs to Mozilla, and what is done about it.

The point of this file is that it can be read. If a connection to Mozilla exists, it is written
here, with the file that makes it. This is a source-level inventory, not a complete network audit. Runtime connections require device testing.

## Removed, nothing is sent

| What | Was | Now |
| --- | --- | --- |
| Usage data (Glean) | uploaded to Mozilla's telemetry endpoint | upload forced off in `GleanMetricsService`, switches removed from Settings |
| Marketing data | uploaded to Mozilla | off, switch removed |
| Experiments (Nimbus) | fetched from `firefox.settings.services.mozilla.com` | off, default false |
| Pocket stories | `firefox-android-home-recommendations.getpocket.com` | endpoint pointed at loopback, nothing leaves |
| Help, privacy, what's new | `support.mozilla.org`, `www.mozilla.org` | our own page |

## Still inside the engine

These are upstream service domains relevant to the engine and inherited components, not proof of observed network traffic:

- `firefox.settings.services.mozilla.com` - remote settings, the engine's own configuration
- `push.services.mozilla.com` - push, used by the sync stack
- `accounts.firefox.com` - upstream account service
- `addons.mozilla.org` - the add-on store the engine talks to
- `location.services.mozilla.com` - geolocation, if a page asks for it
- `safebrowsing.googleapis.com` - safe browsing, Google rather than Mozilla, listed for honesty

## The block list

The hosts below are the ones this repository names, taken from the source rather than guessed. They
are the entries for the browser WebExtension block that ships with the ad blocker:

```
mozilla.org
mozilla.com
mozilla.net
firefox.com
firefox.net
getpocket.com
mozaws.net
mozilla.cloud
accounts.firefox.com
addons.mozilla.org
services.addons.mozilla.org
addons.cdn.mozilla.net
push.services.mozilla.com
pushendpoint.mozilla.org
location.services.mozilla.com
firefox.settings.services.mozilla.com
incoming.telemetry.mozilla.org
probeinfo.telemetry.mozilla.org
spocs.getpocket.com
img-getpocket.cdn.mozilla.net
```

Blocking the whole of `mozilla.org` and `firefox.com` also covers the developer documentation and
the store, which a user may legitimately want to open in a tab. That is a decision to make, not an
accident: the list above is written down so it can be narrowed before it is switched on.

Some inherited code still contains deliberately altered or disabled endpoint settings. The hostnames listed here identify genuine external services and the EyeShield targets; they are not instructions to re-enable remote fetches. EyeShield is a WebExtension filter, not a packet-level firewall.
