#!/bin/bash
# Create OS-specific distribution packages for Quizia
# Generates: quizia-ubuntu.zip, quizia-macos.zip, quizia-windows.zip

set -e

VERSION="1.0.0"
BUILD_DIR="/home/nixon-wainaina/NetBeansProjects/quizia/quizia_frontend/app/build/libs"
DIST_BASE="/tmp/quizia-dist-os"
OUTPUT_DIR="/home/nixon-wainaina/NetBeansProjects/quizia/distributions"

# Cleanup and setup
rm -rf "$DIST_BASE"
rm -rf "$OUTPUT_DIR"
mkdir -p "$DIST_BASE"
mkdir -p "$OUTPUT_DIR"

echo "🚀 Creating OS-specific Quizia distributions..."
echo ""

# =============================================================================
# UBUNTU/LINUX DISTRIBUTION
# =============================================================================
echo "📦 Creating Ubuntu/Linux distribution..."
UBUNTU_DIR="$DIST_BASE/quizia-ubuntu-$VERSION"
mkdir -p "$UBUNTU_DIR"

# Copy jar
cp "$BUILD_DIR"/quizia-frontend-all.jar "$UBUNTU_DIR/quizia.jar"

# Create Ubuntu-specific run script
cat > "$UBUNTU_DIR/run-quizia.sh" << 'EOF'
#!/bin/bash
# Quizia - Ubuntu/Linux Launcher

echo "🎯 Starting Quizia Quiz Application..."
echo ""
echo "Requirements:"
echo "  ✓ Java 21 or higher installed"
echo ""

# Check for Java
if ! command -v java &> /dev/null; then
    echo "❌ Java is not installed!"
    echo "Please install Java 21 or higher:"
    echo "  Ubuntu/Debian: sudo apt-get install openjdk-21-jdk"
    echo "  Fedora/RHEL: sudo dnf install java-21-openjdk"
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | grep -oP 'version "\K[0-9]+' | head -1)
echo "✓ Java version: $JAVA_VERSION detected"
echo ""

# Launch application
java -jar quizia.jar "$@"
EOF
chmod +x "$UBUNTU_DIR/run-quizia.sh"

# Create Ubuntu install guide
cat > "$UBUNTU_DIR/INSTALL.md" << 'EOF'
# 🎯 Quizia - Ubuntu/Linux Installation Guide

## Prerequisites

You need Java 21 or higher installed on your system.

### Install Java 21

**Ubuntu/Debian:**
```bash
sudo apt-get update
sudo apt-get install openjdk-21-jdk
```

**Fedora/RHEL:**
```bash
sudo dnf install java-21-openjdk
```

**Arch:**
```bash
sudo pacman -S jdk21-openjdk
```

Verify installation:
```bash
java -version
```

## Running Quizia

Simply run the launcher script:

```bash
./run-quizia.sh
```

Or run directly:
```bash
java -jar quizia.jar
```

## Troubleshooting

**Java not found:**
- Make sure Java 21+ is installed and in your PATH
- Try `which java` to check

**Permission denied:**
- Run: `chmod +x run-quizia.sh`

## Application Features

- 🎮 Interactive Quiz Application
- 📊 Real-time Leaderboard
- 👥 Multiplayer Support
- 🎯 6 Questions Per Quiz
- ⏱️ 10-Second Timer Per Question

## System Requirements

- **OS**: Ubuntu 18.04+ / Debian 10+ / Any Linux Distribution
- **RAM**: 2GB minimum
- **Java**: OpenJDK 21 or Oracle JDK 21+
- **Disk Space**: 200MB

Enjoy the quiz! 🚀
EOF

# Create README
cat > "$UBUNTU_DIR/README.md" << 'EOF'
# Quizia - Cross-Platform Quiz Application

Welcome to Quizia! A modern, interactive quiz application with real-time multiplayer features.

## Quick Start

1. Ensure Java 21+ is installed
2. Run: `./run-quizia.sh`
3. Create or join a room
4. Select a topic and start playing!

## Features

✨ **Multi-Player Support** - Create rooms and invite friends
✨ **Real-time Leaderboard** - See live rankings
✨ **6 Questions Per Quiz** - Quick and engaging rounds
✨ **Auto-Advance** - 10-second timer per question
✨ **Multiple Topics** - General Knowledge, Science, History, and more

## Getting Started

### Creating a Room
1. Enter your username
2. Click "Create Room"
3. Select topics
4. Share the Room ID with friends

### Joining a Room
1. Enter your username
2. Click "Join Room"
3. Find a room and click "Join"
4. Wait for the creator to start

### Playing the Quiz
1. Read each question carefully
2. Select your answer
3. Questions auto-advance after 10 seconds
4. See results on the leaderboard

## Support

