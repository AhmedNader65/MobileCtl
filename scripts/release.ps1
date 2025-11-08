# Helper script to create a new release
$ErrorActionPreference = "Stop"

Write-Host "🚀 mobilectl Release Helper" -ForegroundColor Cyan
Write-Host ""

# Check if we're on master/main
$branch = git branch --show-current
if ($branch -ne "master" -and $branch -ne "main") {
    Write-Host "⚠️  Warning: You're not on master/main branch (current: $branch)" -ForegroundColor Yellow
    $continue = Read-Host "Continue anyway? (y/N)"
    if ($continue -ne "y") {
        exit 1
    }
}

# Check for uncommitted changes
$status = git status -s
if ($status) {
    Write-Host "⚠️  You have uncommitted changes:" -ForegroundColor Yellow
    git status -s
    Write-Host ""
    $commit = Read-Host "Commit these changes? (y/N)"
    if ($commit -eq "y") {
        $message = Read-Host "Commit message"
        git add .
        git commit -m $message
    } else {
        Write-Host "❌ Please commit or stash your changes first" -ForegroundColor Red
        exit 1
    }
}

# Get current version
$currentVersion = git describe --tags --abbrev=0 2>$null
if (-not $currentVersion) {
    $currentVersion = "v0.0.0"
}

Write-Host "📌 Current version: $currentVersion" -ForegroundColor Gray
Write-Host ""

# Parse version
$versionMatch = $currentVersion -match 'v(\d+)\.(\d+)\.(\d+)'
if ($versionMatch) {
    $major = [int]$Matches[1]
    $minor = [int]$Matches[2]
    $patch = [int]$Matches[3]
} else {
    $major = 0
    $minor = 0
    $patch = 0
}

# Suggest next version
$nextPatch = "v$major.$minor.$($patch + 1)"
$nextMinor = "v$major.$($minor + 1).0"
$nextMajor = "v$($major + 1).0.0"

Write-Host "Suggested versions:" -ForegroundColor Cyan
Write-Host "  1) $nextPatch (patch - bug fixes)"
Write-Host "  2) $nextMinor (minor - new features)"
Write-Host "  3) $nextMajor (major - breaking changes)"
Write-Host "  4) Custom version"
Write-Host ""

$choice = Read-Host "Select version (1-4)"

switch ($choice) {
    "1" { $newVersion = $nextPatch }
    "2" { $newVersion = $nextMinor }
    "3" { $newVersion = $nextMajor }
    "4" {
        $newVersion = Read-Host "Enter version (e.g., v1.2.3)"
        # Validate format
        if ($newVersion -notmatch '^v\d+\.\d+\.\d+$') {
            Write-Host "❌ Invalid version format. Use: v1.2.3" -ForegroundColor Red
            exit 1
        }
    }
    default {
        Write-Host "❌ Invalid choice" -ForegroundColor Red
        exit 1
    }
}

Write-Host ""
$releaseNotes = Read-Host "Enter release description (or press Enter to use default)"

if (-not $releaseNotes) {
    $releaseNotes = "Release $newVersion"
}

Write-Host ""
Write-Host "Summary:" -ForegroundColor Cyan
Write-Host "  Version: $newVersion"
Write-Host "  Description: $releaseNotes"
Write-Host ""

$confirm = Read-Host "Create release? (y/N)"
if ($confirm -ne "y") {
    Write-Host "❌ Release cancelled" -ForegroundColor Red
    exit 1
}

# Create and push tag
Write-Host ""
Write-Host "📦 Creating tag..." -ForegroundColor Cyan
git tag -a $newVersion -m $releaseNotes

Write-Host "⬆️  Pushing to GitHub..." -ForegroundColor Cyan
git push origin $branch
git push origin $newVersion

Write-Host ""
Write-Host "✅ Release $newVersion created!" -ForegroundColor Green
Write-Host ""
Write-Host "🔄 GitHub Actions is now building the release..." -ForegroundColor Cyan
Write-Host "📍 Check progress: https://github.com/AhmedNader65/MobileCtl/actions" -ForegroundColor Gray
Write-Host "📦 View releases: https://github.com/AhmedNader65/MobileCtl/releases" -ForegroundColor Gray
Write-Host ""
Write-Host "The release will be available in a few minutes with:" -ForegroundColor Cyan
Write-Host "  - mobilectl-linux.tar.gz"
Write-Host "  - mobilectl-macos.tar.gz"
Write-Host "  - mobilectl-windows.zip"
