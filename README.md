# AI-Powered Mood-based Music Playlist Generator

This project aims to create an AI-powered system that builds Spotify playlists based on the user's current mood. By analyzing mood inputs and leveraging Spotify's API, it generates music suggestions tailored to how the user feels.

## Running the App

Set the following environment variables so the app can authenticate with Spotify:

```
SPOTIPY_CLIENT_ID=<your client id>
SPOTIPY_CLIENT_SECRET=<your client secret>
SPOTIPY_REDIRECT_URI=<redirect uri configured in the Spotify dashboard>
OPENAI_API_KEY=<your OpenAI key>
```

When launched, the app prompts you to authenticate with Spotify and then automatically creates a playlist in your account.

