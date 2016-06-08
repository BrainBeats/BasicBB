package com.example.brainbeats.basicbb.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class TagDbHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 2;

    static final String DATABASE_NAME = "tag.db";

    public TagDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public static final String CREATE_TAG_TABLE = "CREATE TABLE " + TagContract.TagEntry.TABLE_NAME + "(" +
            TagContract.TagEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
            TagContract.TagEntry.COLUMN_NAME + " TEXT NOT NULL UNIQUE);";
    public static final String DROP_TAG_TABLE = "DROP TABLE IF EXISTS " + TagContract.TagEntry.TABLE_NAME;

    public static final String CREATE_SONGTAG_TABLE = "CREATE TABLE " + TagContract.SongTagEntry.TABLE_NAME + "(" +
            TagContract.SongTagEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
            TagContract.SongTagEntry.COLUMN_SONG + " TEXT NOT NULL UNIQUE," +
            TagContract.SongTagEntry.COLUMN_TAG + " TEXT NOT NULL);";
    public static final String DROP_SONGTAG_TABLE = "DROP TABLE IF EXISTS " + TagContract.SongTagEntry.TABLE_NAME;

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(CREATE_TAG_TABLE);
        sqLiteDatabase.execSQL(CREATE_SONGTAG_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        sqLiteDatabase.execSQL(DROP_TAG_TABLE);
        sqLiteDatabase.execSQL(DROP_SONGTAG_TABLE);
        onCreate(sqLiteDatabase);
    }
}
