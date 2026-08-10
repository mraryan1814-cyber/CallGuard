# CallGuard

Kotlin + Jetpack Compose Android app jo chuninda (2-4) numbers ki calls ko
pehle N baar block/silent karta hai, aur Nth call par ring hone deta hai.

## Kaise khole (Android Studio)

1. Android Studio (Koala ya naya) mein **Open** karke is folder ko select karein.
2. Gradle sync hone dein (internet chahiye first time ke liye).
3. Ek real phone par run karein (emulator par calling behaviour theek se test
   nahi hoti - USB debugging on karke device par install karein).

## Pehli baar app chalane par zaroori steps

1. App **READ_PHONE_STATE** aur (Android 13+) **Notifications** permission
   maangega - allow karein.
2. Ek system dialog aayega jo app ko **"Default Call Screening App"** banane
   ke liye poochega - **Allow/Set as default** karein. Iske bina
   `CallScreeningServiceImpl` ko koi call hi nahi milegi.
   - Agar dialog miss ho jaaye: Settings → Apps → Default apps →
     Caller ID & spam / Call screening app → CallGuard select karein
     (device ke Android version ke hisaab se path thoda alag ho sakta hai).
3. Pehli baar app kholte hi 4-digit PIN set karne ko kahega.

## Core logic (CallGuardEngine.kt)

- Number monitored list mein nahi hai → normal ring (app kuch nahi karta).
- Number whitelist mein hai → hamesha ring.
- Master switch OFF → sab kuch normal ring (blocking disabled).
- Monitored number:
  - Agar last call se gap set kiye gaye time (1/2/5 min) se zyada hai →
    counter 0 se restart.
  - Counter ++ karke agar set kiye gaye count (3/4/5/7) se kam hai → block
    (hard reject ya silent, Settings mein choose kar sakte hain).
  - Counter us count tak pahunch jaaye → us call ko ring hone dein, aur agli
    call ke liye counter wapas 0 kar dein (naya cycle shuru).

## Files ka structure

```
app/src/main/java/com/callguard/app/
  data/        -> Prefs (DataStore settings), CounterStore (per-number state),
                   Room (HistoryEntity/Dao/Database) for call log
  logic/       -> CallGuardEngine.kt (pure decision logic)
  service/     -> CallScreeningServiceImpl (block/allow decision),
                   CallMonitorService (PhoneStateListener/TelephonyCallback
                   foreground service for live call-state tracking)
  receiver/    -> BootReceiver, MidnightResetReceiver (daily auto-reset)
  tile/        -> MasterSwitchTileService (Quick Settings tile)
  util/        -> AlarmScheduler, CsvUtils (backup/export/import)
  ui/          -> Compose screens: PIN, Home, Numbers, Whitelist, Settings,
                   History + Navigation.kt
```

## Forgot PIN

"Forgot PIN" → master recovery code daalein: **bettaua** (sirf alphabets,
case-insensitive) → PIN reset ho jaata hai, phir naya PIN set kar sakte hain.

## Notes

- SMS se koi lena-dena nahi hai, sirf calls handle hoti hain.
- History Room database (`callguard.db`) mein store hoti hai, History screen
  se dekh/clear kar sakte hain.
- Backup Settings screen se CSV export/import hota hai
  (format: `type,number` jahan type MONITORED ya WHITELIST hota hai).
