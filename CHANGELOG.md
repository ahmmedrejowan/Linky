# Changelog

All notable changes to Linky will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

---

## [1.0.0] - 2026-03-20

### Added
- **Link Management**
  - Save links with automatic preview fetching (title, description, image)
  - Favorite links for quick access
  - Archive links to declutter main view
  - Soft delete with 30-day trash retention
  - Automatic trash cleanup via WorkManager

- **Collections**
  - Create custom collections with icons and colors
  - Organize links into collections
  - Collection-based filtering

- **Vault (Secure Storage)**
  - PIN-protected encrypted storage (AES-256-GCM)
  - Queue-based vault move (send links without unlocking)
  - Auto-lock timeout settings
  - Secure PIN change functionality

- **Reader Mode & Snapshots**
  - Capture webpage content as readable snapshots
  - Markdown rendering with image support
  - Adjustable font size and line height
  - Offline reading capability

- **Batch Import**
  - Paste multiple URLs for bulk import
  - URL extraction from text
  - Preview fetching with progress tracking
  - Duplicate detection and filtering

- **Import/Export**
  - Export data to .linky format (ZIP-based)
  - Import backups with conflict resolution
  - Skip or replace duplicate handling

- **Utilities**
  - Link health checker (detect broken links)
  - Duplicate link detection and merge
  - Advanced filtering (by domain, date, notes, preview)
  - Bulk operations (delete, favorite, move)

- **Widgets**
  - Home screen widget showing recent links
  - Quick access to saved links

- **UI/UX**
  - Material 3 design with dynamic theming
  - Dark/Light/System theme support
  - Grid and list view modes
  - Pull-to-refresh
  - Clipboard URL detection
  - Share intent handling

- **Settings**
  - Appearance customization
  - Privacy & security options
  - Data storage management
  - In-app update checker

### Technical
- **Architecture:** Clean Architecture with MVVM
- **UI:** Jetpack Compose with Material 3
- **Database:** Room with migrations
- **DI:** Koin dependency injection
- **Async:** Kotlin Coroutines and Flow
- **Security:** AndroidX Security Crypto
- **Background:** WorkManager for periodic tasks
- **Widgets:** Glance App Widgets

---

## Version History

| Version | Release Date | Highlights |
|---------|--------------|------------|
| 1.0.0 | 2026-03-20 | Initial release |

---

[Unreleased]: https://github.com/ahmmedrejowan/Linky/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/ahmmedrejowan/Linky/releases/tag/v1.0.0
