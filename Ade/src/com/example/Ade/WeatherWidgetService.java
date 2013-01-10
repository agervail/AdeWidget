
package com.example.Ade;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

/**
 * This is the service that provides the factory to be bound to the collection service.
 */
public class WeatherWidgetService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new StackRemoteViewsFactory(this.getApplicationContext(), intent);
    }
}

/**
 * This is the factory that will provide data to the collection widget.
 */
class StackRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {
    private Context mContext;
    private Cursor mCursor;

    public StackRemoteViewsFactory(Context context, Intent intent) {
        mContext = context;
        intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);
    }

    public void onCreate() {
        // Since we reload the cursor in onDataSetChanged() which gets called immediately after
        // onCreate(), we do nothing here.
    }

    public void onDestroy() {
        if (mCursor != null) {
            mCursor.close();
        }
    }

    public int getCount() {
        return mCursor.getCount();
    }

    public RemoteViews getViewAt(int position) {
        // Get the data for this position from the content provider
        String lesson = "Unknown Day";
        Hour hour = new Hour(0,0);
        if (mCursor.moveToPosition(position)) {
            final int hourColIndex = mCursor.getColumnIndex(CourseDataProvider.Columns.HOUR);
            final int minColIndex = mCursor.getColumnIndex(CourseDataProvider.Columns.MINUTE);
            final int lessonColIndex = mCursor.getColumnIndex(CourseDataProvider.Columns.LESSON);
            lesson = mCursor.getString(lessonColIndex);
            hour = new Hour(mCursor.getInt(hourColIndex),mCursor.getInt(minColIndex));

        }

        final int itemId = R.layout.widget_item;
        RemoteViews rv = new RemoteViews(mContext.getPackageName(), itemId);
        rv.setTextViewText(R.id.widget_item, hour + " /" + lesson);

        // Set the click intent so that we can handle it and show a toast message
        final Intent fillInIntent = new Intent();
        final Bundle extras = new Bundle();
        extras.putString(CourseProvider.EXTRA_DAY_ID, lesson);
        fillInIntent.putExtras(extras);
        rv.setOnClickFillInIntent(R.id.widget_item, fillInIntent);

        return rv;
    }

    public RemoteViews getLoadingView() {
        // We aren't going to return a default loading view in this sample
        return null;
    }

    public int getViewTypeCount() {
        // Technically, we have two types of views (the dark and light background views)
        return 2;
    }

    public long getItemId(int position) {
        return position;
    }

    public boolean hasStableIds() {
        return true;
    }

    public void onDataSetChanged() {
        // Refresh the cursor
        if (mCursor != null) {
            mCursor.close();
        }
        mCursor = mContext.getContentResolver().query(CourseDataProvider.CONTENT_URI, null, null,
                null, null);
    }
}
