#!/usr/bin/env bash
# Prints the notes of one version from CHANGELOG.md, the text of its GitHub Release.
#   tools/release-notes.sh 1.0.0          the section "## [1.0.0] · <date>"
#   tools/release-notes.sh unreleased     the section "## Sin publicar" (to preview the next release)
# Usage in a release: gh release create v1.0.0 sobre-io-1.0.0.apk --notes "$(tools/release-notes.sh 1.0.0)"
set -euo pipefail

version="${1:?usage: tools/release-notes.sh <version>|unreleased}"
changelog="$(dirname "$0")/../CHANGELOG.md"

if [[ "$version" == "unreleased" ]]; then
  heading="## Sin publicar"
else
  heading="## [$version]"
fi

notes="$(awk -v heading="$heading" '
  index($0, heading) == 1 { found = 1; next }
  found && /^## / { exit }
  found { print }
' "$changelog" | sed -e '/./,$!d')"

if [[ -z "${notes//[[:space:]]/}" ]]; then
  echo "No notes for '$version' in CHANGELOG.md" >&2
  exit 1
fi
printf '%s\n' "$notes"
