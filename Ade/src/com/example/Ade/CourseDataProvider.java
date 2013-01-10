package com.example.Ade;


import java.util.ArrayList;
import java.util.Random;

import android.annotation.SuppressLint;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;

class Hour {
    public int h;
    public int m;

    Hour(int h, int m) {
        this.h = h;
        this.m = m;
    }

    @Override
    public String toString() {
        String hS = Integer.toString(h);
        String mS = Integer.toString(m);

        if (h / 10 == 0) {
            hS = "0" + hS;
        }
        if (m / 10 == 0) {
            mS = "0" + mS;
        }
        return hS + ":" + mS;
    }
}

class CourseInfo {
    public int hour;
    public int minute;
    public String name;

    public CourseInfo(int hour, int minute, String name) {
        this.hour = hour;
        this.minute = minute;
        this.name = name;
    }
}

/**
 * The AppWidgetProvider for our sample weather widget.
 */
@SuppressLint("UseValueOf")
public class CourseDataProvider extends ContentProvider {
    public static final Uri CONTENT_URI =
            Uri.parse("content://com.example.Ade.provider");

    public static class Columns {
        public static final String ID = "_id";
        public static final String HOUR = "hour";
        public static final String MINUTE = "minute";
        public static final String LESSON = "lesson";
    }

    /**
     * Generally, this data will be stored in an external and persistent location (ie. File,
     * Database, SharedPreferences) so that the data can persist if the process is ever killed.
     * For simplicity, in this sample the data will only be stored in memory.
     */
    private static final ArrayList<CourseInfo> sData = new ArrayList<CourseInfo>();

    @Override
    public boolean onCreate() {
        // We are going to initialize the data provider with some default values
        sData.add(new CourseInfo(10, 15, "ang"));
        sData.add(new CourseInfo(5, 3, "inf"));
        return true;
    }

    @SuppressLint("UseValueOf")
    @Override
    public synchronized Cursor query(Uri uri, String[] projection, String selection,
                                     String[] selectionArgs, String sortOrder) {
        assert (uri.getPathSegments().isEmpty());

        // In this sample, we only query without any parameters, so we can just return a cursor to
        // all the weather data.
        final MatrixCursor c = new MatrixCursor(
                new String[]{Columns.ID, Columns.HOUR, Columns.MINUTE, Columns.LESSON});
        for (int i = 0; i < sData.size(); ++i) {
            final CourseInfo data = sData.get(i);
            c.addRow(new Object[]{new Integer(i), data.hour, data.minute, data.name});
        }
        return c;
    }

    @Override
    public String getType(Uri uri) {
        return "vnd.android.cursor.dir/vnd.weatherlistwidget.temperature";
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        // This example code does not support inserting
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // This example code does not support deleting
        return 0;
    }

    @Override
    public synchronized int update(Uri uri, ContentValues values, String selection,
                                   String[] selectionArgs) {
        assert (uri.getPathSegments().size() == 1);

        Random r = new Random();
        sData.clear();
        for (int i = 0; i < 5; i++) {
            sData.add(new CourseInfo(r.nextInt(24), r.nextInt(60), "u" + r.nextInt(100)));
        }

        // Notify any listeners that the data backing the content provider has changed, and return
        // the number of rows affected.
        getContext().getContentResolver().notifyChange(uri, null);
        return 1;
    }

}

