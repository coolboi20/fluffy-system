"""Streamlit app for generating mood-based playlists using OpenAI."""

import json
import os
from typing import Any, Dict, List

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
        content = re.sub(r"^```.*?\n", "", content)  # remove opening ```json or ```
        content = re.sub(r"\n```$", "", content)     # remove closing ```
        content = content.strip()

     # Extract JSON using regex in case GPT includes extra text or code fences
    json_match = re.search(r"\{.*\}", content, re.DOTALL)
    if json_match:
        json_content = json_match.group(0)
    else:
        json_content = content

    try:
        playlist = json.loads(content)
    except json.JSONDecodeError:
        st.error("Failed to parse playlist data from OpenAI response. Displaying raw output for debugging:")
        st.code(content)
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

