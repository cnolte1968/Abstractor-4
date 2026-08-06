#!/bin/bash
# git_post_ui_push_health_gate.sh

set -euo pipefail

echo "=== Post-Push Git Health Gate ==="

# 1. Ensure we are in the correct repository root
PROJECT_ROOT=$(git rev-parse --show-toplevel)
if [ "$(pwd -P)" != "$PROJECT_ROOT" ]; then
    echo "ERROR: Must be run from the repository root ($PROJECT_ROOT)."
    exit 1
fi

# 2. Get local HEAD
LOCAL_HEAD=$(git rev-parse HEAD)
echo "Local HEAD: $LOCAL_HEAD"

# 3. Get remote HEAD
REMOTE_SHA=$(git ls-remote origin refs/heads/main | awk '{print $1}')
if [ -z "$REMOTE_SHA" ]; then
    echo "ERROR: Could not read remote HEAD."
    exit 1
fi
echo "Remote HEAD: $REMOTE_SHA"

# 4. If identical, exit with PASS
if [ "$LOCAL_HEAD" = "$REMOTE_SHA" ]; then
    echo "STATUS: POST-PUSH-GIT-HEALTH PASS / ALREADY SYNCHRONIZED"
    exit 0
fi

echo "HEAD mismatch detected. Fetching origin/main..."

# 5. Fetch origin main
git fetch --no-tags origin main

ORIGIN_MAIN=$(git rev-parse origin/main)
if [ "$ORIGIN_MAIN" != "$REMOTE_SHA" ]; then
    echo "ERROR: origin/main ($ORIGIN_MAIN) does not match remote HEAD ($REMOTE_SHA)."
    exit 1
fi

echo "Verifying workspace files against origin/main..."

# 6. Verify workspace files
# Get all tracked files in origin/main
TRACKED_FILES=$(git ls-tree -r --name-only origin/main)

MISMATCH=0
MISMATCH_LIST=""

for file in $TRACKED_FILES; do
    if [ ! -f "$file" ]; then
        echo "MISMATCH: Tracked file missing in workspace: $file"
        MISMATCH=1
        MISMATCH_LIST="$MISMATCH_LIST\n$file (missing)"
        continue
    fi
    
    # Compare object hash of workspace file with git hash
    WS_HASH=$(git hash-object "$file")
    TREE_HASH=$(git ls-tree origin/main "$file" | awk '{print $3}')
    
    if [ "$WS_HASH" != "$TREE_HASH" ]; then
        echo "MISMATCH: Tracked file content differs: $file"
        MISMATCH=1
        MISMATCH_LIST="$MISMATCH_LIST\n$file (content diff)"
    fi
done

if [ "$MISMATCH" -eq 1 ]; then
    echo -e "\nERROR: Workspace does not match origin/main perfectly."
    echo -e "Mismatched files:$MISMATCH_LIST"
    echo "STOP."
    exit 1
fi

echo "Workspace matches origin/main perfectly. Proceeding with mixed reset..."

# 7. Perform reset
git reset --mixed origin/main

# 8. Post-reset checks
NEW_HEAD=$(git rev-parse HEAD)
if [ "$NEW_HEAD" != "$REMOTE_SHA" ]; then
    echo "ERROR: Reset failed. Local HEAD is $NEW_HEAD, expected $REMOTE_SHA."
    exit 1
fi

echo "Running fsck..."
if ! git fsck --full --strict; then
    echo "ERROR: git fsck failed after reset."
    exit 1
fi

echo "Showing current status (expecting only untracked build/cache files):"
git status --short

echo "STATUS: POST-PUSH-GIT-HEALTH PASS / SYNCHRONIZED"
exit 0
