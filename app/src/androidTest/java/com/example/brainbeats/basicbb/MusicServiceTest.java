package com.example.brainbeats.basicbb;

import android.test.AndroidTestCase;

import java.util.ArrayList;
import java.util.List;

public class MusicServiceTest extends AndroidTestCase{

    public void testFilter() {
        MusicService musicService = new MusicService();

        List<Song> songList = new ArrayList<>();
        songList.add(new Song(1, "1",null,null));
        songList.add(new Song(2, "2",null,null));
        songList.add(new Song(3, "3",null,null));
        songList.add(new Song(4, "4",null,null));
        songList.add(new Song(5, "5",null,null));
        final int wantedA = 3;
        final int wantedB = 1;
        final Integer integer = musicService.filterPositionsByIds(
                new long[]{wantedA,wantedB}, songList
        );
        assertTrue(songList.get(integer).getID() == wantedA || songList.get(integer).getID() == wantedB );
    }

    public void testFilterWithNoIDs() {
        MusicService musicService = new MusicService();

        List<Song> songList = new ArrayList<>();
        songList.add(new Song(1, "1",null,null));
        songList.add(new Song(2, "2",null,null));
        songList.add(new Song(3, "3",null,null));
        songList.add(new Song(4, "4",null,null));
        songList.add(new Song(5, "5",null,null));
        final Integer integer = musicService.filterPositionsByIds(
                new long[]{}, songList
        );
        assertTrue(integer == -1);
    }
}