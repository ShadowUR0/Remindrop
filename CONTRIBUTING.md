# Contributing

Contributions are welcome, including AI-assisted contributions.

Please keep Remindrop aligned with its core goals:

- lightweight native Android code
- no trackers, ads, analytics, or telemetry
- no mandatory account or backend
- no Internet permission unless the project's privacy model is explicitly reconsidered
- minimal dependency surface
- battery-friendly scheduling without polling or permanent background services
- accessible Material 3 UI
- Android 7.0+ compatibility

## AI-assisted contributions

Using AI tools to help write, review, translate, test, or refactor code is allowed. The contributor remains responsible for everything submitted.

Before opening an AI-assisted pull request:

- Read the README, this file, and the existing implementation before making changes
- Review and understand all AI-generated code before committing it
- Keep changes focused; do not include unrelated rewrites, formatting passes, or speculative architecture changes
- Preserve Remindrop's local-only privacy model: do not add Internet access, cloud services, accounts, analytics, ads, trackers, telemetry, Firebase, or background polling/services unless the change has been explicitly discussed and approved
- Do not add new permissions or dependencies unless they are necessary; explain each new one in the pull request
- Prefer existing project patterns and standard Android/Kotlin APIs over introducing new frameworks
- Preserve the package ID, minimum Android version, signing/release setup, and F-Droid compatibility unless the requested change specifically requires modifying them
- Put user-visible text in Android string resources rather than hardcoding it, and update relevant localizations when changing existing UI text
- Never include secrets, signing keys, keystores, credentials, tokens, private data, or generated local environment files
- Verify the final result yourself; do not rely on an AI tool claiming that the project builds or tests pass

Before submitting a change, run the same core verification used by CI:

```bash
gradle --no-daemon --stacktrace :app:assembleDebug :app:assembleRelease :app:lintDebug :app:lintRelease
```

Keep pull requests focused and explain any behavior change, new permission, or new dependency clearly. If AI assistance was substantial, a short note in the pull request is encouraged so reviewers know which areas deserve extra attention.
