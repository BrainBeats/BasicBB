package com.example.brainbeats.basicbb.data;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.test.AndroidTestCase;

public class TagProviderTest extends AndroidTestCase {

    public void deleteAllRecordsFromProvider() {
        mContext.getContentResolver().delete(
                TagContract.TagEntry.CONTENT_URI,
                null,
                null
        );
        mContext.getContentResolver().delete(
                TagContract.SongTagEntry.CONTENT_URI,
                null,
                null
        );

        Cursor cursor = mContext.getContentResolver().query(
                TagContract.TagEntry.CONTENT_URI,
                null,
                null,
                null,
                null
        );
        assertEquals("Error: Records not deleted from Tag table during delete", 0, cursor.getCount());
        cursor.close();

        cursor = mContext.getContentResolver().query(
                TagContract.SongTagEntry.CONTENT_URI,
                null,
                null,
                null,
                null
        );
        assertEquals("Error: Records not deleted from SongTag table during delete", 0, cursor.getCount());
        cursor.close();
    }


    public void testProviderRegistry() {
        PackageManager pm = mContext.getPackageManager();

        // We define the component name based on the package name from the context and the
        // TagProvider class.
        ComponentName componentName = new ComponentName(mContext.getPackageName(),
                TagProvider.class.getName());
        try {

            ProviderInfo providerInfo = pm.getProviderInfo(componentName, 0);
            // Make sure that the registered authority matches the authority from the Contract.
            assertEquals("Error: TagProvider registered with authority: " + providerInfo.authority +
                            " instead of authority: " + TagContract.CONTENT_AUTHORITY,
                    providerInfo.authority, TagContract.CONTENT_AUTHORITY);
        } catch (PackageManager.NameNotFoundException e) {
            assertTrue("Error: TagProvider not registered at " + mContext.getPackageName(),
                    false);
        }
    }

    public void testGetType() {
        String type = mContext.getContentResolver().getType(TagContract.TagEntry.CONTENT_URI);
        assertEquals("Error: the TagEntry CONTENT_URI should return TagEntry.CONTENT_TYPE",
                TagContract.TagEntry.CONTENT_TYPE, type);

        type = mContext.getContentResolver().getType(TagContract.SongTagEntry.CONTENT_URI);
        assertEquals("Error: the SongTagEntry CONTENT_URI should return SongTagEntry.CONTENT_TYPE",
                TagContract.SongTagEntry.CONTENT_TYPE, type);
    }

    public void testInsertReadProvider() {
        ContentValues testValues = new ContentValues();
        testValues.put(TagContract.TagEntry.COLUMN_NAME, "happy");

        TestUtilities.TestContentObserver tco = TestUtilities.getTestContentObserver();
        final ContentResolver contentResolver = mContext.getContentResolver();
        contentResolver.registerContentObserver(TagContract.TagEntry.CONTENT_URI, true, tco);
        Uri tagUri = contentResolver.insert(TagContract.TagEntry.CONTENT_URI, testValues);

        tco.waitForNotificationOrFail();
        contentResolver.unregisterContentObserver(tco);

        long tagRowId = ContentUris.parseId(tagUri);
        assertTrue(tagRowId != -1);

        Cursor cursor = contentResolver.query(
                TagContract.TagEntry.CONTENT_URI,
                null, // leaving "columns" null just returns all the columns.
                null, // cols for "where" clause
                null, // values for "where" clause
                null  // sort order
        );

        TestUtilities.validateCursor("testInsertReadProvider. Error validating TagEntry.",
                cursor, testValues);

        ContentValues songTagValues = new ContentValues();
        songTagValues.put(TagContract.SongTagEntry.COLUMN_SONG, 12);
        songTagValues.put(TagContract.SongTagEntry.COLUMN_TAG, tagRowId);
        tco = TestUtilities.getTestContentObserver();

        mContext.getContentResolver().registerContentObserver(TagContract.SongTagEntry.CONTENT_URI, true, tco);

        Uri songTagInsertUri = mContext.getContentResolver()
                .insert(TagContract.SongTagEntry.CONTENT_URI, songTagValues);
        assertTrue(songTagInsertUri != null);

        tco.waitForNotificationOrFail();
        mContext.getContentResolver().unregisterContentObserver(tco);

        Cursor songTagCursor = mContext.getContentResolver().query(
                TagContract.SongTagEntry.CONTENT_URI,  // Table to Query
                null, // leaving "columns" null just returns all the columns.
                TagContract.SongTagEntry.TABLE_NAME + "." + TagContract.SongTagEntry.COLUMN_TAG + " = " + tagRowId, // cols for "where" clause
                null, // values for "where" clause
                null // columns to group by
        );

        assertTrue(songTagCursor.getCount() > 0);
        TestUtilities.validateCursor("testInsertReadProvider. Error validating TagEntry insert.",
                songTagCursor, songTagValues);
    }

