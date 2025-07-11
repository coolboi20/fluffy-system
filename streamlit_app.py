"""Streamlit app for generating mood-based playlists using OpenAI."""

import json
import os
from typing import Any, Dict, List

import spotipy
from spotipy.oauth2 import SpotifyOAuth

from openai import OpenAI
import streamlit as st
import re


def generate_playlist(mood: str, api_key: str) -> Dict[str, Any]:
    """Call OpenAI to create a playlist for the given mood."""

    client = OpenAI(api_key=api_key)

    messages = [
        {
            "role": "system",
            "content": (
                "You create short music playlists. Respond strictly in JSON with "
                "the keys 'title', 'description' and 'songs'. 'songs' must be a "
                "list of objects containing 'title' and 'artist'. Include 10 to "
                "15 songs. Do not add any explanation or extra text. "
            ),
        },
        {"role": "user", "content": f"Generate a playlist for the mood: {mood}"},
    ]

    response = client.chat.completions.create(
        model="gpt-4o",
        messages=messages,
        temperature=0.7,
    )

    content = response.choices[0].message.content

    # Remove code fences if present
    if content.startswith("```"):
        content = re.sub(r"^```.*?\n", "", content)
        content = re.sub(r"\n```$", "", content)
        content = content.strip()

    # Extract JSON portion in case GPT adds extra text
    json_match = re.search(r"\{.*\}", content, re.DOTALL)
    json_content = json_match.group(0) if json_match else content

    try:
        return json.loads(json_content)
    except json.JSONDecodeError:
        st.error(
            "Failed to parse playlist data from OpenAI response. Displaying raw output for debugging:"
        )
        st.code(content)
        return {"title": "", "description": "", "songs": []}


def create_spotify_playlist(sp: spotipy.Spotify, playlist: Dict[str, Any]) -> str:
    """Create a Spotify playlist and add tracks from the AI-generated list.

    Returns the external Spotify URL of the created playlist.
    """

    user_id = sp.current_user()["id"]

    new_playlist = sp.user_playlist_create(
        user_id,
        playlist.get("title", "New Playlist"),
        public=True,
        description=playlist.get("description", ""),
    )

    track_uris: List[str] = []
    for track in playlist.get("songs", []):
        query = f"track:{track['title']} artist:{track['artist']}"
        try:
            results = sp.search(q=query, type="track", limit=1)
            items = results.get("tracks", {}).get("items", [])
            if items:
                track_uris.append(items[0]["uri"])
            else:
                st.warning(f"Track not found on Spotify: {track['title']} - {track['artist']}")
        except Exception as e:
            st.warning(f"Error searching for track: {track['title']} - {track['artist']} ({e})")

    if track_uris:
        sp.playlist_add_items(new_playlist["id"], track_uris)

    return new_playlist["external_urls"]["spotify"]


# Streamlit app UI
st.title("AI Mood-Based Playlist Generator")

# Get API key from environment or user input
api_key = os.getenv("OPENAI_API_KEY")
if not api_key:
    st.error("OPENAI_API_KEY is not set. Please configure it in your Streamlit Cloud Secrets.")
    st.stop()

# Spotify authentication setup
sp_client_id = os.getenv("SPOTIPY_CLIENT_ID")
sp_client_secret = os.getenv("SPOTIPY_CLIENT_SECRET")
sp_redirect_uri = os.getenv("SPOTIPY_REDIRECT_URI")

if not all([sp_client_id, sp_client_secret, sp_redirect_uri]):
    st.error("Spotify client credentials are not configured in the environment.")
    st.stop()

sp_oauth = SpotifyOAuth(
    client_id=sp_client_id,
    client_secret=sp_client_secret,
    redirect_uri=sp_redirect_uri,
    scope="playlist-modify-public playlist-modify-private user-read-private",
    show_dialog=False,
)

query_params = st.query_params
if "code" in query_params and "token_info" not in st.session_state:
    code = query_params["code"]
    try:
        
        token_info = sp_oauth.get_access_token(code)
        access_token = token_info["access_token"]
        sp = spotipy.Spotify(auth=access_token)
        st.session_state["token_info"] = token_info
        st.query_params.clear()
    except Exception as e:
        st.error(f"Spotify OAuth failed: {e}")
        st.stop()

if "token_info" in st.session_state:
    token_info = st.session_state["token_info"]
    if sp_oauth.is_token_expired(token_info):
        token_info = sp_oauth.refresh_access_token(token_info["refresh_token"])
        st.session_state["token_info"] = token_info
    sp = spotipy.Spotify(auth=token_info["access_token"])
else:
    auth_url = sp_oauth.get_authorize_url()
    # Open in a new tab so nothing is ever sandboxed:
    login_link = f'''
      <a href="{auth_url}"
         target="_blank" rel="noopener noreferrer"
         style="font-size:18px; color:#1DB954; text-decoration:none;">
        🎵 Log in with Spotify (opens new tab)
      </a>
    '''
    st.markdown(login_link, unsafe_allow_html=True)
    st.stop()

# Input for user's mood
mood_input = st.text_input("Describe your current mood or vibe:")

if st.button("Generate Playlist"):
    if not mood_input:
        st.warning("Please describe your mood to generate a playlist.")
    elif not api_key:
        st.warning("An OpenAI API key is required.")
    else:
        with st.spinner("Generating playlist..."):
            playlist = generate_playlist(mood_input, api_key)

        # Display playlist information
        st.subheader(playlist.get("title", ""))
        st.write(playlist.get("description", ""))

        st.markdown("**Track List:**")
        for idx, track in enumerate(playlist.get("songs", []), start=1):
            st.write(f"{idx}. **{track['title']}** - {track['artist']}")

        with st.spinner("Creating playlist on Spotify..."):
            playlist_url = create_spotify_playlist(sp, playlist)
        st.success(f"Playlist created on Spotify! [Open Playlist]({playlist_url})")

