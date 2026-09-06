# Releasing Remindrop

Remindrop's normal CI builds an installable debug APK on every push. Production GitHub Releases use a persistent private signing key so users can update from one release to the next without signature conflicts.

## One-time signing setup

Create a release keystore and keep an offline backup. Losing this key means future APKs cannot update installations signed with it.

Example with JDK `keytool`:

```bash
keytool -genkeypair -v \
  -keystore remindrop-release.jks \
  -alias remindrop \
  -keyalg RSA \
  -keysize 4096 \
  -validity 10000
```

Base64-encode the keystore as a single line and add the following GitHub Actions repository secrets:

- `REMINDROP_RELEASE_KEYSTORE_BASE64`
- `REMINDROP_RELEASE_STORE_PASSWORD`
- `REMINDROP_RELEASE_KEY_ALIAS`
- `REMINDROP_RELEASE_KEY_PASSWORD`

Never commit the keystore or its passwords to the repository.

## Create a release

After the signing secrets exist, push a version tag such as:

```bash
git tag v0.1.0
git push origin v0.1.0
```

The `Release` workflow will:

1. decode the private keystore only inside the temporary GitHub runner
2. build the minified release APK
3. run release lint checks
4. create the GitHub Release
5. attach `Remindrop-vX.Y.Z.apk`

GitHub-hosted runner storage is ephemeral. The decoded keystore is not committed or uploaded as an artifact.

## F-Droid

F-Droid builds from source and applies its own repository signing process. The GitHub release signing key is for direct APK distribution and is separate from F-Droid's signing flow.
