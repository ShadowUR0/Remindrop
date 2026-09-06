# Contributing

Contributions are welcome.

Please keep Remindrop aligned with its core goals:

- lightweight native Android code
- no trackers, ads, analytics, or telemetry
- no mandatory account or backend
- no Internet permission unless the project's privacy model is explicitly reconsidered
- minimal dependency surface
- battery-friendly scheduling without polling or permanent background services
- accessible Material 3 UI
- Android 7.0+ compatibility

Before submitting a change, build the app with:

```bash
gradle --no-daemon :app:assembleDebug
```

Keep pull requests focused and explain any new permission or dependency clearly.
