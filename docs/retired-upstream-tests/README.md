# Retired upstream feature tests

The Eyebrowse fork already removed the upstream add-on management UI, translation UI, and studies UI from the app before the namespace migration. CI run 46070301210 exposed unresolved references in inherited tests for those absent classes, menus and navigation actions. These tests cannot exercise the current app. Their exact original contents are retained here, outside the active Gradle source sets, rather than deleting the history or disabling the unit-test task.

The following 15 whole test files target removed implementations:

- `addons/AddonDetailsBindingDelegateTest.kt`
- `addons/AddonPermissionDetailsBindingDelegateTest.kt`
- `addons/AddonsManagementViewTest.kt`
- `addons/ExtensionsProcessDisabledBackgroundControllerTest.kt`
- `addons/ExtensionsProcessDisabledForegroundControllerTest.kt`
- `addons/InstalledAddonDetailsFragmentTest.kt`
- `browser/TranslationsBindingTest.kt`
- `extension/WebExtensionPromptFeatureTest.kt`
- `settings/studies/DefaultStudiesInteractorTest.kt`
- `settings/studies/StudiesAdapterTest.kt`
- `settings/studies/StudiesViewTest.kt`
- `translations/TranslationsDialogBindingTest.kt`
- `translations/TranslationsDialogMiddlewareTest.kt`
- `translations/TranslationsDialogReducerTest.kt`
- `translations/preferences/downloadlanguages/DownloadLanguagesFeatureTest.kt`

Eight mixed test files retain all unrelated tests. The 14 obsolete methods were removed from those active files; original mixed files are archived under `original-mixed/`. The former add-on-manager deep link test now checks that the unsupported link does not navigate. No currently implemented feature was removed to make compilation pass. If those upstream features are restored, restore and adapt their archived tests at the same time. The full remaining app unit-test task is still required by the APK verification workflow. This does not establish that the remaining suite passes, nor does it substitute for device testing.
