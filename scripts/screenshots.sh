#!/usr/bin/env bash
# Captures README screenshots of the Wear OS sample on a running Wear OS emulator (large round).
#
# Every screenshot shows LIVE data: the sample opens one screen from the `scene` extra and looks its words up in
# api.dictionaryapi.dev with no cache, exactly like a normal launch. There are no recorded responses to fall back on.
# Each scene logs SCENE_READY once its live entry is on screen; SCENE_FAILED (offline, an HTTP error, "No
# Definitions Found") or a timeout fails the run, so CI goes red instead of committing an error screen. Every
# capture must also be the resumed activity, show the scene's text and pass the blank image check.
set -euo pipefail
source "$(dirname "$0")/screenshot-lib.sh"

# Text each scene shows once loaded; none of it appears on the loading or error screens.
expected_text() {
  case "$1" in
    lookup) echo "Suggested words" ;;
    word) echo "serendipity" ;;
    meanings) echo "light · 1 of" ;;
    synonyms) echo "of happy" ;;
  esac
}

install_sample
for scene in lookup word meanings synonyms; do
  fresh_launch --es scene "$scene"
  wait_for_scene "$scene"
  capture "wear-$scene" "$(expected_text "$scene")"
  # The Free Dictionary API is a free community service: space the scenes out a little.
  sleep 3
done
