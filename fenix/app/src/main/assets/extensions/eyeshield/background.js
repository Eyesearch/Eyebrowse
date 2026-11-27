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

let blockedThisSession = 0;

browser.webRequest.onBeforeRequest.addListener(
  (details) => {
    if (isBlocked(hostOf(details.url))) {
      blockedThisSession += 1;
      browser.storage.local.set({ blockedTotal: blockedThisSession, lastBlocked: hostOf(details.url) });
      return { cancel: true };
    }
    return {};
  },
  { urls: ["<all_urls>"] },
  ["blocking"],
);
