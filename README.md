# Mouse Mapper (Android 14)

A native Android 14 app that configures per-app mouse-to-touch profiles for left/right/middle click, X1/X2, and vertical wheel up/down. Each input can be disabled or mapped to a tap, repeated tap, or swipe at percentage-based screen coordinates.

## Run

Open this project in Android Studio, install it on an Android 14 device, open **Mouse Mapper**, and enable its accessibility service in Android Settings. Choose `*` for the global profile or enter the target app's package name, configure the controls, and save. Connect a mouse and test in a non-sensitive app first.

## Platform boundaries

This is not a BlueStacks emulator-side input mapper. Android does not grant ordinary apps an equivalent system-wide input hook, and AccessibilityService gestures are subject to Android/device and foreground-app restrictions. Mouse events may still reach the foreground app, resulting in duplicate actions; games may reject accessibility-injected gestures. The implementation therefore does not promise reliable game input or interception, and does not implement BlueStacks-only functionality such as keyboard/gamepad schemes, scripts, aim-lock/free-look, MOBA skill casting, tilt, or custom game control profiles. Accessibility services should be used consistently with Android and app-store accessibility policies.

## BlueStacks documentation reviewed

- [Create and edit controls in BlueStacks 5](https://support.bluestacks.com/hc/en-us/articles/360058928871-How-to-create-and-edit-controls-in-BlueStacks-5): documents tap spot, repeated tap, D-pad, aim/pan/shoot, free look, script, swipe, zoom, tilt, MOBA controls, rotate, edge scroll, scroll, and mouse wheel, plus editing and control schemes.
- [Game controls menu in BlueStacks 5](https://support.bluestacks.com/hc/en-us/articles/360055522292-How-to-use-the-Game-controls-menu-on-BlueStacks-5): documents enable/disable, on-screen display, opacity, schemes, and mouse sensitivity.
- [Android AccessibilityService](https://developer.android.com/reference/android/accessibilityservice/AccessibilityService): describes user-enabled accessibility services, overlay/gesture capabilities, and platform constraints.
- [Android input events](https://developer.android.com/develop/ui/views/touch-and-input/input-events): describes input event delivery to views.
