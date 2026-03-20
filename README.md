<div align="center">
  <img src="https://raw.githubusercontent.com/ahmmedrejowan/Linky/main/files/logo.png" alt="Linky Logo" width="120" height="120">

<h3>Modern Link & Bookmark Manager for Android</h3>

<p>
    A privacy-focused link manager built with Jetpack Compose and Material 3. Save, organize, and secure your bookmarks with collections, vault encryption, reader mode, and more.
  </p>

[![Android](https://img.shields.io/badge/Platform-Android-green.svg?style=flat)](https://www.android.com/)
[![API](https://img.shields.io/badge/API-24%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=24)
[![License](https://img.shields.io/badge/License-GPL%20v3-blue.svg)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-100%25-purple.svg)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Latest-blue.svg)](https://developer.android.com/jetpack/compose)

</div>

---

## Features

- **Link Management** - Save links with automatic preview fetching (title, description, image)
- **Collections** - Organize links into custom collections with icons and colors
- **Vault** - PIN-protected encrypted storage (AES-256-GCM) for sensitive links
- **Reader Mode** - Capture webpage snapshots for offline reading
- **Batch Import** - Paste multiple URLs for bulk import with duplicate detection
- **Import/Export** - Backup and restore your data in .linky format
- **Favorites & Archive** - Quick access to important links and declutter your view
- **Trash** - Soft delete with 30-day retention and auto-cleanup
- **Widgets** - Home screen widget for quick access to recent links
- **Material 3 Design** - Modern UI with dark mode and dynamic colors
- **100% Privacy** - No ads, no tracking, no analytics

---

## Download

![GitHub Release](https://img.shields.io/github/v/release/ahmmedrejowan/Linky)

You can download the latest APK from here

<a href="https://github.com/ahmmedrejowan/Linky/releases/latest">
<img src="https://raw.githubusercontent.com/ahmmedrejowan/Linky/main/files/get.png" width="224px" align="center"/>
</a>

Check out the [releases](https://github.com/ahmmedrejowan/Linky/releases) section for more details.

---

## Screenshots


| Shots                                                                                        | Shots                                                                                        | Shots                                                                                        |
| -------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------- |
| ![Screenshot 1](https://raw.githubusercontent.com/ahmmedrejowan/Linky/main/files/shot1.webp) | ![Screenshot 2](https://raw.githubusercontent.com/ahmmedrejowan/Linky/main/files/shot2.webp) | ![Screenshot 3](https://raw.githubusercontent.com/ahmmedrejowan/Linky/main/files/shot3.webp) |
| ![Screenshot 4](https://raw.githubusercontent.com/ahmmedrejowan/Linky/main/files/shot4.webp) | ![Screenshot 5](https://raw.githubusercontent.com/ahmmedrejowan/Linky/main/files/shot5.webp) | ![Screenshot 6](https://raw.githubusercontent.com/ahmmedrejowan/Linky/main/files/shot6.webp) |
| ![Screenshot 7](https://raw.githubusercontent.com/ahmmedrejowan/Linky/main/files/shot7.webp) | ![Screenshot 8](https://raw.githubusercontent.com/ahmmedrejowan/Linky/main/files/shot8.webp) | ![Screenshot 9](https://raw.githubusercontent.com/ahmmedrejowan/Linky/main/files/shot9.webp) |

---

## Architecture

Linky follows **Clean Architecture** principles with **MVVM** pattern:

```
app/src/main/java/com/rejowan/linky/
├── data/                      # Data layer
│   ├── local/                 # Room database, DAOs, entities
│   ├── mapper/                # Entity <-> Domain mappers
│   ├── repository/            # Repository implementations
│   ├── export/                # Import/Export managers
│   └── security/              # Vault encryption
│
├── domain/                    # Domain layer
│   ├── model/                 # Domain models
│   ├── repository/            # Repository interfaces
│   └── usecase/               # Use cases
│
├── presentation/              # Presentation layer (UI)
│   ├── components/            # Reusable Compose components
│   ├── feature/               # Feature screens
│   ├── navigation/            # Navigation graph
│   └── theme/                 # Material 3 theming
│
├── di/                        # Koin dependency injection
├── widget/                    # Glance app widgets
├── worker/                    # WorkManager workers
└── util/                      # Utilities
```

### Tech Stack

- **UI Framework**: Jetpack Compose (100% Compose UI)
- **Language**: Kotlin (100%)
- **Architecture**: MVVM + Clean Architecture
- **Dependency Injection**: Koin
- **Database**: Room
- **Async**: Kotlin Coroutines + Flow
- **Navigation**: Jetpack Navigation Compose
- **Security**: AndroidX Security Crypto (AES-256-GCM)
- **HTML Parsing**: Jsoup
- **Reader Mode**: Readability4J
- **Image Loading**: Coil
- **Widgets**: Glance
- **Background Tasks**: WorkManager

---

## Requirements

- **Minimum SDK**: API 24 (Android 7.0 Nougat)
- **Target SDK**: API 36 (Android 16)
- **Compile SDK**: API 36
- **Gradle**: 9.4.0
- **AGP**: 9.1.0
- **Kotlin**: 2.3.10
- **Java**: 17

### Permissions

- `INTERNET` - Fetch link previews and webpage content
- `POST_NOTIFICATIONS` - Show clipboard detection prompts (Android 13+)

**Note:** This app does not collect or transmit any user data.

---

## Build & Run

To build and run the project, follow these steps:

1. Clone the repository:
   ```bash
   git clone https://github.com/ahmmedrejowan/Linky.git
   ```
2. Open the project in Android Studio.
3. Sync the project with Gradle files.
4. Connect your Android device or start an emulator.
5. Click on the "Run" button in Android Studio to build and run the app.

---

## Testing

```bash
# Run unit tests
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest
```

---

## Contributing

Contributions are welcome! Please read [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

### Quick Start

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---

## License

```
Copyright (C) 2026 K M Rejowan Ahmmed

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.
```

> **Warning**
> This is a copyleft license. Any derivative work must also be open source under the same license.

---

## Community

- [Discussions](https://github.com/ahmmedrejowan/Linky/discussions) - Ask questions, share ideas
- [Issues](https://github.com/ahmmedrejowan/Linky/issues) - Report bugs, request features
- [Releases](https://github.com/ahmmedrejowan/Linky/releases) - Download latest versions

---

## Author

**K M Rejowan Ahmmed**

- GitHub: [@ahmmedrejowan](https://github.com/ahmmedrejowan)
- Email: [kmrejowan@gmail.com](mailto:kmrejowan@gmail.com)

---

## Acknowledgments

- [Jetpack Compose](https://developer.android.com/jetpack/compose) - Modern Android UI toolkit
- [Material Design 3](https://m3.material.io/) - Design system
- [Room](https://developer.android.com/training/data-storage/room) - Database library
- [Koin](https://insert-koin.io/) - Dependency injection framework
- [Coil](https://coil-kt.github.io/coil/) - Image loading library
- [Jsoup](https://jsoup.org/) - HTML parsing library
- [Readability4J](https://github.com/nicklockwood/SwiftFormat) - Reader mode extraction
- [Glance](https://developer.android.com/jetpack/compose/glance) - App widget framework

---

## Changelog

### v1.0.0 (2026-03-20) - Initial Release

**Link Management**
- Save links with automatic preview fetching (title, description, image)
- Favorite, archive, and soft-delete links with 30-day trash retention

**Collections**
- Create custom collections with icons and colors for organized browsing

**Vault**
- PIN-protected encrypted storage (AES-256-GCM) with auto-lock and queue-based import

**Reader Mode & Snapshots**
- Capture webpage content as offline-readable snapshots with adjustable typography

**Batch Import / Export**
- Paste multiple URLs for bulk import; export and restore data in `.linky` format

**Utilities**
- Link health checker, duplicate detection, advanced filtering, and bulk operations

**Widgets & UI**
- Home screen widget, Material 3 design, dark/light/system themes, grid and list views

**Technical**
- Clean Architecture + MVVM, Jetpack Compose, Room, Koin, Coroutines, WorkManager

See [CHANGELOG.md](CHANGELOG.md) for the full version history.

---
