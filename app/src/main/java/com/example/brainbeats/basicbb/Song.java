package com.example.brainbeats.basicbb;

public class Song {

    private long id;
    private String title;
    private String artist;
    private String state;

    public Song(long songID, String songTitle, String songArtist, String aState) {
        id=songID;
        title=songTitle;
        artist=songArtist;
        state=aState;
    }

    public long getID(){return id;}
    public String getTitle(){return title;}
    public String getArtist(){return artist;}
    public String getState(){return state;}

    public void setId(long id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public void setState(String state) {
        this.state = state;
    }
}
