#!/usr/bin/env python3
"""One-time first-party source migration, not a third-party branding replacement."""
from pathlib import Path
import re
import subprocess
import sys

OLD = "org.mozilla.fenix"
NEW = "org.eyesearch.eyebrowse"
OLD_PATH = OLD.replace(".", "/")
NEW_PATH = NEW.replace(".", "/")
CLASSES = (
    "FenixLogSink", "FenixDialogFragment", "FenixSnackbarDelegate",
    "FenixSnackbar", "FenixSnackbarBehavior", "FenixAccountManager",
    "FenixFxAEntryPoint", "FenixTabCounterMenu", "FenixOverlay",
    "FenixMessageSurfaceId", "FenixOnboarding", "FenixSwitchPreference",
    "FenixTypography", "FenixRobolectricTestApplication",
    "FenixRobolectricTestRunner",
)
RESOURCES = (
    "fenix_error_1", "fenix_search_widget", "fenix_snackbar_background",
    "fenix_snackbar_error_background", "fenix_snackbar",
    "preference_widget_switch_fenix_style",
)
RENAME = {x: x.replace("Fenix", "Eyebrowse", 1) for x in CLASSES}
RENAME.update({x + "Test": RENAME[x] + "Test" for x in CLASSES})
RENAME.update({"FenixSnackbarKt": "EyebrowseSnackbarKt",
               "FenixSnackbarKtTest": "EyebrowseSnackbarKtTest"})
RENAME.update({x: x.replace("fenix", "eyebrowse") for x in RESOURCES})
RENAME["messaging-fenix.fml.yaml"] = "messaging-eyebrowse.fml.yaml"
NAMES = re.compile(r"(?<![A-Za-z0-9_])(" + "|".join(
    re.escape(x) for x in sorted(RENAME, key=len, reverse=True)
) + r")(?![A-Za-z0-9_])")
# Keep the broadcast action used by existing pinned widgets. It is a
# persisted protocol identifier, not the name of the receiver class.
KEEP_ACTION = OLD + ".onboarding.WidgetPinnedReceiver.widgetPinned"
ACTION_MARKER = "__EYEBROWSE_KEEP_LEGACY_WIDGET_ACTION__"
# Bare quoted Mozilla application IDs can denote the actual upstream app.
# Source declarations, Gradle namespace and FML package are handled separately.
APP_ID = re.compile(r"""(?P<quote>["'])(org\.mozilla\.fenix(?:\.(?:debug|nightly|beta|release))?)(?P=quote)""")
TEXT = {".kt", ".java", ".xml", ".gradle", ".kts", ".pro", ".yaml", ".yml",
        ".toml", ".properties", ".py", ".md", ".sh"}
def tracked():
    return [Path(p) for p in subprocess.check_output(
        ["git", "ls-files", "-z"]).decode().split("\0") if p]
def is_candidate(p):
    s = p.as_posix()
    if not (s.startswith("eyebrowse/") or s.startswith(".github/workflows/")):
        return False
    if any(x in p.parts for x in ("assets", "raw", "resources", "certificates")):
        return False
    return p.suffix in TEXT and p.name != "migrate_namespace.py"
def transform(p, data):
    if p.suffix == ".md":
        # Only our developer documentation, not upstream URLs/attribution.
        return data.replace("org/mozilla/fenix/", "org/eyesearch/eyebrowse/")
    data = data.replace(KEEP_ACTION, ACTION_MARKER)
    held = []
    def preserve(m):
        held.append(m.group())
        return "__EYEBROWSE_KEEP_APP_ID_" + str(len(held)-1) + "__"
    if p.suffix in {".kt", ".java", ".xml", ".py"}:
        data = APP_ID.sub(preserve, data)
    data = data.replace(OLD_PATH, NEW_PATH)
    data = re.sub(r"(?<![A-Za-z0-9_])org\.mozilla\.fenix(?![A-Za-z0-9_])", NEW, data)
    data = NAMES.sub(lambda m: RENAME[m.group()], data)
    for i, value in enumerate(held):
        data = data.replace("__EYEBROWSE_KEEP_APP_ID_" + str(i) + "__", value)
    return data.replace(ACTION_MARKER, KEEP_ACTION)
def destination(p):
    s = p.as_posix()
    if s.startswith("eyebrowse/"):
        s = s.replace("/" + OLD_PATH + "/", "/" + NEW_PATH + "/")
        result = Path(s)
        if result.name in RENAME:
            return result.with_name(RENAME[result.name])
        if result.stem in RENAME:
            return result.with_name(RENAME[result.stem] + result.suffix)
        return result
    return p
def main():
    files = tracked()
    changes = []
    targets = {}
    for p in files:
        q = destination(p)
        if q in targets:
            raise SystemExit(f"Path collision: {p} and {targets[q]} -> {q}")
        targets[q] = p
        if q != p and q.exists():
            raise SystemExit(f"Existing target: {q}")
        if is_candidate(p):
            raw = p.read_bytes()
            try:
                data = raw.decode("utf-8")
            except UnicodeDecodeError:
                raise SystemExit(f"Non UTF-8 source: {p}")
            new = transform(p, data).encode("utf-8")
        else:
            raw = new = None
        if q != p or raw != new:
            changes.append((p, q, raw, new))
    if not changes:
        print("No changes required.")
        return
    print(f"Planned {len(changes)} changes, {sum(p != q for p,q,_,_ in changes)} path moves")
    for p,q,_,_ in changes:
        if p != q:
            print(f"move {p} -> {q}")
    if "--apply" not in sys.argv:
        return
    for p,q,raw,new in changes:
        if q != p:
            q.parent.mkdir(parents=True, exist_ok=True)
            p.rename(q)
        if raw != new:
            q.write_bytes(new)
    gradle = Path("eyebrowse/app/build.gradle").read_text()
    assert "namespace 'org.eyesearch.eyebrowse'" in gradle
    assert 'applicationId "org.eyesearch.eyebrowse"' in gradle
    manifest = Path("eyebrowse/app/src/main/AndroidManifest.xml").read_text()
    assert ".EyebrowseApplication" in manifest
    for p in Path("eyebrowse").rglob("*"):
        if not p.is_file() or p.suffix not in {".kt", ".java"}:
            continue
        if "/" + OLD_PATH + "/" in p.as_posix():
            raise SystemExit(f"Legacy package path: {p}")
        if re.search(r"^\s*(?:package|import)\s+org\.mozilla\.fenix\b", p.read_text(), re.M):
            raise SystemExit(f"Legacy source declaration/import: {p}")
    print("Static namespace and application ID checks passed.")
if __name__ == "__main__":
    main()
