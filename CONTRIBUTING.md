# Contributing to Linky

Hey there! Thanks for wanting to contribute to Linky. Whether it's a bug fix, new feature, or just a typo correction - every contribution helps make this app better for everyone.

## Ways to Contribute

### Found a Bug?

1. Search [existing issues](https://github.com/ahmmedrejowan/Linky/issues) first - maybe it's already reported
2. If not, open a new issue using the Bug Report template
3. The more details you provide, the easier it is to fix!

### Have an Idea?

We love hearing new ideas! Share them in [Discussions](https://github.com/ahmmedrejowan/Linky/discussions/categories/ideas) or open a Feature Request issue.

### Want to Code?

Awesome! Here's how:

1. Fork the repo
2. Create a branch: `git checkout -b feature/your-feature`
3. Make your changes
4. Test it works
5. Open a Pull Request

Don't worry about getting everything perfect - we can work through it together in the PR.

## Setting Up Locally

**You'll need:**
- Android Studio (Ladybug or newer)
- JDK 17

**Quick start:**
```bash
git clone https://github.com/ahmmedrejowan/Linky.git
cd Linky
./gradlew assembleDebug
```

## Code Style

We try to keep things consistent:

- **Kotlin** - Follow standard [Kotlin conventions](https://kotlinlang.org/docs/coding-conventions.html)
- **Compose** - Use `remember`, proper state hoisting, keep composables small
- **Architecture** - Clean Architecture with MVVM

For commits, we use conventional format like:
- `feat(vault): add auto-lock timeout`
- `fix(import): fix duplicate detection`

But don't stress too much about this - we can always squash and clean up commits later.

## Project Structure

```
app/src/main/java/com/rejowan/linky/
├── data/
│   ├── local/           # Room database, DAOs, entities
│   ├── mapper/          # Entity <-> Domain mappers
│   ├── repository/      # Repository implementations
│   ├── export/          # Import/Export managers
│   └── security/        # Vault encryption
├── domain/
│   ├── model/           # Domain models
│   ├── repository/      # Repository interfaces
│   └── usecase/         # Use cases
├── presentation/
│   ├── components/      # Reusable UI components
│   ├── feature/         # Feature screens
│   ├── navigation/      # NavGraph, Routes
│   └── theme/           # Color, Theme, Type
├── di/                  # Koin DI modules
├── widget/              # Glance app widgets
├── worker/              # WorkManager workers
└── util/                # Utilities
```

## Key Technologies

- **UI:** Jetpack Compose + Material 3
- **Database:** Room
- **DI:** Koin
- **Async:** Kotlin Coroutines + Flow
- **Security:** AndroidX Security Crypto (AES-256-GCM)
- **Background:** WorkManager
- **Widgets:** Glance

## Questions?

- Need help? Ask in [Discussions Q&A](https://github.com/ahmmedrejowan/Linky/discussions/categories/q-a)
- Found a bug? Open an [Issue](https://github.com/ahmmedrejowan/Linky/issues)
- Have an idea? Share in [Discussions](https://github.com/ahmmedrejowan/Linky/discussions/categories/ideas)

---

Thanks again for contributing!