For issues or feature requests, visit our GitHub repository.

Version: 1.0.0
EOF

cd "$DIST_BASE"
zip -r "$OUTPUT_DIR/quizia-ubuntu-$VERSION.zip" "quizia-ubuntu-$VERSION/" > /dev/null
echo "✅ Created: quizia-ubuntu-$VERSION.zip ($(du -sh quizia-ubuntu-$VERSION | cut -f1))"
echo ""

# =============================================================================
# MACOS DISTRIBUTION
# =============================================================================
echo "📦 Creating macOS distribution..."
MACOS_DIR="$DIST_BASE/quizia-macos-$VERSION"
mkdir -p "$MACOS_DIR"

# Copy jar
cp "$BUILD_DIR"/quizia-frontend-all.jar "$MACOS_DIR/quizia.jar"

# Create macOS-specific run script with .command extension
cat > "$MACOS_DIR/run-quizia.command" << 'EOF'
#!/bin/bash
# Quizia - macOS Launcher

echo "🎯 Starting Quizia Quiz Application..."
echo ""
echo "Requirements:"
echo "  ✓ Java 21 or higher installed"
echo ""

# Get script directory
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$DIR"

# Check for Java
if ! command -v java &> /dev/null; then
    echo "❌ Java is not installed!"
    echo ""
    echo "Please install Java 21 or higher:"
    echo "  Option 1: Homebrew"
    echo "    brew install openjdk@21"
    echo ""
    echo "  Option 2: Download from"
    echo "    https://www.oracle.com/java/technologies/downloads/#java21"
    echo ""
    echo "After installation, try running this script again."
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | grep -oP 'version "\K[0-9]+' | head -1)
echo "✓ Java version: $JAVA_VERSION detected"
echo ""

# Launch application
java -jar quizia.jar "$@"
EOF
chmod +x "$MACOS_DIR/run-quizia.command"

# Create macOS install guide
cat > "$MACOS_DIR/INSTALL.md" << 'EOF'
# 🎯 Quizia - macOS Installation Guide

## Prerequisites

You need Java 21 or higher installed on your system.

### Install Java 21 on macOS

**Option 1: Using Homebrew (Recommended)**
```bash
# Install Homebrew if not already installed
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

# Install Java 21
brew install openjdk@21

# Link Java (if not automatically in PATH)
sudo ln -sfn /opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk /Library/Java/JavaVirtualMachines/openjdk-21.jdk
```

**Option 2: Download from Oracle**
1. Visit https://www.oracle.com/java/technologies/downloads/#java21
2. Download macOS installer
3. Follow installation wizard

Verify installation:
```bash
java -version
```

## Running Quizia

### Method 1: Double-click (Easiest)
Simply double-click `run-quizia.command` in Finder

### Method 2: Terminal
```bash
chmod +x run-quizia.command
./run-quizia.command
```

### Method 3: Direct Java
```bash
java -jar quizia.jar
```

## Troubleshooting

**Java not found error:**
- Make sure Java 21+ is installed
- Try opening Terminal and running: `java -version`
- If not found, reinstall Java using Homebrew

**"Permission denied" error:**
- Run in Terminal: `chmod +x run-quizia.command`

**App won't launch:**
- Ensure the file is extracted completely
- Check that `quizia.jar` is in the same folder as the launcher script

## Application Features

- 🎮 Interactive Quiz Application
- 📊 Real-time Leaderboard
- 👥 Multiplayer Support
- 🎯 6 Questions Per Quiz
- ⏱️ 10-Second Timer Per Question

## System Requirements

- **OS**: macOS 10.14 (Mojave) or higher
- **Processor**: Intel or Apple Silicon (M1/M2/M3)
- **RAM**: 2GB minimum
- **Java**: OpenJDK 21 or Oracle JDK 21+
- **Disk Space**: 200MB

Enjoy the quiz! 🚀
EOF

# Create README
cat > "$MACOS_DIR/README.md" << 'EOF'
# Quizia - Cross-Platform Quiz Application

Welcome to Quizia! A modern, interactive quiz application with real-time multiplayer features.

## Quick Start

1. Ensure Java 21+ is installed
2. Double-click `run-quizia.command` or run `./run-quizia.command` in Terminal
3. Create or join a room
4. Select a topic and start playing!

## Features

✨ **Multi-Player Support** - Create rooms and invite friends
✨ **Real-time Leaderboard** - See live rankings
✨ **6 Questions Per Quiz** - Quick and engaging rounds
✨ **Auto-Advance** - 10-second timer per question
✨ **Multiple Topics** - General Knowledge, Science, History, and more

## Getting Started

### Creating a Room
1. Enter your username
2. Click "Create Room"
3. Select topics
4. Share the Room ID with friends

