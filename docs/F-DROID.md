# Publishing Remindrop on F-Droid

Remindrop is designed to be eligible for the official F-Droid repository: it is GPL-3.0 licensed, has public source code, uses only free/open-source Android dependencies, has no ads or trackers, and does not request Internet access.

Application ID: `com.shadowuro.remindrop`

## What F-Droid does

The official F-Droid repository builds the APK from source. You do not upload your normal GitHub APK to F-Droid as the primary submission. F-Droid uses build metadata, checks out a specific source commit, builds it in its own environment, and normally signs the resulting APK with an F-Droid signing key.

## Before the first submission

1. Keep the source repository public.
2. Keep the GPL-3.0 license file in the repository.
3. Publish a stable tagged source release such as `v0.1.0`.
4. Make sure `versionName` and `versionCode` in `app/build.gradle.kts` match that release.
5. Keep the Fastlane metadata under `fastlane/metadata/android/` up to date. F-Droid can use this for the app title, descriptions, changelogs, and later screenshots.
6. Keep GitHub Issues enabled so F-Droid maintainers have a public issue tracker.

## Fastest submission route

The fastest route is a merge request to the official `fdroid/fdroiddata` repository on GitLab.

1. Fork `https://gitlab.com/fdroid/fdroiddata` on GitLab.
2. Create `metadata/com.shadowuro.remindrop.yml` in your fork.
3. Start from `packaging/fdroid/com.shadowuro.remindrop.yml.example` in this repository.
4. Replace `REPLACE_WITH_FULL_V0_1_0_COMMIT_SHA` with the full 40-character commit SHA that the `v0.1.0` tag points to.
5. Test the metadata with F-Droid Server when possible:

```bash
fdroid readmeta
fdroid rewritemeta com.shadowuro.remindrop
fdroid checkupdates --allow-dirty com.shadowuro.remindrop
fdroid lint com.shadowuro.remindrop
fdroid build com.shadowuro.remindrop
```

6. Commit the metadata to your fdroiddata fork and open a merge request to `fdroid/fdroiddata` with the `New App` label/template.
7. Watch the merge request and answer any packager or bot questions until it is accepted.

The official F-Droid quick-start guide also documents a container-based build environment if you do not want to install fdroidserver directly.

## Easier but slower route

If you do not want to write/test fdroiddata metadata yourself, open a Request For Packaging in the official F-Droid RFP tracker and provide the Remindrop source repository. A packager can prepare the metadata, but this normally takes longer than submitting a working fdroiddata merge request yourself.

## Future releases

The proposed metadata uses tagged releases. For each new public version:

1. Increase `versionCode` and `versionName`.
2. Update the Fastlane changelog using the new version code as the filename.
3. Build and verify the app.
4. Create a stable tag such as `v0.1.1`.
5. Publish the GitHub release.

F-Droid can then detect tagged versions and its metadata can be updated automatically when the tag/version pattern remains consistent.

## Signing

The GitHub release signing key and the F-Droid repository signing key are separate by default. Losing your GitHub release keystore prevents seamless updates for users who installed the GitHub-signed APK, so keep that keystore backed up securely and never commit it to Git.
