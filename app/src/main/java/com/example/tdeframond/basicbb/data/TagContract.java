package com.example.tdeframond.basicbb.data;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.net.Uri;
import android.provider.BaseColumns;

public class TagContract {

    public static final String CONTENT_AUTHORITY = "com.pi.android.brainbeats";

    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    public static final String PATH_TAG = "tag";
    public static final String PATH_SONGTAG = "songtag";

    public static final class TagEntry implements BaseColumns {

        public static final String TABLE_NAME = "tag";

        public static final String COLUMN_NAME= "name";

        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_TAG).build();

        public static final String CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_TAG;

        public static Uri buildTagUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }
    }

    public static final class SongTagEntry implements BaseColumns {
        public static final String TABLE_NAME = "song_tag";

        public static final String COLUMN_SONG= "song";

        public static final String COLUMN_TAG= "tag";

        public static final String CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_SONGTAG;

        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_SONGTAG).build();

        public static Uri buildSongTagUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }
    }


}