### Joining a Room
1. Enter your username
2. Click "Join Room"
3. Find a room and click "Join"
4. Wait for the creator to start

### Playing the Quiz
1. Read each question carefully
2. Select your answer
3. Questions auto-advance after 10 seconds
4. See results on the leaderboard

## Support

For issues or feature requests, visit our GitHub repository.

Version: 1.0.0
EOF

cd "$DIST_BASE"
zip -r "$OUTPUT_DIR/quizia-macos-$VERSION.zip" "quizia-macos-$VERSION/" > /dev/null
echo "✅ Created: quizia-macos-$VERSION.zip ($(du -sh quizia-macos-$VERSION | cut -f1))"
echo ""

# =============================================================================
# WINDOWS DISTRIBUTION
# =============================================================================
echo "📦 Creating Windows distribution..."
WINDOWS_DIR="$DIST_BASE/quizia-windows-$VERSION"
mkdir -p "$WINDOWS_DIR"

# Copy jar
cp "$BUILD_DIR"/quizia-frontend-all.jar "$WINDOWS_DIR/quizia.jar"

# Create Windows batch launcher
cat > "$WINDOWS_DIR/run-quizia.bat" << 'EOF'
@echo off
REM Quizia - Windows Launcher

echo.
echo ========================================
echo   Quizia - Quiz Application Launcher
echo ========================================
echo.
echo Checking for Java installation...
echo.

REM Check for Java
where java >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Java is not installed or not in your PATH!
    echo.
    echo Please install Java 21 or higher:
    echo   1. Visit: https://www.oracle.com/java/technologies/downloads/#java21
    echo   2. Download the Windows installer
    echo   3. Run the installer and follow the wizard
    echo   4. Restart this application
    echo.
    pause
    exit /b 1
)

REM Get Java version
for /f "tokens=3" %%i in ('java -version 2^>^&1 ^| findstr /R "version"') do set JAVA_VERSION=%%i
echo [OK] Java detected: %JAVA_VERSION%
echo.

REM Launch application
echo Launching Quizia...
echo.
java -jar quizia.jar %*

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERROR] Failed to launch application
    echo.
    pause
)
EOF

# Create Windows PowerShell launcher
cat > "$WINDOWS_DIR/run-quizia.ps1" << 'EOF'
# Quizia - Windows PowerShell Launcher

Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "  Quizia - Quiz Application Launcher" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""
Write-Host "Checking for Java installation..." -ForegroundColor Yellow

# Check for Java
try {
    $javaVersion = java -version 2>&1 | Select-String 'version'
    Write-Host "[OK] Java detected: $javaVersion" -ForegroundColor Green
    Write-Host ""
} catch {
    Write-Host "[ERROR] Java is not installed or not in your PATH!" -ForegroundColor Red
    Write-Host ""
    Write-Host "Please install Java 21 or higher:" -ForegroundColor Yellow
    Write-Host "  1. Visit: https://www.oracle.com/java/technologies/downloads/#java21" -ForegroundColor Yellow
    Write-Host "  2. Download the Windows installer" -ForegroundColor Yellow
    Write-Host "  3. Run the installer and follow the wizard" -ForegroundColor Yellow
    Write-Host "  4. Restart this application" -ForegroundColor Yellow
    Write-Host ""
    Read-Host "Press Enter to exit"
    exit 1
}

# Launch application
Write-Host "Launching Quizia..." -ForegroundColor Yellow
Write-Host ""

$jarPath = Join-Path $PSScriptRoot "quizia.jar"
& java -jar $jarPath $args

if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Host "[ERROR] Failed to launch application" -ForegroundColor Red
    Write-Host ""
    Read-Host "Press Enter to exit"
}
EOF

# Create Windows install guide
cat > "$WINDOWS_DIR/INSTALL.md" << 'EOF'
# 🎯 Quizia - Windows Installation Guide

## Prerequisites

You need Java 21 or higher installed on your system.

### Install Java 21 on Windows

1. **Download Java 21**
   - Visit: https://www.oracle.com/java/technologies/downloads/#java21
   - Click "Download" under "Windows x64 Installer"

2. **Run the Installer**
   - Double-click `jdk-21_windows-x64_bin.exe`
   - Follow the installation wizard
   - Accept the license agreement
   - Choose installation location (default is fine)
   - Click "Install"

3. **Verify Installation**
   - Open Command Prompt (press `Win + R`, type `cmd`, press Enter)
   - Type: `java -version`
   - You should see version information

## Running Quizia

### Method 1: Double-click (Easiest)
Simply double-click one of these files in the folder:
- `run-quizia.bat` (Command Prompt version)
- `run-quizia.ps1` (PowerShell version)

