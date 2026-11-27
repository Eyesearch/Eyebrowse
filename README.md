# Eyebrowse

Eyebrowse is an Android browser developed by Eyesearch. It is an independent fork of Mozilla's open-source mobile browser, using GeckoView and Mozilla Android Components. The product name, logo and account experience are Eyebrowse.

## Repository structure

| Path | Purpose |
| --- | --- |
| `eyebrowse/` | Eyebrowse Android application. |
| `android-components/` | Shared browser components, including upstream libraries. |
| `docs/` | Eyebrowse technical notes and validation checklists. |
| `version.txt` | Upstream version of the source tree. |

## Development

Use JDK 17 and Android SDK 34. The ordinary build resolves GeckoView and other dependencies from their existing repositories.

```sh
cd eyebrowse
./gradlew assembleEyebrowseDebug
```

The APK workflow is `Build APKs`; it runs on manual dispatch or an `apk-*` tag. The compile-check workflow runs on pushes to develop and relevant pull requests. Do not create APK tags just to change documentation or branding.

The application ID is `org.eyesearch.eyebrowse` for release, with channel-specific suffixes for other builds.

## Features and validation

- Eyebrowse branding and shared splash/launcher resources.
- Indo Account integration, EyeBlock and EyeShield.
- WireGuard support using authorized third-party profiles; VPN Gate directory and opt-in experimental OpenVPN development.
- Runtime behavior, privacy claims and VPN connectivity require device validation. See the technical notes in `docs/`.

## Source attribution and license

The upstream browser is Mozilla Firefox for Android (internal codename Fenix). Eyebrowse is not an official Mozilla product. Genuine upstream names, license notices, source attribution, dependency coordinates, external URLs, extension identifiers and compatibility test fixtures must not be mechanically rebranded.

See `LICENSE` (MPL 2.0) and the applicable licenses of bundled third-party components. An experimental OpenVPN backend is not part of ordinary APK builds.
