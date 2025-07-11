"""Streamlit app for generating mood-based playlists using OpenAI."""

import json
import os
from typing import Any, Dict, List

import openai
import streamlit as st


def generate_playlist(mood: str, api_key: str) -> Dict[str, Any]:
    """Call OpenAI to create a playlist for the given mood."""

    openai.api_key = api_key

    messages = [
        {
            "role": "system",
            "content": (
                "You create short music playlists. Respond strictly in JSON with "
                "the keys 'title', 'description' and 'songs'. 'songs' must be a "
                "list of objects containing 'title' and 'artist'. Include 10 to "
                "15 songs."
            ),
        },
        {"role": "user", "content": f"Generate a playlist for the mood: {mood}"},
    ]

    response = openai.ChatCompletion.create(
        model="gpt-4o",
        messages=messages,
        temperature=0.7,
    )

    content = response["choices"][0]["message"]["content"]

    try:
        playlist = json.loads(content)
    except json.JSONDecodeError:
        st.error("Failed to parse playlist data from OpenAI response.")
        return {"title": "", "description": "", "songs": []}

    return playlist


# Streamlit app UI
st.title("AI Mood-Based Playlist Generator")

# Get API key from environment or user input
api_key = os.getenv("OPENAI_API_KEY")
if not api_key:
    api_key = st.text_input("Enter your OpenAI API key:", type="password")

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

        st.markdown(
            "\n---\n*Placeholder: Integrate Spotify API via spotipy here to create the playlist on Spotify.*"
        )

