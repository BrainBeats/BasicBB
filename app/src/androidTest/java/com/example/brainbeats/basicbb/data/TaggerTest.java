package com.example.brainbeats.basicbb.data;

import android.test.AndroidTestCase;

public class TaggerTest extends AndroidTestCase{

    private Tagger tagger;

    public void setUp() {
        tagger = new Tagger(mContext);
        tagger.deleteAll();
    }

    public void testAddTag() throws Exception {
        String tag = "exited";
        final long tagID = tagger.addTag(tag);
        assertEquals(tagger.getTagByTagID(tagID), tag);
    }

    public void testAddTagForSong() throws Exception {
        int id = 53;
        String tag = "calme";
        final long tagId = tagger.addTagToSong(id, tag);
        assertEquals(tagger.getTagByTagID(tagId), tag);
        String searchedTag = tagger.getTagBySongID(id);
        assertEquals(tag, searchedTag);
    }

    public void testGetUnknownTag() {
        assertEquals("", tagger.getTagBySongID(1099900));
    }

    public void testGetTagIdByName() {
        String tag = "exited";
        final long tagID = tagger.addTag(tag);
        assertEquals(tagID, tagger.getTagId(tag));
    }

    public void testGetAllMusicByTag() {
        int id = 53;
        String tag = "calme";
        final long tagId = tagger.addTagToSong(id, tag);
        String searchedTag = tagger.getTagBySongID(id);
        final long[] allMusic = tagger.getAllMusicByTag(searchedTag);
        assertTrue(allMusic.length > 0);
        assertEquals(allMusic[0], id);
    }
}