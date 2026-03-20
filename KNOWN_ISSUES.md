# Known Issues

This document lists known issues and limitations in Linky, along with workarounds where available.

## Link Previews

### Some websites don't return preview data
**Issue:** Certain websites block meta tag scraping or don't provide Open Graph/Twitter Card metadata, resulting in missing titles, descriptions, or images.

**Workaround:** Manually edit the link to add a custom title and description.

**Status:** Limitation of website configuration

---

### Preview images may not load for some sites
**Issue:** Some websites serve images that require authentication or have CORS restrictions.

**Workaround:** The link will still be saved; only the preview image will be missing.

**Status:** Known limitation

---

## Reader Mode & Snapshots

### Reader mode fails on some websites
**Issue:** Websites with heavy JavaScript rendering, paywalls, or anti-scraping measures may not parse correctly in reader mode.

**Workaround:** Open the link in a browser instead.

**Status:** Limitation of Readability4J parser

---

### Snapshot content may differ from live page
**Issue:** Snapshots capture the page content at a point in time. Dynamic content or login-required content won't be captured.

**Workaround:** Snapshots are best for static articles and blog posts.

**Status:** By design

---

## Vault

### Vault data lost on app reinstall
**Issue:** If you reinstall the app without backing up, vault links are permanently lost as the encryption keys are tied to the app installation.

**Workaround:** Always export a backup before uninstalling the app.

**Status:** Security feature (encryption keys are not recoverable)

---

### Pending vault links visible briefly
**Issue:** Links queued for vault (before unlock) are stored unencrypted until the vault is unlocked.

**Workaround:** Unlock the vault periodically to process pending links.

**Status:** Known trade-off for UX convenience

---

## Clipboard Detection

### Clipboard URL not detected on some devices
**Issue:** Some device manufacturers restrict clipboard access in the background, preventing automatic URL detection.

**Workaround:** Manually paste the URL using the add link screen.

**Status:** Android platform/OEM restriction

---

### Clipboard prompt appears multiple times
**Issue:** The clipboard prompt may reappear if the app is quickly backgrounded and foregrounded.

**Workaround:** Dismiss the prompt; it's throttled to once per 3 seconds.

**Status:** Minor UX issue

---

## Widget

### Widget doesn't update immediately
**Issue:** The home screen widget may not reflect newly added links immediately.

**Workaround:** The widget updates periodically. You can also re-add the widget to force a refresh.

**Status:** Android widget update limitations

---

## Import/Export

### Large backups may take time
**Issue:** Exporting or importing backups with many links (1000+) may take several seconds.

**Workaround:** Be patient; a progress indicator is shown.

**Status:** Normal behavior

---

### Legacy JSON imports not supported
**Issue:** Only .linky format backups are supported. Plain JSON exports from other apps cannot be imported directly.

**Workaround:** Manually add links or use batch import with URLs.

**Status:** By design

---

## Performance

### Initial app launch may be slow
**Issue:** The first launch after installation may take a few seconds while initializing the database and preloading data.

**Workaround:** Subsequent launches will be faster.

**Status:** Normal behavior for initial setup

---

### Many links may slow down scrolling
**Issue:** Having thousands of links with images may cause slight lag when scrolling.

**Workaround:** Use collections to organize links and reduce the number displayed at once.

**Status:** Under optimization

---

## Reporting New Issues

If you encounter an issue not listed here, please report it:

1. **Check existing issues:** [GitHub Issues](https://github.com/ahmmedrejowan/Linky/issues)
2. **Create a new issue** with:
   - Device model and Android version
   - App version
   - Steps to reproduce
   - Expected vs actual behavior
   - Screenshots if applicable

---

## Fixed Issues

Issues that have been fixed in recent releases:

| Issue | Fixed In | Description |
|-------|----------|-------------|
| - | v1.0.0 | Initial release |

---

*Last updated: 2026-03-20*
