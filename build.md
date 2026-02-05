# SafeChestsX Build Guide (v3.1)

This guide shows how to build the plugin jar from source on **Windows**, **macOS**, and **Linux**.

## 1) Prerequisites
- **Git**
- **Java 21 (JDK)**  
  Make sure `java -version` shows **21**.

## 2) Clone the project
```bash
git clone <REPO_URL>
cd SafeChestsX
```

## 3) Build the jar

### Windows (PowerShell)
```powershell
.\gradlew.bat clean build
```

### macOS / Linux
```bash
./gradlew clean build
```

## 4) Locate the output jar
After a successful build, the jar is located at:
```
build/libs/SafeChestsX-3.1.0.jar
```

## 5) Copy to your server
1. Stop your Paper server.
2. Copy the jar into `plugins/`.
3. Start the server.

## Troubleshooting
- **Gradle wrapper not executable (macOS/Linux)**:
  ```bash
  chmod +x ./gradlew
  ```
- **Wrong Java version**:
  ```bash
  java -version
  ```
  Install JDK 21 and re-run the build.
