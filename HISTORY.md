# Change history

This repository is a fresh copy. The work before it was done in the previous
repository, which was a fork of `mozilla-mobile/firefox-android`. That fork is kept as
`Eyesearch/Eyebrowse1` (archived) for the record. This file lists what was done, so the
record survives here even if the archive is deleted.

All of these are merged into `main`. Each line is one pull request.

| # | Change | Commit |
| --- | --- | --- |
| 1 | Sign-in screen: new Eyebrowse artwork, our pairing URL | `0d2118f` |
| 2 | Telemetry: stop Glean uploads and Mozilla experiment fetches | `fb19282` |
| 3 | Search: DuckDuckGo as the only engine, no remote settings list | `5da0623` |
| 4 | Accounts: stop using Mozilla account and sync servers | `8682875` |
| 5 | Accounts: manage-account link opens our page, not Mozilla's | `960e0e4` |
| 6 | Links: help, legal and in-product links point at eyebrowse.in | `0663597` |
| 7 | Links: add-on help links point at our support site | `2db39cd` |
| 8 | Home: drop Mozilla Online China monetisation links and icons | `77f84f7` |
| 9 | Menu: remove Report site issue | `2ec4cf7` |
| 10 | Extensions: remove the entry points from menus and settings | `f5e7369` |
| 11 | Extensions: delete the add-on manager, screens and navigation | `59b0151` |
| 12 | Translations: delete the translation feature | `be8d135` |
| 13 | Experiments: remove the studies screen and the Nimbus hooks | `23310ff` |
| 14 | Repo: our own README and policies, drop Mozilla docs and bot config | `5cf3920` |

## Where things stand

- The app builds a debug APK in CI on every push to `main` and every pull request.
- Nothing in the app talks to Mozilla servers except the GeckoView and
  application-services binaries, which come from Maven.
- Sign-in and sync point at `accounts.eyebrowse.in`, which does not exist yet, so those
  screens fail by design until the backend is live.
