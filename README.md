# Eyebrowse

Eyebrowse is a privacy-focused mobile browser for Android, built by Eyesearch. It is a
rebranded, trimmed fork of Firefox for Android, with the Mozilla services removed: no
telemetry, no Mozilla account servers, no sponsored content, no Mozilla help links.

## What is in here

| Path | What it is |
| --- | --- |
| `fenix/` | The Android app (UI, settings, screens). This is the product. |
| `android-components/` | The browser component library the app is built on. |
| `focus-android/` | The upstream lightweight browser variant, kept for the shared components. |
| `version.txt` | The upstream version this tree is based on. |

## Build

The debug APK is built by CI on every push to `main` and every pull request, in the
`Assemble Eyebrowse debug APK` workflow. The artifact is downloadable from the run page.

To build locally you need JDK 17, the Android SDK, and network access to
`maven.mozilla.org`, which is where the GeckoView and application-services binaries come
from:

```
cd fenix
./gradlew assembleFenixDebug
```

## What was changed from upstream

- Rebranded to Eyebrowse: app name, icon, sign-in artwork, all user-visible strings.
- Telemetry upload and Mozilla experiment fetches are off.
- DuckDuckGo is the only search engine, bundled locally, no remote settings fetch.
- Account and sync servers point at our own backend, not Mozilla's.
- Help, legal and in-product links point at `eyebrowse.in`.
- Removed: add-ons, translations, report-site-issue, studies, Mozilla Online China
  monetisation links.

## Licence

MPL 2.0, see `LICENSE`. Files modified by Eyesearch keep their MPL headers, and the
source of those modifications is this repository. Mozilla trademarks and the Firefox name
are not used.
