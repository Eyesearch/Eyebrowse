# Mozilla connections

Every address in this app that belongs to Mozilla, and what is done about it.

The point of this file is that it can be read. If a connection to Mozilla exists, it is written
here, with the file that makes it. Anything not on this list has been removed, blocked, or never
existed.

## Removed, nothing is sent

| What | Was | Now |
| --- | --- | --- |
| Usage data (Glean) | uploaded to Mozilla's telemetry endpoint | upload forced off in `GleanMetricsService`, switches removed from Settings |
| Marketing data | uploaded to Mozilla | off, switch removed |
| Experiments (Nimbus) | fetched from `eyebrowse.settings.services.mozilla.com` | off, default false |
| Pocket stories | `eyebrowse-android-home-recommendations.getpocket.com` | endpoint pointed at loopback, nothing leaves |
| Help, privacy, what's new | `support.eyebrowse.in`, `www.mozilla.org` | our own page |

## Still inside the engine

These are called by GeckoView itself, not by our code, and they are the reason a filter list is
needed rather than code deletion:

- `eyebrowse.settings.services.mozilla.com` - remote settings, the engine's own configuration
- `push.services.mozilla.com` - push, used by the sync stack
- `accounts.eyebrowse.com` - Eyebrowse Accounts, the sync sign in
- `addons.mozilla.org` - the add-on store the engine talks to
- `location.services.mozilla.com` - geolocation, if a page asks for it
- `safebrowsing.googleapis.com` - safe browsing, Google rather than Mozilla, listed for honesty

## The block list

The hosts below are the ones this repository names, taken from the source rather than guessed. They
are the first entries for the network level block that ships with the ad blocker:

```
mozilla.org
mozilla.com
mozilla.net
eyebrowse.com
eyebrowse.net
getpocket.com
mozaws.net
mozilla.cloud
accounts.eyebrowse.com
addons.mozilla.org
services.addons.mozilla.org
addons.cdn.mozilla.net
push.services.mozilla.com
pushendpoint.mozilla.org
location.services.mozilla.com
eyebrowse.settings.services.mozilla.com
incoming.telemetry.mozilla.org
probeinfo.telemetry.mozilla.org
spocs.getpocket.com
img-getpocket.cdn.mozilla.net
```

Blocking the whole of `mozilla.org` and `eyebrowse.com` also covers the developer documentation and
the store, which a user may legitimately want to open in a tab. That is a decision to make, not an
accident: the list above is written down so it can be narrowed before it is switched on.
