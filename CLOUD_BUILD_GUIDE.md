# Build the MRPSP Asset APK entirely in the cloud (no PC software)

## What you need
- A GitHub account.
- A browser on your phone/tablet/any computer. Android Studio is not required locally.
- The MRPSP website URL must already be deployed and working.

## Steps
1. Sign in to https://github.com.
2. Create a new repository named `MRPSP-Asset-App`. For a private operational app, choose **Private**.
3. Upload the extracted project files/folders to the repository root, preserving the directory structure. Make sure `.github/workflows/build-apk.yml`, `app/`, `build.gradle.kts`, `settings.gradle.kts`, and `gradle.properties` are present at the repository root.
   - If GitHub's browser upload cannot preserve the folders from your device, use GitHub Codespaces for that repository and upload/extract the ZIP in the Codespaces browser environment, then commit the extracted files.
4. Open the repository's **Actions** tab. If prompted, enable Actions.
5. Select **Build MRPSP Android APK** from the workflow list, then press **Run workflow**. A push to `main` also starts a build.
6. Wait until the workflow finishes with a green check mark.
7. Open that workflow run, scroll to **Artifacts**, and download `MRPSP-Asset-App-debug-APK`.
8. Extract the downloaded artifact ZIP. It contains `app-debug.apk`.
9. Transfer/open `app-debug.apk` on your Android phone and install it. If Android prompts you, allow installs from that browser/file manager.

## If the build fails
Open the workflow run, open the failed step, and copy the first actual Gradle/SDK error. Do not publish Firebase rules more broadly to fix a build error.

## Before operational use
This is a debug APK for testing, not a signed production release. Verify the website URL in `MainActivity.kt`, camera permission/WebView behaviour, face matching/PIN fallback, both inventory paths, user-specific checkout/return, required return location, and transaction logs. The WebView UI hides other modules but does not secure Firebase by itself. Keep authorization enforced by Firebase rules and trusted server-side logic. Never commit service-account keys or private credentials to GitHub.
