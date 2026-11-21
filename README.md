# Volume-buttons-

A tiny Android app for phones whose volume keys don't work.

A small see-through bubble rests on the side of the screen, over every app. Tap it and the phone's own volume controls appear, the same ones the volume keys bring up: call volume during a call, media volume while something plays. Drag the bubble to move it; it settles on the nearer side and stays there.

It uses no libraries and asks for no permissions; the APK is about 32 KB.

## Build and install

```
./gradlew assembleRelease
adb install -r app/build/outputs/apk/release/app-release.apk
```

The release build is signed with the debug key, so it installs over a debug build.

## Turn it on

Open **Volume Buttons**, tap **Turn on**, find Volume Buttons in the list (it may be under *Downloaded apps* or *Installed services*) and switch it on.

The bubble runs as an accessibility service. That way Android keeps it running and starts it again after a restart. It also lets the bubble draw over other apps without the "Display over other apps" permission, which Android Go phones don't have. The service reads no screen content and receives no events.

## What it survives

Tested on a vivo Y15s (Android 12 Go):

| Event | Bubble |
|---|---|
| Phone restart | Comes back by itself about 3 s after unlocking |
| App crash | Android restarts it |
| Memory cleaners, deep sleep (Doze), battery saver | Keeps running; Android ranks it like a visible app |
| App update | Comes back by itself within about 2 s |
| Screen off for minutes | Keeps running, **once vivo is told not to restrict it** (below) |
| Settings opened fresh, or another app's permission pop-up | vivo switches all accessibility services off; the app switches the bubble back on 1–3 s later, over whatever app is open |
| **Force stop** | **Switched off** by Android: open the app to bring it back |

### vivo: don't let it be restricted

With the default "Smart control", vivo force-stops the app while the screen is off or while i Manager cleans up. Set:

**Settings → Battery → Background power consumption management → Volume Buttons → Don't restrict background power usage**

The bubble uses no measurable battery in the background (0.0 mAh a day there).

### Permission granted over USB

Grant this once:

```
adb shell pm grant com.rasel.volumebuttons android.permission.WRITE_SECURE_SETTINGS
```

It lets the app switch its own service back on:

- about 2 s after the phone switches it off (vivo does this when Settings opens fresh, or when a permission pop-up shows). If it is switched off again within 15 s, the app waits 30 s before trying again rather than fighting;
- after a restart or an app update, if the phone left it off;
- when the app is opened, for example after a force stop.

The grant stays through updates but not through uninstalling. Without it, the bubble still works, but must be turned back on by hand in Accessibility settings. To switch the bubble off for good, force-stop or uninstall the app.
