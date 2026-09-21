# X Hub

A system app for custom ROMs that tells users about the maintainer, the build
and where to get support. It is compiled from source together with the ROM.

## Features
- About the maintainer, ROM name, device, Android version and build date
- Build status badge that is picked up automatically: **Official**, **Unofficial** or **Personal**
- Personal builds show who the build was made for
- Changelog and next update ETA
- Links: Telegram, support chat, channel, GitHub, ROM source, app source, Discord, donation
- Material 3 design with dynamic colors and a bold collapsing header
- Fields left empty show "will be updated soon" (empty links are hidden)

## Repository layout
```
apps/XHub/            the app (Android.bp, manifest, res, assets, Kotlin source)
build/xhub_setup.sh   asks for build info when you set up the environment
vendorsetup.sh        auto-runs the setup from build/envsetup.sh
maxxcodebug.mk        adds the app and the build-type properties to the ROM
```

## Add to your ROM tree
1. Clone into your tree:
   ```
   git clone https://github.com/maxxcodebug/vendor_maxxcodebug vendor/maxxcodebug
   ```
2. Include the makefile in your device or vendor makefile:
   ```
   $(call inherit-product, vendor/maxxcodebug/maxxcodebug.mk)
   ```
3. Set up the environment. You will be asked for the build info.
   Press Enter to keep a value, `-` to clear it, or `s` to skip everything:
   ```
   . build/envsetup.sh
   ```
   If the questions do not appear, run:
   ```
   . vendor/maxxcodebug/build/xhub_setup.sh && xhub_setup
   ```
4. Build the ROM as usual. The `XHub` package is added automatically.

## Build type
Set by the setup script, or manually before building:
```
export ANSHUMANX_BUILD_TYPE=OFFICIAL   # OFFICIAL / UNOFFICIAL / PERSONAL
export ANSHUMANX_BUILD_FOR="Name"      # personal builds only
```

## Update info without rebuilding
Edit `apps/XHub/assets/info.json` in this repo. The app fetches it from GitHub
and falls back to the copy bundled in the ROM when offline. To add a link, add
one line to the `links` list:
```
{ "label": "Instagram", "url": "https://instagram.com/..." }
```

## Design
Material 3 with dynamic colors. If your tree ships Material Components 1.14 or
newer, change the theme parent in `res/values/themes.xml` to
`Theme.Material3Expressive.DayNight.NoActionBar` for the Expressive look.

## SELinux note
If the app always shows "Unofficial", add this line to your sepolicy `property_contexts`:
```
ro.anshumanx. u:object_r:system_prop:s0
```

## Maintainer
Anshuman X - [GitHub](https://github.com/maxxcodebug) - [Telegram](https://t.me/AnshumanAhirwar) - [Channel](https://t.me/otbyramen) - [Support chat](https://t.me/suppportgrop)

---
Copyright © 2026 Anshuman X (maxxcodebug). All rights reserved.
