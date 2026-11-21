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

## If the bubble goes away (vivo and similar phones)

- Settings → Battery → Background power consumption management → Volume Buttons → allow.
- Don't force-stop the app. A force stop makes Android switch the service off, and it must be turned on again.
