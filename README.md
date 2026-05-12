# Explit App

Android Studio Java expense-splitting app (`com.example.explit`) with:
- Group/category management (Family/Friends/Business)
- Participants, receipts, expense items, and shared item flags
- SQLite persistence via `SQLiteOpenHelper`
- Item assignments and proportional tax/tip/service-charge calculations
- Settlement summary with minimized transactions
- Event history reopen/edit
- Currency selection (₱ or $)
- Receipt photo URI/path persistence
- Share summary via Android share intent

## Build

```bash
./gradlew test
```

Open in Android Studio and run on API 28+.