    public void testInsertTwoTagWithSameName() {
        final String tagName = "bolo";
        insertTagWithName(tagName);
        final Cursor firstQuery = mContext.getContentResolver().query(TagContract.TagEntry.CONTENT_URI, null, null, null, null);
        insertTagWithName(tagName);
        final Cursor secondQuery = mContext.getContentResolver().query(TagContract.TagEntry.CONTENT_URI, null, null, null, null);
        assertEquals(firstQuery.getCount(), secondQuery.getCount());
    }

    public void testInsertTwoTagToSameSong() {
        final long firstID = insertTagWithName("bolo");
        final long secondID = insertTagWithName("molo");
        ContentResolver contentResolver = mContext.getContentResolver();

        final Cursor before = contentResolver.query(
                TagContract.SongTagEntry.CONTENT_URI, null, null, null, null
        );
        int countAtTheBeginning = before.getCount() + 1;
        long songID = 12345;
        contentResolver.insert(TagContract.SongTagEntry.CONTENT_URI, createValues(firstID, songID));
        final Cursor firstQuery = contentResolver.query(
                TagContract.SongTagEntry.CONTENT_URI, null, null, null, null
        );
        assertNotNull(firstQuery);
        assertEquals(firstQuery.getCount(), countAtTheBeginning);
        contentResolver.insert(TagContract.SongTagEntry.CONTENT_URI, createValues(secondID, songID));
        final Cursor secondQuery = contentResolver.query(
                TagContract.SongTagEntry.CONTENT_URI, null, null, null, null
        );
        assertNotNull(secondQuery);
        assertEquals(secondQuery.getCount(), countAtTheBeginning);
    }

    @NonNull
    private ContentValues createValues(long tagID, long songID) {
        ContentValues bolo = new ContentValues();
        bolo.put(TagContract.SongTagEntry.COLUMN_TAG, tagID);
        bolo.put(TagContract.SongTagEntry.COLUMN_SONG, songID);
        return bolo;
    }

    private long insertTagWithName(String tagName) {
        ContentValues values = new ContentValues();
        values.put(TagContract.TagEntry.COLUMN_NAME, tagName);

        final Uri uri = TagContract.TagEntry.CONTENT_URI;
        final ContentResolver contentResolver = mContext.getContentResolver();
        Uri inserted = contentResolver.insert(uri, values);
        assertNotNull(inserted);
        return ContentUris.parseId(inserted);
    }

    public void testDeleteRecords() {
        testInsertReadProvider();

        TestUtilities.TestContentObserver tagObserver = TestUtilities.getTestContentObserver();
        mContext.getContentResolver().registerContentObserver(TagContract.TagEntry.CONTENT_URI, true, tagObserver);

        TestUtilities.TestContentObserver songTagObserver = TestUtilities.getTestContentObserver();
        mContext.getContentResolver().registerContentObserver(TagContract.SongTagEntry.CONTENT_URI, true, songTagObserver);

        deleteAllRecordsFromProvider();

        tagObserver.waitForNotificationOrFail();
        songTagObserver.waitForNotificationOrFail();

        mContext.getContentResolver().unregisterContentObserver(tagObserver);
        mContext.getContentResolver().unregisterContentObserver(songTagObserver);
    }

    public void testUpdateTag() {
        ContentValues values = new ContentValues();
        values.put(TagContract.TagEntry.COLUMN_NAME, "triste");

        Uri tagUri = mContext.getContentResolver().
                insert(TagContract.TagEntry.CONTENT_URI, values);
        long tagRowId = ContentUris.parseId(tagUri);

        assertTrue(tagRowId != -1);

        ContentValues updatedValues = new ContentValues(values);
        updatedValues.put(TagContract.TagEntry._ID, tagRowId);
        updatedValues.put(TagContract.TagEntry.COLUMN_NAME, "exite");

        Cursor tagCursor = mContext.getContentResolver().query(
                TagContract.TagEntry.CONTENT_URI, null, null, null, null
        );

        TestUtilities.TestContentObserver tco = TestUtilities.getTestContentObserver();
        tagCursor.registerContentObserver(tco);

        int count = mContext.getContentResolver().update(
                TagContract.TagEntry.CONTENT_URI, updatedValues, TagContract.TagEntry._ID + "= ?",
                new String[] { Long.toString(tagRowId)});
        assertEquals(count, 1);

        tco.waitForNotificationOrFail();

        tagCursor.unregisterContentObserver(tco);
        tagCursor.close();

        Cursor cursor = mContext.getContentResolver().query(
                TagContract.TagEntry.CONTENT_URI,
                null,   // projection
                TagContract.TagEntry._ID + " = " + tagRowId,
                null,   // Values for the "where" clause
                null    // sort order
        );

        TestUtilities.validateCursor("testUpdateTag.  Error validating tag entry update.",
                cursor, updatedValues);

        cursor.close();
    }
}
