package com.example.brainbeats.basicbb.data;

import android.annotation.TargetApi;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.Nullable;

public class TagProvider extends ContentProvider {

    private static final UriMatcher sUriMatcher = buildUriMatcher();

    private TagDbHelper mOpenHelper;

    static final int TAG = 100;
    static final int SONGTAG = 200;

    @Override
    public boolean onCreate() {
        mOpenHelper = new TagDbHelper(getContext());
        return true;
    }

    @Nullable
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        Cursor retCursor;
        switch (sUriMatcher.match(uri)) {
            case TAG:
                SQLiteDatabase db = mOpenHelper.getReadableDatabase();
                retCursor = db.query(TagContract.TagEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            case SONGTAG:
                SQLiteDatabase songTagdb = mOpenHelper.getReadableDatabase();
                retCursor = songTagdb.query(TagContract.SongTagEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        retCursor.setNotificationUri(getContext().getContentResolver(), uri);
        return retCursor;
    }

    @Nullable
    @Override
    public String getType(Uri uri) {
        int match = sUriMatcher.match(uri);
        switch (match) {
            case TAG:
                return TagContract.TagEntry.CONTENT_TYPE;
            case SONGTAG:
                return TagContract.SongTagEntry.CONTENT_TYPE;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        Uri returnUri;
        switch (match) {
            case TAG: {
                final String tagName = values.getAsString(TagContract.TagEntry.COLUMN_NAME);
                final Cursor tagAllreadyExists = query(
                        TagContract.TagEntry.CONTENT_URI,
                        null,
                        TagContract.TagEntry.TABLE_NAME + "." + TagContract.TagEntry.COLUMN_NAME + " = ?",
                        new String[]{tagName}, null);
                if (tagAllreadyExists.getCount() > 0) {
                    tagAllreadyExists.moveToNext();
                    final long id = tagAllreadyExists.getLong(
                            tagAllreadyExists.getColumnIndex(TagContract.TagEntry._ID)
                    );
                    returnUri = TagContract.TagEntry.buildTagUri(id);
                    break;
                }
                long _id = db.insert(TagContract.TagEntry.TABLE_NAME, null, values);
                if ( _id > 0 )
                    returnUri = TagContract.TagEntry.buildTagUri(_id);
                else
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                break;
            }
            case SONGTAG: {
                final String song = values.getAsString(TagContract.SongTagEntry.COLUMN_SONG);
                final Cursor songHasAllreadyTag = query(
                        TagContract.SongTagEntry.CONTENT_URI,
                        null,
                        TagContract.SongTagEntry.TABLE_NAME + "." + TagContract.SongTagEntry.COLUMN_SONG + " = ?",
                        new String[]{song}, null);
                if (songHasAllreadyTag.getCount() > 0) {
                    songHasAllreadyTag.moveToNext();
                    final long id = songHasAllreadyTag.getLong(
                            songHasAllreadyTag.getColumnIndex(TagContract.SongTagEntry._ID)
                    );
                    update(TagContract.SongTagEntry.CONTENT_URI, values,
                            TagContract.SongTagEntry.TABLE_NAME + "." + TagContract.SongTagEntry._ID + " = ?",
                            new String[]{id+""});
                    returnUri = TagContract.SongTagEntry.buildSongTagUri(id);
                    break;
                }
                long _id = db.insert(TagContract.SongTagEntry.TABLE_NAME, null, values);
                if (_id > 0)
                    returnUri = TagContract.SongTagEntry.buildSongTagUri(_id);
                else
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                break;
            }
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return returnUri;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int match = sUriMatcher.match(uri);
        int deletedRows = 0;
        switch (match){
            case TAG:
                deletedRows = db.delete(
                        TagContract.TagEntry.TABLE_NAME, selection, selectionArgs
                );
                break;
            case SONGTAG:
                deletedRows = db.delete(
                        TagContract.SongTagEntry.TABLE_NAME, selection, selectionArgs
                );
                break;
        }
        if (deletedRows != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return deletedRows;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int match = sUriMatcher.match(uri);
        int updatedRows = 0;
        switch (match){
            case TAG:
                updatedRows = db.update(
                        TagContract.TagEntry.TABLE_NAME, values, selection, selectionArgs
                );
                break;
            case SONGTAG:
                updatedRows = db.update(
                        TagContract.SongTagEntry.TABLE_NAME, values, selection, selectionArgs
                );
                break;
        }
        if (updatedRows != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return updatedRows;
    }


    private static UriMatcher buildUriMatcher() {
        UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        matcher.addURI(TagContract.CONTENT_AUTHORITY, TagContract.PATH_TAG, TAG);
        matcher.addURI(TagContract.CONTENT_AUTHORITY, TagContract.PATH_SONGTAG, SONGTAG);
        return matcher;
    }

    // You do not need to call this method. This is a method specifically to assist the testing
    // framework in running smoothly. You can read more at:
    // http://developer.android.com/reference/android/content/ContentProvider.html#shutdown()
    @Override
    @TargetApi(11)
    public void shutdown() {
        mOpenHelper.close();
        super.shutdown();
    }

}
