# X Hub

A system app for custom ROMs that tells users about the maintainer, the ROM, the
build and where to get support. It is compiled from source together with the ROM,
and most of what it shows is found automatically.

## Features
- Dolby-style Material 3 home screen with a living liquid orb that follows the build type
- Build status picked up automatically: **Official**, **Unofficial** or **Personal**
  (personal builds show the same full set of info, plus who the build is for)
- **About the ROM** read from the ROM's own README, with its Telegram channel,
  discussion group, GitHub and website
- Device, Android version, security patch, build date and build ID read from the system
- Changelog and "update available" chip taken from your GitHub releases or commits
- Next update ETA, links and about text you can change on GitHub without rebuilding the ROM
- Spring animations everywhere: cards, buttons, orb and the floating navigation
- Liquid glass floating navigation (blur and lens refraction on Android 13+, frosted glass on older versions)
- Fields left empty show "will be updated soon" and empty links are hidden

## What is automatic
| Info | Where it comes from |
| --- | --- |
| ROM about text | First paragraph of the ROM README (GitHub API) |
| ROM Telegram / Discord | Links in the ROM README, or the ones you give at setup |
| Device, Android, patch, build ID | Android system |
| Build type and "built for" | Set at `envsetup` and baked into the ROM |
| Changelog and update check | Latest release, or recent commits, of your changelog repo |
| Everything else | `apps/XHub/assets/info.json`, refreshed from this repo |

Data is cached for 6 hours, so the app works offline and stays within GitHub's rate limit.

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
3. Set up the environment. You will be asked for the build info and the ROM links.
   Press Enter to keep a value, `-` to clear it, or `s` to skip everything:
   ```
   . build/envsetup.sh
   ```
   If the questions do not appear, run:
   ```
   . vendor/maxxcodebug/build/xhub_setup.sh && xhub_setup
   ```
4. Build the ROM as usual. The `XHub` package is added automatically.

## Works with any ROM
Nothing is tied to one ROM. At setup you give the ROM's GitHub repo and, if you like,
its channel, group and website. Everything else is read from the README.

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

## Requirements
- Android 13 or newer for the glass refraction shader (older versions still work, without refraction)
- `androidx.dynamicanimation` and Material Components in your tree's prebuilts.
  If your tree names the library differently, adjust `static_libs` in `apps/XHub/Android.bp`.

## SELinux note
If the app always shows "Unofficial", add this line to your sepolicy `property_contexts`:
```
ro.anshumanx. u:object_r:system_prop:s0
```

## Maintainer
Anshuman X - [GitHub](https://github.com/maxxcodebug) - [Telegram](https://t.me/AnshumanAhirwar) - [Channel](https://t.me/otbyramen) - [Support chat](https://t.me/suppportgrop)
