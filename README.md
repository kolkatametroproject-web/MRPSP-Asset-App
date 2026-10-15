# MRPSP Asset App (Android WebView source)

This source project is a minimal Android WebView wrapper intended to expose only the existing MRPSP asset checkout/return workflow. It relies on the deployed website for Firebase authentication, registered-face matching, PIN fallback, project-aware inventory, QR/barcode handling, and transaction logs.

## Cloud-only build
See `CLOUD_BUILD_GUIDE.md`. `.github/workflows/build-apk.yml` builds a debug APK using GitHub Actions, so Android Studio does not need to be installed on your own device.

## Important
- This ZIP contains source code, not a prebuilt APK.
- The GitHub Actions build has not been run/verified from this environment.
- Confirm `hostingUrl` in `app/src/main/java/com/mrpsp/assetapp/MainActivity.kt` matches your actual deployed Firebase Hosting URL.
- The hosted HTML must already implement the intended asset checkout/return, registered-face matching and PIN fallback flows.
- A WebView UI filter is not a security boundary. Enforce identity, project access, checkout/return invariants and logs with Firebase rules/trusted server-side logic.
- Never upload service-account keys or private credentials to the repository.
