Quizia Frontend - Distribution & Setup Guide

## Overview
This is the Quizia Quiz Application - a JavaFX-based desktop client for participating in multiplayer quizzes.

## System Requirements
- **Java 21** or later
- **Operating System**: Windows, macOS, or Linux
- **Internet Connection**: Required to connect to the Quizia server

## Installation & Running

### Option 1: Run from JAR (Easiest)
1. Download `quizia-frontend-all.jar` from the releases page
2. Double-click the JAR file, or run from terminal:
   ```bash
   java -jar quizia-frontend-all.jar
   ```

### Option 2: Build from Source
```bash
cd quizia_frontend/app
./gradlew build --no-configuration-cache
java -jar build/libs/quizia-frontend-all.jar
```

## Configuration

The app connects to the Quizia backend server. By default, it uses:
```
https://quizia-backend-operation.onrender.com
```

### Change Server URL
1. **First Run**: The app creates a config file at:
   - **Windows**: `C:\Users\<username>\.quizia\quizia-config.properties`
   - **macOS/Linux**: `~/.quizia/quizia-config.properties`

2. **Edit the config file** and change:
   ```properties
   backend.url=https://your-custom-server.com
   ```

3. **Restart the app** to apply changes

## Usage

1. **Start the App**: Launch the JAR or run from terminal
2. **Enter Username**: You'll be prompted to enter a username
3. **Choose an Action**:
   - **Create Room**: Register a new quiz room with topics
   - **Join Room**: Join an existing quiz room
4. **Play Quiz**: Answer questions and see your ranking

## Troubleshooting

### Can't connect to server
- Check your internet connection
- Verify the server URL in the config file
- Make sure the server is running and accessible

### Wrong Java version
- Install Java 21: https://www.oracle.com/java/technologies/downloads/
- Verify: `java -version` in terminal

### Config file not created
- Create the folder manually: `.quizia` in your home directory
- Create `quizia-config.properties` with content:
  ```properties
  backend.url=https://quizia-backend-operation.onrender.com
  ```

## Features
- ✅ Multiplayer quiz rooms
- ✅ Multiple quiz topics
- ✅ Real-time question delivery
- ✅ Leaderboard tracking
- ✅ Configurable server connection

## Support
For issues or questions, contact the development team or submit an issue on GitHub.
