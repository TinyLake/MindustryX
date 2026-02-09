#!/usr/bin/env bash

set -e

# 定义颜色
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

export GIT_COMMITTER_DATE='2024-01-01 00:00:00 +0000' # fix git am date

# --- Helper Functions ---

function getRef() {
    git ls-tree "$1" "$2" | cut -d' ' -f3 | cut -f1
}

# 检查当前是否处于 git am / rebase 过程中 (兼容 Worktree/Submodule)
function is_rebase_in_progress() {
    # git rev-parse --git-path 会自动处理 .git 是文件还是目录的情况
    if [ -d "$(git rev-parse --git-path rebase-apply)" ] || \
       [ -d "$(git rev-parse --git-path rebase-merge)" ]; then
        return 0 # True, in progress
    else
        return 1 # False
    fi
}

function git_reset() {
  base=$1
  git am --abort || true
  git reset --hard "$base" || (git fetch origin "$base" && git reset --hard "$base")
}

function commit_marker() {
    msg=$1
    echo -e "${YELLOW}Creating marker commit: $msg${NC}"
    GIT_AUTHOR_DATE='2024-01-01 00:00:00 +0000' \
    git commit --allow-empty --no-author --no-gpg-sign --no-date -m "$msg" --author="System <system@example.com>"
}

# 核心优化函数：安全的 git am
function git_am() {
    local patch_path="$1"

    # 检查是否有匹配的补丁文件
    if ! ls $patch_path >/dev/null 2>&1; then
        echo -e "${YELLOW}No patches found matching: $patch_path. Skipping.${NC}"
        return
    fi

    echo -e "${GREEN}Applying patches from: $patch_path${NC}"

    # 尝试应用补丁。这里使用 set +e 临时允许失败，因为我们要捕获它
    set +e
    git am --no-gpg-sign -3 $patch_path
    local status=$?
    set -e

    # 只要还处于 git am 进行中（rebase-apply目录存在），就进入循环
    while is_rebase_in_progress; do
        echo -e "\n${RED}!!! CONFLICT DETECTED !!!${NC}"
        echo -e "You are currently in: ${BLUE}$(pwd)${NC}"
        echo "---------------------------------------------------"
        echo -e "  ${GREEN}[c]${NC} Continue : Run 'git am --continue' (after resolving)"
        echo -e "  ${GREEN}[k]${NC} Skip     : Run 'git am --skip' (discard current patch)"
        echo -e "  ${GREEN}[s]${NC} Shell    : Open a sub-shell to fix conflicts (vim, git status, etc.)"
        echo -e "  ${GREEN}[a]${NC} Abort    : Run 'git am --abort' and stop script"
        echo "---------------------------------------------------"

        read -p "Select action [c/k/s/a]: " choice
        echo ""

        case "$choice" in
            s|S)
                echo -e "${YELLOW}>>> Entering Sub-shell. Type 'exit' to return to menu. <<<\n${NC}"
                # 启动一个子 Shell，修改提示符让用户知道自己在子层级
                PS1="(Fixing Conflict) \u@\h:\w$ " bash --norc
                echo -e "\n${YELLOW}>>> Exited Sub-shell. Back to script menu. <<<${NC}"
                ;;

            c|C)
                echo -e "${YELLOW}Running 'git am --continue'...${NC}"
                set +e
                git am --continue
                local cmd_status=$?
                set -e

                if [ $cmd_status -eq 0 ]; then
                    echo -e "${GREEN}Continue successful!${NC}"
                else
                    echo -e "${RED}Continue failed. Please fix conflicts or skip.${NC}"
                fi
                ;;

            k|K)
                echo -e "${YELLOW}Skipping current patch...${NC}"
                set +e
                git am --skip
                local cmd_status=$?
                set -e

                if [ $cmd_status -eq 0 ]; then
                    echo -e "${GREEN}Patch skipped.${NC}"
                else
                    echo -e "${RED}Skip failed (Check if git am is actually running).${NC}"
                fi
                ;;

            a|A)
                echo -e "${RED}Aborting git am...${NC}"
                git am --abort
                echo "Script aborted by user."
                exit 1
                ;;

            *)
                echo "Invalid input."
                ;;
        esac
    done
}

# --- Arc Repository ---

echo -e "${GREEN}====== Start Patch Arc ======${NC}"

base=$(getRef HEAD Arc)
if [ -z "$base" ]; then echo "${RED}Error: Could not find Arc ref${NC}"; exit 1; fi

cd Arc || (echo "Arc directory not found" && exit 1)

git_reset $base
commit_marker "#PATCH-BASE#"
git_am "../patches/arc/*"

echo "Arc Now at $(git rev-parse HEAD)" && cd ..
echo -e "${GREEN}====== End Patch Arc ======${NC}"


# --- Main Repository ---

echo -e "${GREEN}====== Start Patch Work ======${NC}"

base=$(getRef HEAD work)
if [ -z "$base" ]; then echo "${RED}Error: Could not find work ref${NC}"; exit 1; fi

cd work || (echo "work directory not found" && exit 1)

git_reset $base
commit_marker "#PATCH-BASE#"
git_am "../patches/picked/*"

commit_marker "#END-PICKED#"
git_am "../patches/client/*"

echo "Now at $(git rev-parse HEAD)" && cd ..
echo -e "${GREEN}====== All Done ======${NC}"