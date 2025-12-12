#!/usr/bin/env bash

# Arc Repository
cd Arc || (echo "Arc directory not found" && exit 1)
base=$(git log --grep "#PATCH-BASE#" --format=reference | awk '{print $1}')
echo "Arc_BASE=$base"
rm -rf ../patches/arc/*
git format-patch --full-index --no-signature --zero-commit -N --ignore-blank-lines -o ../patches/arc "$base..."
cd .. && git add patches/arc

# Main Repository
cd work || (echo "work directory not found" && exit 1)
base=$(git log --grep "#PATCH-BASE#" --format=reference | awk '{print $1}')
picked=$(git log --grep "#END-PICKED#" --format=reference | awk '{print $1}')
echo "BASE=$base,PICKED=$picked"

rm -rf ../patches/picked/*
git format-patch --full-index --no-signature --zero-commit -N --ignore-blank-lines -o ../patches/picked "$base...$picked^"
rm -rf ../patches/client/*
git format-patch --full-index --no-signature --zero-commit -N --ignore-blank-lines -o ../patches/client "$picked..."
cd .. && git add patches