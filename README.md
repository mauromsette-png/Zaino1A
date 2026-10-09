# Zaino 1A: Android app

Every evening, at a time you choose, the app sends a notification listing **tomorrow's subjects** in hour order. Tapping the notification opens a **checklist** for packing the school bag.

The app is preloaded with the timetable of class 1A, Liceo Scientifico, valid from 5 October 2026. The interface is in Italian.

## What the app does

- **Evening reminder.** The default time is 19:00 and you can change it. The reminder only fires on evenings before a school day: Sunday to Friday, but not Saturday, because Sunday has no lessons.
- **Checklist.** It lists each of the day's subjects once, plus "Diario" and "Astuccio" (both editable). Ticks are saved separately for each day. When everything is ticked, the app shows "Zaino pronto! ✅". The ◀ ▶ buttons move between school days.
- **Impostazioni (settings).** Here you can:
  - turn the reminder on or off and set its time;
  - send a test notification;
  - edit the timetable, one subject per line in hour order;
  - restore the original 1A timetable;
  - edit the list of things to bring every day.
- **Restarts.** The reminder is set again automatically after the phone restarts or the clock changes.
- **Requirements.** Android 8.0 or newer. The app has no ads, needs no internet and uses no external libraries.

## How to get the APK

### Option A: Android Studio (on your computer)

1. Install [Android Studio](https://developer.android.com/studio).
2. Unzip this folder and choose **File → Open**, then select the `Zaino1A` folder. Wait for the Gradle sync to finish.
3. Choose **Build → Build App Bundle(s) / APK(s) → Build APK(s)**.
4. The APK is created at `app/build/outputs/apk/debug/app-debug.apk`.
   - To install it directly, connect the phone by USB with USB debugging turned on and press ▶ Run.

### Option B: GitHub (no software to install)

1. Create a new repository on github.com and upload the contents of this folder, keeping the hidden `.github` folder.
2. Open the **Actions** tab. The "Build APK" job starts by itself and takes about 3–5 minutes.
3. Open the finished run and download **Zaino1A-apk**. It is a zip file containing `app-debug.apk`.

## Installing on the phone

1. Copy `app-debug.apk` to the phone and open it.
2. Android will ask you to allow installing apps from that source (for example Files or Chrome). Allow it.
3. Open **Zaino 1A** and accept the **notifications** permission.
4. Optional: go to Impostazioni and tap **"Consenti sveglie e promemoria"**. This makes the reminder arrive at the exact minute. Without it, the reminder can be a few minutes late.
5. Some phones (Xiaomi, Huawei, Oppo, Samsung) stop apps in the background to save battery. If the reminder doesn't arrive, set the app's battery usage to **"Senza restrizioni" / "Unrestricted"**.

## Limitations

- **School holidays** are not known to the app. On those days you will still get a reminder; turn it off in Impostazioni for the holiday period.
- **Timetable changes:** if the school publishes a new timetable, update it in Impostazioni.
