# Prism

Lightweight Android launcher for Minecraft Java Edition.

## Features

- Offline mode (username-only login)
- Bundled jar — no extra downloads needed
- Configurable RAM and resolution
- Minimal UI, fast launch

## Build

### GitHub Actions (recommended)
Push to `main` and the APK will be built automatically.
Download from the Actions artifacts.

### Local
```bash
# Download and patch the jar
./scripts/download_and_patch.sh app/src/main/assets

# Build
./gradlew assembleDebug
```

## Requirements

- Android 8.0+ (API 26)
- arm64 device
- JRE runtime in assets (see docs)

## License

For educational purposes only.
