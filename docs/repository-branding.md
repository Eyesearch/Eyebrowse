# Repository branding policy

Eyebrowse is the first-party product name. Use it in product descriptions, repository metadata, app-facing strings, first-party artwork and developer instructions. The release application ID is org.eyesearch.eyebrowse.

The upstream Mozilla Firefox for Android project, GeckoView, Android Components, Mozilla license headers and real external domains/extension IDs are retained under their actual names. Do not mechanically replace these identifiers: renaming them can break dependency resolution, web compatibility, privacy filtering or legal attribution.

The repository tree was reviewed for old-brand filenames and first-party metadata. Source search also found legitimate upstream telemetry descriptions, experiment payload IDs, test fixtures, webcompat references and external profiler documentation. Those are not product branding. The repository-wide guard warns about paths for review, and rejects regressions in selected first-party product metadata. It is not a claim that every file has been exhaustively reviewed.

EyeShield's prior blanket string replacement had changed Mozilla's real Firefox domains to Eyebrowse domains. The blocklist now targets the real external domains again; other endpoint behavior and privacy claims are unchanged and still require network testing. Do not substitute a product brand into a third-party hostname.

The repository's application directory has been migrated from `fenix/` to `eyebrowse/`, and the Gradle root project name is Eyebrowse. CI paths, brand guard, developer documentation and first-party package configuration were updated. The Gradle flavor is now `eyebrowse`, and the first-party dependency plugin and application class have been renamed. The first-party source namespace has been migrated to `org.eyesearch.eyebrowse`, including generated binding and protobuf package references. The namespace migration and debug Kotlin compilation passed in GitHub Actions run 46068419127. Legacy deep-link schemes and persisted identifiers are retained until compatibility is verified. APK and device verification are separate from successful compilation.
