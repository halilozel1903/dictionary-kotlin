#!/usr/bin/env bash
# Captures README screenshots of the Wear OS sample on a running Wear OS emulator (large round).
#
# Every screenshot shows LIVE data: the sample opens one screen from the `scene` extra and looks its words up with
# no cache, exactly like a normal launch. There are no recorded responses to fall back on. DICTIONARY_SOURCE picks
# the live source: `freedictionary` (api.dictionaryapi.dev, the default) or `wiktionary` (Wiktionary's REST API).
# CI sets it to the source that answered its pre-check.
# Each scene logs SCENE_READY once its live entry is on screen; SCENE_FAILED (offline, an HTTP error, "No
# Definitions Found") or a timeout fails the run, so CI goes red instead of committing an error screen. Every
# capture must also be the resumed activity, show the scene's text and pass the blank image check.
set -euo pipefail
source "$(dirname "$0")/screenshot-lib.sh"

DICTIONARY_SOURCE="${DICTIONARY_SOURCE:-freedictionary}"
echo "Live source: $DICTIONARY_SOURCE"

# Text each scene shows once loaded; none of it appears on the loading or error screens.
expected_text() {
  case "$1" in
    lookup) echo "Suggested words" ;;
    word) echo "serendipity" ;;
    meanings) echo "light · 1 of" ;;
  esac
}

install_sample
for scene in lookup word meanings; do
  fresh_launch --es scene "$scene" --es source "$DICTIONARY_SOURCE"
  wait_for_scene "$scene"
  capture "wear-$scene" "$(expected_text "$scene")"
  # The Free Dictionary API is a free community service: space the scenes out a little.
  sleep 3
done
