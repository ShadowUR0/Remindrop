# Releasing Remindrop

Remindrop's normal CI builds an installable debug APK on every push and an unsigned optimized release APK for verification. Production GitHub Releases use one persistent private signing key so users can update from one release to the next without Android signature conflicts.

## One-time signing setup

Create the release keystore on your own computer and keep at least one secure offline backup. Do not generate a new key for later releases. Losing this key means future direct-download APKs cannot update installations signed with it.

### 1. Create the keystore

With JDK `keytool` available:

```bash
keytool -genkeypair -v \
  -storetype JKS \
  -keystore remindrop-release.jks \
  -alias remindrop \
  -keyalg RSA \
  -keysize 4096 \
  -validity 10000
```

On Windows PowerShell the command can be entered on one line:

```powershell
keytool -genkeypair -v -storetype JKS -keystore remindrop-release.jks -alias remindrop -keyalg RSA -keysize 4096 -validity 10000
```

Choose a strong store password. When `keytool` asks for the key password, you can press Enter to use the same password. Keep the keystore and password somewhere safe outside the Git repository.

### 2. Base64-encode the keystore

Linux/macOS:

```bash
base64 -w 0 remindrop-release.jks > remindrop-release.base64.txt
```

Windows PowerShell:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("$PWD\remindrop-release.jks")) | Set-Content -NoNewline remindrop-release.base64.txt
```

### 3. Add GitHub Actions secrets

Open the repository on GitHub, then go to **Settings → Secrets and variables → Actions → New repository secret** and add these four secrets:

- `REMINDROP_RELEASE_KEYSTORE_BASE64` — the full contents of `remindrop-release.base64.txt`
- `REMINDROP_RELEASE_STORE_PASSWORD` — the keystore password
- `REMINDROP_RELEASE_KEY_ALIAS` — `remindrop`
- `REMINDROP_RELEASE_KEY_PASSWORD` — the key password (the same as the store password if you pressed Enter above)

Never commit the `.jks`, base64 text, or passwords to the repository.

## Create a release

After the four secrets exist, the easiest method is:

1. Open **Actions** in GitHub.
2. Select **Release**.
3. Select **Run workflow**.
4. Enter `v0.1.0` for the first release.
5. Run it from `main`.

The workflow checks that `v0.1.0` matches `versionName = "0.1.0"`, builds the optimized signed release, verifies its Android signature with `apksigner`, computes a SHA-256 checksum, creates the tag if needed, and publishes the GitHub Release.

You can alternatively create/push a matching Git tag yourself:

```bash
git tag v0.1.0
git push origin v0.1.0
```

The final release contains:

- `Remindrop-vX.Y.Z.apk`
- `Remindrop-vX.Y.Z.apk.sha256`
- GitHub's automatically generated source archives

The APK is built with R8 minification and Android resource shrinking. GitHub-hosted runner storage is ephemeral, and the decoded keystore is not committed or uploaded as an artifact.

## Before every later release

1. Increase `versionCode`.
2. Change `versionName` to the new version.
3. Add the matching Fastlane changelog file named after the new version code.
4. Let the normal Android build workflow pass.
5. Run the Release workflow with the matching tag, for example `v0.1.1`.

Always use the same release keystore.

## F-Droid

F-Droid builds from source and normally applies its own repository signing process. The GitHub release signing key is for direct APK distribution and is separate from F-Droid's signing flow.

See [F-DROID.md](F-DROID.md) for the official-repository submission process.
