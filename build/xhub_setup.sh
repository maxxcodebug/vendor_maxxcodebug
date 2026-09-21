# Copyright (c) 2026 Anshuman X (maxxcodebug). All rights reserved.
# X Hub build-info setup. Runs when you do: . build/envsetup.sh
#  - Enter        = keep the suggested/previous value (or skip if empty)
#  - -            = clear the field (skip it)
#  - s at start   = skip ALL questions and reuse saved answers
# Skipped fields show "will be updated soon" in the app (links are hidden).
# Re-run any time with:  xhub_setup

_XHUB_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
_XHUB_ASSET="$_XHUB_DIR/apps/XHub/assets/info.json"
_XHUB_SAVED="${XDG_CONFIG_HOME:-$HOME/.config}/xhub/answers.sh"

_xhub_ask() { # var label
    local var="$1" label="$2" def ans
    def="${!var}"
    if [ -n "$def" ]; then
        read -r -p "  $label [$def]: " ans
    else
        read -r -p "  $label (Enter = skip): " ans
    fi
    if [ "$ans" = "-" ]; then ans=""; elif [ -z "$ans" ]; then ans="$def"; fi
    printf -v "$var" '%s' "$ans"
}

xhub_setup() {
    # ---- defaults you already told me ----
    XH_MAINTAINER="Anshuman X"
    XH_TELEGRAM="https://t.me/AnshumanAhirwar"
    XH_SUPPORT="https://t.me/suppportgrop"
    XH_CHANNEL="https://t.me/otbyramen"
    XH_GITHUB="https://github.com/maxxcodebug"
    XH_ROM_NAME= XH_ABOUT= XH_ROM_GH= XH_APP_GH= XH_DISCORD= XH_DONATE=
    XH_ETA= XH_CHANGELOG= XH_TYPE=UNOFFICIAL XH_FOR=

    # ---- previous answers ----
    [ -f "$_XHUB_SAVED" ] && . "$_XHUB_SAVED"

    # ---- auto-detect what we can ----
    if [ -z "$XH_APP_GH" ] && [ -d "$_XHUB_DIR/.git" ]; then
        local u; u="$(git -C "$_XHUB_DIR" remote get-url origin 2>/dev/null)"
        u="${u%.git}"; u="${u/git@github.com:/https://github.com/}"
        XH_APP_GH="$u"
    fi

    # ---- non-interactive shells: just use saved/defaults ----
    if [ ! -t 0 ]; then
        _xhub_apply; return 0
    fi

    echo
    echo "== X Hub build info =="
    read -r -p "Press Enter to review, or 's' to skip all: " _go
    if [ "$_go" != "s" ] && [ "$_go" != "S" ]; then
        _xhub_ask XH_MAINTAINER "Maintainer name"
        _xhub_ask XH_ROM_NAME   "ROM name"
        _xhub_ask XH_TYPE       "Build type (OFFICIAL / UNOFFICIAL / PERSONAL)"
        XH_TYPE="$(echo "${XH_TYPE:-UNOFFICIAL}" | tr '[:lower:]' '[:upper:]')"
        case "$XH_TYPE" in OFFICIAL|UNOFFICIAL|PERSONAL) ;; *) XH_TYPE=UNOFFICIAL ;; esac
        if [ "$XH_TYPE" = "PERSONAL" ]; then
            _xhub_ask XH_FOR "Who is this personal build for"
        else
            XH_FOR=""
        fi
        _xhub_ask XH_ETA        "Next update ETA (e.g. 5 Oct 2026)"
        _xhub_ask XH_CHANGELOG  "Changelog for this build (comma separated)"
        _xhub_ask XH_ABOUT      "About you (one line)"
        _xhub_ask XH_TELEGRAM   "Telegram link"
        _xhub_ask XH_SUPPORT    "Support group link"
        _xhub_ask XH_CHANNEL    "Channel link"
        _xhub_ask XH_GITHUB     "GitHub profile"
        _xhub_ask XH_ROM_GH     "ROM GitHub repo"
        _xhub_ask XH_APP_GH     "App GitHub repo"
        _xhub_ask XH_DISCORD    "Discord link"
        _xhub_ask XH_DONATE     "Donation link"
    fi
    _xhub_apply
}

_xhub_apply() {
    # save answers for next time
    mkdir -p "$(dirname "$_XHUB_SAVED")"
    : > "$_XHUB_SAVED"
    local v
    for v in XH_MAINTAINER XH_ROM_NAME XH_TYPE XH_FOR XH_ETA XH_CHANGELOG XH_ABOUT \
             XH_TELEGRAM XH_SUPPORT XH_CHANNEL XH_GITHUB XH_ROM_GH XH_APP_GH XH_DISCORD XH_DONATE; do
        printf '%s=%q\n' "$v" "${!v}" >> "$_XHUB_SAVED"
    done

    # values the ROM makefile reads (vendor/maxxcodebug/maxxcodebug.mk)
    export ANSHUMANX_BUILD_TYPE="$XH_TYPE"
    export ANSHUMANX_BUILD_FOR="$XH_FOR"

    # generate the JSON bundled inside the app
    export XH_MAINTAINER XH_ROM_NAME XH_ETA XH_CHANGELOG XH_ABOUT XH_TELEGRAM XH_SUPPORT \
           XH_CHANNEL XH_GITHUB XH_ROM_GH XH_APP_GH XH_DISCORD XH_DONATE
    python3 - "$_XHUB_ASSET" <<'PY'
import json, os, sys, datetime
e = os.environ.get
links = []
for label, key in [("Telegram (me)", "XH_TELEGRAM"),
                   ("Support chat", "XH_SUPPORT"),
                   ("Channel", "XH_CHANNEL"),
                   ("GitHub", "XH_GITHUB"),
                   ("ROM source", "XH_ROM_GH"),
                   ("App source", "XH_APP_GH"),
                   ("Discord", "XH_DISCORD")]:
    if e(key):
        links.append({"label": label, "url": e(key)})
if e("XH_DONATE"):
    links.append({"label": "Support me", "url": e("XH_DONATE"), "primary": True})

data = {"maintainer": e("XH_MAINTAINER", ""),
        "rom_name": e("XH_ROM_NAME", ""),
        "about": e("XH_ABOUT", ""),
        "next_update_eta": e("XH_ETA", ""),
        "links": links,
        "changelog": []}
changes = [c.strip() for c in e("XH_CHANGELOG", "").split(",") if c.strip()]
if changes:
    data["changelog"].append({"version": "This build",
                              "date": datetime.date.today().isoformat(),
                              "changes": changes})
with open(sys.argv[1], "w") as f:
    json.dump(data, f, indent=2, ensure_ascii=False)
PY
    echo "X Hub: build type = $XH_TYPE${XH_FOR:+ (for $XH_FOR)}"
}
