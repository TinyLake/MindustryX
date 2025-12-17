#!/usr/bin/env bash

set -e

function getRef() {
    git ls-tree "$1" "$2" | cut -d' ' -f3 | cut -f1
}

upstreamRef="$1"

refHEAD_Arc=$(getRef HEAD Arc || exit 1)
cd Arc && git fetch origin "$upstreamRef" || exit 1
refRemote_Arc=$(git rev-parse FETCH_HEAD || exit 1)
cd ..

refHEAD=$(getRef HEAD work || exit 1)
cd work && git fetch origin "$upstreamRef" || exit 1
refRemote=$(git rev-parse FETCH_HEAD || exit 1)
cd ..

# Both No Update, exit
if [ "$refHEAD_Arc" == "$refRemote_Arc" ] && [ "$refHEAD" == "$refRemote" ]; then
  echo "No update in both Arc and work"
  exit
fi

echo "Arc $refHEAD_Arc -> $refRemote_Arc"
echo "Work $refHEAD -> $refRemote"
(cd Arc && git reset --hard FETCH_HEAD || (echo "Fail reset Arc" && exit 1))
(cd work && git reset --hard FETCH_HEAD || (echo "Fail reset work" && exit 1))
git add --force Arc work && git commit -m "Update HEAD -> $upstreamRef($refRemote)"

echo "Rebuilding patches"
./scripts/applyPatches.sh
echo "After resolve conflict, run '../scripts/genPatches.sh'"