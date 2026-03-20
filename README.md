# AI-Powered Mood-based Music Playlist Generator (Java Edition)

This project is a Java-based version of the AI-powered system that builds Spotify playlists based on the user's current mood. It uses Spring Boot, OpenAI's API, and Spotify's Web API.

## Requirements

- JDK 26
- Maven
- Spotify Developer Account
- OpenAI API Key

## Running the App

### Environment Variables

Set the following environment variables to authenticate with Spotify and OpenAI:

```bash
SPOTIPY_CLIENT_ID=<your client id>
SPOTIPY_CLIENT_SECRET=<your client secret>
SPOTIPY_REDIRECT_URI=http://localhost:8501/
OPENAI_API_KEY=<your OpenAI key>
```

### From the IDE (IntelliJ IDEA)

1. Open the project in IntelliJ IDEA.
2. Select the `Run_Fluffy_App` Run Configuration.
3. Add your environment variables to the Run Configuration.
4. Click Run.

### From the CLI

```bash
mvn spring-boot:run
```

The app will be available at `http://localhost:8501/`.

## How it works

1. Log in with Spotify: Click the "🎵 Log in with Spotify" button to authorize the app.
2. Enter Mood: Describe how you feel (e.g., "energetic for a workout" or "calm rainy afternoon").
3. Generate: The app calls OpenAI to suggest a list of songs matching that mood.
4. Save to Spotify: Click "Create on Spotify" to save the suggested playlist directly to your Spotify account.

