// EyeShield: the browser engine calls Mozilla for its own settings, experiments, push and add-on
// catalogue, and none of that is written by our code, so deleting our code would not stop it.
// This cancels those requests inside the browser, before they leave the phone, and counts them so
// the number can be shown to the user instead of being taken on trust.

const BLOCKED_HOSTS = [
  "mozilla.org",
  "mozilla.com",
  "mozilla.net",
  "firefox.com",
  "firefox.net",
  "getpocket.com",
  "mozaws.net",
  "mozilla.cloud",
];

// Anything the engine reaches for on its own behalf.
const BLOCKED_EXACT = [
  "accounts.firefox.com",
  "addons.mozilla.org",
  "services.addons.mozilla.org",
  "addons.cdn.mozilla.net",
  "push.services.mozilla.com",
  "pushendpoint.mozilla.org",
  "location.services.mozilla.com",
  "firefox.settings.services.mozilla.com",
  "incoming.telemetry.mozilla.org",
  "probeinfo.telemetry.mozilla.org",
  "spocs.getpocket.com",
  "img-getpocket.cdn.mozilla.net",
];

function hostOf(url) {
  try {
    return new URL(url).hostname.toLowerCase();
  } catch (e) {
    return "";
  }
}

function isBlocked(host) {
  if (!host) {
    return false;
  }
  if (BLOCKED_EXACT.includes(host)) {
    return true;
  }
  return BLOCKED_HOSTS.some((domain) => host === domain || host.endsWith("." + domain));
}

// The ad list ships inside the APK, so blocking works on the first page and offline. It is a plain
// list of domains, one per line, produced at build time from a public filter list.
let adHosts = new Set();

async function loadAdList() {
  try {
    const response = await fetch(browser.runtime.getURL("ads.txt"));
    if (!response.ok) {
      return;
    }
    const text = await response.text();
    adHosts = new Set(
      text.split("\n").map((line) => line.trim().toLowerCase()).filter((line) => line && !line.startsWith("#")),
    );
  } catch (e) {
    // No list is not an error: the shield still stops Mozilla's own calls.
  }
}

loadAdList();

function isAd(host) {
  if (!host) {
    return false;
  }
  if (adHosts.has(host)) {
    return true;
  }
  // a domain on the list also covers its subdomains, which is how ad servers are usually served
  return [...adHosts].some((domain) => host.endsWith("." + domain));
}

let blockedThisSession = 0;

browser.webRequest.onBeforeRequest.addListener(
  (details) => {
    const host = hostOf(details.url);
    if (isBlocked(host) || isAd(host)) {
      blockedThisSession += 1;
      browser.storage.local.set({ blockedTotal: blockedThisSession, lastBlocked: hostOf(details.url) });
      return { cancel: true };
    }
    return {};
  },
  { urls: ["<all_urls>"] },
  ["blocking"],
);
