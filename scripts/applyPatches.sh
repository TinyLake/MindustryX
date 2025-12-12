#!/usr/bin/env bash

export GIT_COMMITTER_DATE='2024-01-01 00:00:00 +0000' # fix git am date

function getRef() {
    git ls-tree "$1" "$2" | cut -d' ' -f3 | cut -f1
}

function commit_marker() {
    msg=$1
    GIT_AUTHOR_DATE='2024-01-01 00:00:00 +0000' \
    git commit --allow-empty --no-author --no-gpg-sign --no-date -m $msg --author="System <system@example.com>"
}

# Arc Repository
base=$(getRef HEAD Arc || exit 1)
cd Arc || (echo "Arc directory not found" && exit 1)
git reset --hard "$base" || (git fetch origin $base && git reset --hard "$base")

commit_marker "#PATCH-BASE#"
git am --no-gpg-sign -3 ../patches/arc/*

echo "Arc Now at $(git rev-parse HEAD)" && cd ..
echo qc"====== End Patch Arc ======"


# Main Repository
base=$(getRef HEAD work || exit 1)
cd work || (echo "work directory not found" && exit 1)
git reset --hard "$base" || (git fetch origin $base && git reset --hard "$base")

commit_marker "#PATCH-BASE#"
git am --no-gpg-sign -3 ../patches/picked/*
commit_marker "#END-PICKED#"
git am --no-gpg-sign -3 ../patches/client/*

echo "Now at $(git rev-parse HEAD)" && cd ..