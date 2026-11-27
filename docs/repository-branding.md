# Repository branding policy

Eyebrowse is the first-party product name. Use it in product descriptions, repository metadata, app-facing strings, first-party artwork and developer instructions. The release application ID is org.eyesearch.eyebrowse.

The upstream Mozilla Firefox for Android project, Fenix internal flavor and namespace, GeckoView, Android Components, Mozilla license headers and real external domains/extension IDs are retained under their actual names. Do not mechanically replace these identifiers: renaming them can break dependency resolution, web compatibility, privacy filtering or legal attribution.

The repository tree was reviewed for old-brand filenames and first-party metadata. Source search also found legitimate upstream telemetry descriptions, experiment payload IDs, test fixtures, webcompat references and external profiler documentation. Those are not product branding. The repository-wide guard warns about paths for review, and rejects regressions in selected first-party product metadata. It is not a claim that every file has been exhaustively reviewed.

EyeShield's prior blanket string replacement had changed Mozilla's real Firefox domains to Eyebrowse domains. The blocklist now targets the real external domains again; other endpoint behavior and privacy claims are unchanged and still require network testing. Do not substitute a product brand into a third-party hostname.

The repository's application directory has been migrated from `fenix/` to `eyebrowse/`, and the Gradle root project name is Eyebrowse. CI paths, brand guard, developer documentation and first-party package configuration were updated. The internal `fenix` Gradle flavor and `org.mozilla.fenix` source namespace remain; migrating these needs coordinated refactoring of generated resources, imports and tests. Compilation and APK/device verification have not been performed.
