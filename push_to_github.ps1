# ComicPulse - Auto GitHub Push Script
# This script commits your changes, authenticates with GitHub, creates a remote repo, and pushes your code automatically.

$ErrorActionPreference = "Stop"

Write-Host "--------------------------------------------------" -ForegroundColor Cyan
Write-Host "   ComicPulse - Auto Git Commit & GitHub Push" -ForegroundColor Cyan
Write-Host "--------------------------------------------------" -ForegroundColor Cyan

# 1. Verify Git status
Write-Host "[1/5] Checking Git repository status..." -ForegroundColor Green
if (-not (Test-Path ".git")) {
    & "C:\Program Files\Git\cmd\git.exe" init
}

# Add all files (respecting the premium .gitignore we created)
& "C:\Program Files\Git\cmd\git.exe" add .

# 2. Check Git configuration
Write-Host "[2/5] Checking Git identity..." -ForegroundColor Green
$gitUser = & "C:\Program Files\Git\cmd\git.exe" config user.name
$gitEmail = & "C:\Program Files\Git\cmd\git.exe" config user.email

if ([string]::IsNullOrEmpty($gitUser) -or [string]::IsNullOrEmpty($gitEmail)) {
    Write-Host "Git identity is not configured. Please enter your details:" -ForegroundColor Yellow
    $email = Read-Host "Enter your GitHub email address"
    $name = Read-Host "Enter your GitHub name/username"
    
    & "C:\Program Files\Git\cmd\git.exe" config --global user.email "$email"
    & "C:\Program Files\Git\cmd\git.exe" config --global user.name "$name"
    Write-Host "Git identity configured successfully!" -ForegroundColor Green
}

# 3. Create Commit
Write-Host "[3/5] Committing project files..." -ForegroundColor Green
& "C:\Program Files\Git\cmd\git.exe" commit -m "feat: complete premium ComicPulse offline reader & RAR5 extraction engine"

# 4. Authenticate with GitHub CLI
Write-Host "[4/5] Checking GitHub authentication..." -ForegroundColor Green
try {
    & "C:\Program Files\GitHub CLI\gh.exe" auth status
} catch {
    Write-Host "You are not logged into GitHub CLI." -ForegroundColor Yellow
    Write-Host "Launching GitHub authentication login now... Please follow the prompts." -ForegroundColor Yellow
    & "C:\Program Files\GitHub CLI\gh.exe" auth login
}

# 5. Create Repository and Push
Write-Host "[5/5] Creating GitHub repository and pushing code..." -ForegroundColor Green
try {
    # Check if remote already exists
    $remoteCheck = & "C:\Program Files\Git\cmd\git.exe" remote
    if ($remoteCheck -contains "origin") {
        Write-Host "Remote 'origin' already exists. Pushing..." -ForegroundColor Yellow
        & "C:\Program Files\Git\cmd\git.exe" branch -M main
        & "C:\Program Files\Git\cmd\git.exe" push -u origin main
    } else {
        # Create a new repo and push in one go using GitHub CLI
        & "C:\Program Files\GitHub CLI\gh.exe" repo create ComicPulse --public --source=. --remote=origin --push
    }
    
    Write-Host "`n==================================================" -ForegroundColor Green
    Write-Host " 🎉 SUCCESS! ComicPulse is successfully on GitHub! " -ForegroundColor Green
    Write-Host "==================================================" -ForegroundColor Green
} catch {
    Write-Host "`n[ERROR] Failed to push repository automatically. Please ensure you have configured your GitHub authentication correctly." -ForegroundColor Red
}

Read-Host "`nPress Enter to exit..."