### Method 2: Command Prompt
1. Open Command Prompt in the application folder:
   - Shift + Right-click in folder → "Open Command window here"
   - Or: Press `Win + R`, type `cmd`, press Enter, then `cd C:\path\to\quizia`

2. Run:
   ```
   run-quizia.bat
   ```

### Method 3: PowerShell
1. Open PowerShell (right-click → "Open PowerShell window here")
2. Run:
   ```
   .\run-quizia.ps1
   ```

### Method 4: Direct Java
1. Open Command Prompt
2. Navigate to the application folder
3. Run:
   ```
   java -jar quizia.jar
   ```

## Troubleshooting

**Java not found error:**
- Make sure Java 21+ is installed
- Open Command Prompt and run: `java -version`
- If not found, reinstall Java from Oracle website
- You may need to restart your computer after installing Java

**"Permission denied" error in PowerShell:**
- Open PowerShell as Administrator
- Run: `Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser`
- Try running the script again

**"Cannot find java.exe":**
- Ensure Java is installed (see step 3 above)
- Try restarting your computer
- Check that Java is in your System PATH:
  - Open System Properties (Settings → System → About → Advanced system settings)
  - Click "Environment Variables"
  - Under "System variables", look for "Path"
  - It should contain your Java installation path (e.g., `C:\Program Files\Java\jdk-21`)

## Application Features

- 🎮 Interactive Quiz Application
- 📊 Real-time Leaderboard
- 👥 Multiplayer Support
- 🎯 6 Questions Per Quiz
- ⏱️ 10-Second Timer Per Question

## System Requirements

- **OS**: Windows 7 SP1 or higher (Windows 10/11 recommended)
- **Processor**: x64 architecture
- **RAM**: 2GB minimum
- **Java**: Oracle JDK 21 or OpenJDK 21+
- **Disk Space**: 200MB

Enjoy the quiz! 🚀
EOF

# Create README
cat > "$WINDOWS_DIR/README.md" << 'EOF'
# Quizia - Cross-Platform Quiz Application

Welcome to Quizia! A modern, interactive quiz application with real-time multiplayer features.

## Quick Start

1. Ensure Java 21+ is installed
2. Double-click `run-quizia.bat` or `run-quizia.ps1`
3. Create or join a room
4. Select a topic and start playing!

## Features

✨ **Multi-Player Support** - Create rooms and invite friends
✨ **Real-time Leaderboard** - See live rankings
✨ **6 Questions Per Quiz** - Quick and engaging rounds
✨ **Auto-Advance** - 10-second timer per question
✨ **Multiple Topics** - General Knowledge, Science, History, and more

## Getting Started

### Creating a Room
1. Enter your username
2. Click "Create Room"
3. Select topics
4. Share the Room ID with friends

### Joining a Room
1. Enter your username
2. Click "Join Room"
3. Find a room and click "Join"
4. Wait for the creator to start

### Playing the Quiz
1. Read each question carefully
2. Select your answer
3. Questions auto-advance after 10 seconds
4. See results on the leaderboard

## Support

For issues or feature requests, visit our GitHub repository.

Version: 1.0.0
EOF

cd "$DIST_BASE"
zip -r "$OUTPUT_DIR/quizia-windows-$VERSION.zip" "quizia-windows-$VERSION/" > /dev/null
echo "✅ Created: quizia-windows-$VERSION.zip ($(du -sh quizia-windows-$VERSION | cut -f1))"
echo ""

# =============================================================================
# SUMMARY
# =============================================================================
echo "=========================================="
echo "✨ Distribution Creation Complete!"
echo "=========================================="
echo ""
echo "📁 Output Location: $OUTPUT_DIR"
echo ""
echo "Generated Files:"
ls -lh "$OUTPUT_DIR"/*.zip 2>/dev/null | awk '{print "  - " $9 " (" $5 ")"}'
echo ""
echo "Summary:"
echo "  📦 quizia-ubuntu-$VERSION.zip"
echo "     └─ For: Ubuntu, Debian, and Linux distributions"
echo "     └─ Launcher: run-quizia.sh"
echo ""
echo "  📦 quizia-macos-$VERSION.zip"
echo "     └─ For: macOS 10.14+ (Intel & Apple Silicon)"
echo "     └─ Launcher: run-quizia.command"
echo ""
echo "  📦 quizia-windows-$VERSION.zip"
echo "     └─ For: Windows 7 SP1+ (x64)"
echo "     └─ Launchers: run-quizia.bat, run-quizia.ps1"
echo ""
echo "Each package includes:"
echo "  ✓ quizia.jar (Application)"
echo "  ✓ OS-specific launcher script"
echo "  ✓ Installation guide (INSTALL.md)"
echo "  ✓ Quick start guide (README.md)"
echo ""
echo "🚀 Ready to distribute!"
