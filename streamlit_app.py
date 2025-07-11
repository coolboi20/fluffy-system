import streamlit as st
import openai
from typing import Dict, List


def generate_playlist(mood: str) -> Dict[str, any]:
    """Placeholder function to generate a playlist using OpenAI's API."""
    # TODO: Replace this stub with actual OpenAI API call.
    # Example of how you would call the API:
    # openai.api_key = "YOUR_OPENAI_API_KEY"
    # response = openai.ChatCompletion.create(...)
    # Parse the response to extract playlist information.

    songs: List[Dict[str, str]] = [
        {"title": f"Song {i}", "artist": f"Artist {i}"}
        for i in range(1, 11)
    ]
    return {
        "title": f"Awesome Vibes for {mood.title()}",
        "description": f"A sample playlist generated for the mood: {mood}",
        "songs": songs,
    }


# Streamlit app UI
st.title("AI Mood-Based Playlist Generator")

# Input for user's mood
mood_input = st.text_input("Describe your current mood or vibe:")

if st.button("Generate Playlist") and mood_input:
    with st.spinner("Generating playlist..."):
        playlist = generate_playlist(mood_input)

    # Display playlist information
    st.subheader(playlist["title"])
    st.write(playlist["description"])

    st.markdown("**Track List:**")
    for idx, track in enumerate(playlist["songs"], start=1):
        st.write(f"{idx}. **{track['title']}** - {track['artist']}")

    st.markdown(
        "\n---\n*Placeholder: Integrate Spotify API via spotipy here to create the playlist on Spotify.*"
    )

