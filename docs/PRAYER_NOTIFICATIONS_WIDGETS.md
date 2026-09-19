# Prayer notifications & home-screen widgets

Spec for strengthening prayer reminders and adding glanceable widgets.

## Current behavior (as found in source)

- **Scheduling:** `PrayerSettingsController` + `BootCompletedReceiver` /
  `TIMEZONE_CHANGED` / `MY_PACKAGE_REPLACED` reschedule path.
- **Fire:** `PrayerAlarmReceiver` shows a notification; channels:
  - `prayer_adhan_channel_v2` — high importance (“Prayer Adhan”)
  - `prayer_notification_channel_v2` — default (“Prayer Notifications”)
- **Sound policy:** SharedPreferences `reminder_type` (`Notification` vs
  `Alarm`) and `alarm_sound` (e.g. “Mecca Adhan”).
- **Calculation:** `OfflinePrayerCalculator` + Adhan library; location from
  `UserPreferencesRepository`.
- **Tracking DB:** `PrayerDatabase` (`prayer_logs`, `qaza_counts`).
- **Widgets:** `NextPrayerWidgetReceiver` (2×2) + `DayPrayerWidgetReceiver` (4×2) registered; day strip highlights the next prayer.

## Target: shade “ongoing” + adhan channels

Keep adhan alerts distinct from a quiet ongoing status.

| Channel ID | Name | Importance | Use |
|------------|------|------------|-----|
| `prayer_adhan_channel_v2` | Prayer Adhan | HIGH | Exact prayer / pre-adhan alarm |
| `prayer_notification_channel_v2` | Prayer Notifications | DEFAULT | Soft reminders |
| `prayer_ongoing_channel_v1` **(new)** | Prayer status | LOW | Ongoing shade row: next prayer + countdown |

### Ongoing notification

- Not a full-screen intent; silent, updatable.
- Content: next prayer name, local time, countdown; tap → `PrayerTimesScreen`.
- Start when user enables “Show next prayer in shade”; stop on disable /
  logout / location unavailable.
- Must **never** reuse the adhan channel (avoid hijacking ringer behavior).
- When adhan fires, ongoing updates to “now: Maghrib” then advances to next.

### Adhan notification

- Retain HIGH channel + optional full-screen intent only if user opted into
  alarm-style reminders and exact-alarm permission granted.
- Custom adhan URI when packaged; fall back to system alarm/notification URI
  (current code path).

## Home-screen widgets

Register `AppWidgetProvider`s after layouts exist. Suggested sizes:

### 2×2 — Next prayer

- Next prayer name + clock time
- Small countdown (“in 42m”)
- Optional city label
- Tap → open Prayer Times

### 4×2 — Day strip

- Fajr / Dhuhr / Asr / Maghrib / Isha times for today
- Highlight next upcoming
- Optional checkmarks if logged in `prayer_logs`
- Tap → Prayer Times; long-press config for calculation method later

### Implementation sketch

```text
com.noorpro.app.prayer.widget/
  NextPrayerWidgetReceiver.kt      // 2x2
  DayPrayerWidgetReceiver.kt       // 4x2
  PrayerWidgetUpdater.kt           // shared refresh from calculator
res/layout/
  widget_next_prayer_2x2.xml
  widget_day_prayers_4x2.xml
res/xml/
  widget_next_prayer_info.xml      // minResize / targetCell
  widget_day_prayers_info.xml
```

Manifest (when implemented):

```xml
<receiver android:name="...NextPrayerWidgetReceiver" android:exported="false">
  <intent-filter>
    <action android:name="android.appwidget.action.APPWIDGET_UPDATE" />
  </intent-filter>
  <meta-data android:name="android.appwidget.provider"
             android:resource="@xml/widget_next_prayer_info" />
</receiver>
```

Update triggers: periodic `WorkManager` (~15–30 min), exact alarm near prayer,
boot completed, timezone change (already handled for alarms).

## Permissions already declared

`POST_NOTIFICATIONS`, `SCHEDULE_EXACT_ALARM`, `RECEIVE_BOOT_COMPLETED`,
`USE_FULL_SCREEN_INTENT`, location — align runtime prompts with Settings UX.

## Package note

Widget/ongoing code lives under `com.noorpro.app.prayer` (see
`ARCHITECTURE.md` / `docs/PACKAGE_RENAME.md`). Prefer that package for new
feature files.


## Battery optimization

Settings → Advanced includes a **Battery optimization tip** that deep-links to
unrestricted / ignore-battery settings so exact prayer alarms and the ongoing
shade survive OEM Doze. Prefer user-initiated settings intents over silent
whitelisting.
