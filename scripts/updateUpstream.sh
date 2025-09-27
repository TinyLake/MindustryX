#!/usr/bin/env bash

function getRef() {
    git ls-tree "$1" "$2" | cut -d' ' -f3 | cut -f1
}

upstream="$1"
upstreamRef="$2"
refHEAD=$(cd .. && getRef HEAD work || exit 1)

git fetch "$upstream" "$upstreamRef" || exit 1
refRemote=$(git rev-parse FETCH_HEAD || exit 1)

if [ "$refHEAD" != "$refRemote" ]; then
  echo "$refHEAD -> $refRemote"
  git reset --hard FETCH_HEAD || (echo "Fail reset" && exit 1)
  (cd .. && git add --force work && git commit -m "Update HEAD -> $upstreamRef($refRemote)")
  echo "Rebuilding patches"
  ../scripts/applyPatches.sh
  echo "After resolve conflict, run '../scripts/genPatches.sh'"
else
  echo "No update, revert to work"
  git reset --hard work
fi