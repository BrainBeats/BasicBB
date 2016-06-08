package com.example.tdeframond.basicbb;

/**
 * Created by tdeframond on 19/05/16.
 */
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

}
