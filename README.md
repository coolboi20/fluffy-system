# AI-Powered Mood-based Music Playlist Generator (Java Edition)

This project is a Java-based version of the AI-powered system that builds Spotify playlists based on the user's current mood. It uses Spring Boot, Google Gemini AI, and Spotify's Web API.

## Requirements

- JDK 26
- Maven (or use included `mvnw`)
- Spotify Developer Account
- Google Gemini API Key

## Running the App

### Environment Variables

Set the following environment variables:

```powershell
$env:SPOTIPY_CLIENT_ID="your_client_id"
$env:SPOTIPY_CLIENT_SECRET="your_client_secret"
$env:SPOTIPY_REDIRECT_URI="http://localhost:8501/"
$env:GEMINI_API_KEY="your_actual_new_key_here"
```

> **Warning:** Your previous `GEMINI_API_KEY` was reported as leaked and revoked by Google. You MUST generate a new one at [Google AI Studio](https://aistudio.google.com/) to continue using the AI features.

### From the CLI (Recommended for Java 26)

Due to the futuristic nature of Java 26, use the provided `run.ps1` script or the manual command to bypass compatibility checks:

```powershell
# First, ensure you are using JDK 26
$env:JAVA_HOME = "C:\Users\mpho.mahase\.jdks\openjdk-26"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

# Build the project
.\mvnw.cmd clean compile

# Run the app
.\run.ps1
```

The app will be available at `http://localhost:8501/`.

### From the IDE (IntelliJ IDEA)

1. Open the project in IntelliJ IDEA.
2. Ensure Project SDK is set to **Java 26**.
3. If you see `TypeTag :: UNKNOWN`, ensure **Settings > Build > Compiler > Java Compiler > Project bytecode version** is set to **26**.
4. Add environment variables to your Run Configuration.

## What is SPOTIPY_REDIRECT_URI?

The **Redirect URI** is a security measure in the Spotify OAuth2 flow.
1. When you click "Log in", the app sends you to Spotify.
2. After you authorize the app, Spotify sends you back to this specific URI with a "code".
3. The app uses this code to get your access token.

**Crucial:** This URI must match exactly what you have registered in your [Spotify Developer Dashboard](https://developer.spotify.com/dashboard) under your App's settings. For local development, use `http://localhost:8501/`.

## How it works

1. **Log in with Spotify**: Click the "🎵 Log in with Spotify" button.
2. **Enter Mood or Upload Image**: Describe how you feel (e.g., "energetic for a workout") or upload a picture.
3. **Generate**: The app calls Google Gemini to suggest songs matching that mood or image.
4. **Save to Spotify**: Click "Create on Spotify" to save the playlist to your account.

