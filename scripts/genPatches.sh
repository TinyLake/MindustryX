#!/usr/bin/env bash

set -euo pipefail

normalize_patch_dir() {
    local dir="$1"
    find "$dir" -type f -name '*.patch' -print0 | while IFS= read -r -d '' patch; do
        perl -0pi -e 's/\r\n/\n/g; s/\r/\n/g; $_ .= "\n" if length($_) && substr($_, -1) ne "\n";' "$patch"
    done
}

# Arc Repository
cd Arc || (echo "Arc directory not found" && exit 1)
base=$(git log --grep "#PATCH-BASE#" --format=reference | awk '{print $1}')
echo "Arc_BASE=$base"
rm -rf ../patches/arc/*
git format-patch --full-index --no-signature --zero-commit -N --ignore-blank-lines -o ../patches/arc "$base..."
normalize_patch_dir ../patches/arc
cd .. && git add patches/arc

# Main Repository
cd work || (echo "work directory not found" && exit 1)
base=$(git log --grep "#PATCH-BASE#" --format=reference | awk '{print $1}')
picked=$(git log --grep "#END-PICKED#" --format=reference | awk '{print $1}')
echo "BASE=$base,PICKED=$picked"

rm -rf ../patches/picked/*
git format-patch --full-index --no-signature --zero-commit -N --ignore-blank-lines -o ../patches/picked "$base...$picked^"
normalize_patch_dir ../patches/picked
rm -rf ../patches/client/*
git format-patch --full-index --no-signature --zero-commit -N --ignore-blank-lines -o ../patches/client "$picked..."
normalize_patch_dir ../patches/client
cd .. && git add patches
