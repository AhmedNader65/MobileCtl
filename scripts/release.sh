#!/bin/bash
# Helper script to create a new release
set -e

echo "🚀 mobilectl Release Helper"
echo ""

# Check if we're on master/main
BRANCH=$(git branch --show-current)
if [[ "$BRANCH" != "master" && "$BRANCH" != "main" ]]; then
    echo "⚠️  Warning: You're not on master/main branch (current: $BRANCH)"
    read -p "Continue anyway? (y/N) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi

# Check for uncommitted changes
if [[ -n $(git status -s) ]]; then
    echo "⚠️  You have uncommitted changes:"
    git status -s
    echo ""
    read -p "Commit these changes? (y/N) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        read -p "Commit message: " COMMIT_MSG
        git add .
        git commit -m "$COMMIT_MSG"
    else
        echo "❌ Please commit or stash your changes first"
        exit 1
    fi
fi

# Get current version
CURRENT_VERSION=$(git describe --tags --abbrev=0 2>/dev/null || echo "v0.0.0")
echo "📌 Current version: $CURRENT_VERSION"
echo ""

# Suggest next version
MAJOR=$(echo $CURRENT_VERSION | cut -d'.' -f1 | tr -d 'v')
MINOR=$(echo $CURRENT_VERSION | cut -d'.' -f2)
PATCH=$(echo $CURRENT_VERSION | cut -d'.' -f3)

NEXT_PATCH="v$MAJOR.$MINOR.$((PATCH + 1))"
NEXT_MINOR="v$MAJOR.$((MINOR + 1)).0"
NEXT_MAJOR="v$((MAJOR + 1)).0.0"

echo "Suggested versions:"
echo "  1) $NEXT_PATCH (patch - bug fixes)"
echo "  2) $NEXT_MINOR (minor - new features)"
echo "  3) $NEXT_MAJOR (major - breaking changes)"
echo "  4) Custom version"
echo ""

read -p "Select version (1-4): " CHOICE

case $CHOICE in
    1) NEW_VERSION=$NEXT_PATCH ;;
    2) NEW_VERSION=$NEXT_MINOR ;;
    3) NEW_VERSION=$NEXT_MAJOR ;;
    4)
        read -p "Enter version (e.g., v1.2.3): " NEW_VERSION
        # Validate format
        if [[ ! $NEW_VERSION =~ ^v[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
            echo "❌ Invalid version format. Use: v1.2.3"
            exit 1
        fi
        ;;
    *)
        echo "❌ Invalid choice"
        exit 1
        ;;
esac

echo ""
echo "📝 Release notes:"
read -p "Enter release description (or press Enter to use default): " RELEASE_NOTES

if [ -z "$RELEASE_NOTES" ]; then
    RELEASE_NOTES="Release $NEW_VERSION"
fi

echo ""
echo "Summary:"
echo "  Version: $NEW_VERSION"
echo "  Description: $RELEASE_NOTES"
echo ""

read -p "Create release? (y/N) " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "❌ Release cancelled"
    exit 1
fi

# Create and push tag
echo ""
echo "📦 Creating tag..."
git tag -a "$NEW_VERSION" -m "$RELEASE_NOTES"

echo "⬆️  Pushing to GitHub..."
git push origin "$BRANCH"
git push origin "$NEW_VERSION"

echo ""
echo "✅ Release $NEW_VERSION created!"
echo ""
echo "🔄 GitHub Actions is now building the release..."
echo "📍 Check progress: https://github.com/AhmedNader65/MobileCtl/actions"
echo "📦 View releases: https://github.com/AhmedNader65/MobileCtl/releases"
echo ""
echo "The release will be available in a few minutes with:"
echo "  - mobilectl-linux.tar.gz"
echo "  - mobilectl-macos.tar.gz"
echo "  - mobilectl-windows.zip"
