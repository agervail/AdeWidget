package com.example.Ade;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.*;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.widget.RemoteViews;
import android.widget.Toast;

import java.util.Random;

/**
 * Our data observer just notifies an update for all weather widgets when it detects a change.
 */
class WeatherDataProviderObserver extends ContentObserver {
    private AppWidgetManager mAppWidgetManager;
    private ComponentName mComponentName;

    WeatherDataProviderObserver(AppWidgetManager mgr, ComponentName cn, Handler h) {
        super(h);
        mAppWidgetManager = mgr;
        mComponentName = cn;
    }

    @Override
    public void onChange(boolean selfChange) {
        // The data has changed, so notify the widget that the collection view needs to be updated.
        // In response, the factory's onDataSetChanged() will be called which will requery the
        // cursor for the new data.
        mAppWidgetManager.notifyAppWidgetViewDataChanged(
                mAppWidgetManager.getAppWidgetIds(mComponentName), R.id.weather_list);
    }
}


public class CourseProvider extends AppWidgetProvider {
    public static String CLICK_ACTION = "com.example.android.weatherlistwidget.CLICK";
    public static String REFRESH_ACTION = "com.example.android.weatherlistwidget.REFRESH";
    public static String EXTRA_DAY_ID = "com.example.android.weatherlistwidget.day";

    private static HandlerThread sWorkerThread;
    private static Handler sWorkerQueue;
    private static WeatherDataProviderObserver sDataObserver;



    public CourseProvider() {
        // Start the worker thread
        sWorkerThread = new HandlerThread("CourseProvider-worker");
        sWorkerThread.start();
        sWorkerQueue = new Handler(sWorkerThread.getLooper());

    }

    // XXX: clear the worker queue if we are destroyed?

    @Override
    public void onEnabled(Context context) {
        // Register for external updates to the data to trigger an update of the widget.  When using
        // content providers, the data is often updated via a background service, or in response to
        // user interaction in the main app.  To ensure that the widget always reflects the current
        // state of the data, we must listen for changes and update ourselves accordingly.
        final ContentResolver r = context.getContentResolver();
        if (sDataObserver == null) {
            final AppWidgetManager mgr = AppWidgetManager.getInstance(context);
            final ComponentName cn = new ComponentName(context, CourseProvider.class);
            sDataObserver = new WeatherDataProviderObserver(mgr, cn, sWorkerQueue);
            r.registerContentObserver(CourseDataProvider.CONTENT_URI, true, sDataObserver);
        }
    }

    @Override
    public void onReceive(Context ctx, Intent intent) {
        final String action = intent.getAction();
        if (action.equals(REFRESH_ACTION)) {
            // BroadcastReceivers have a limited amount of time to do work, so for this sample, we
            // are triggering an update of the data on another thread.  In practice, this update
            // can be triggered from a background service, or perhaps as a result of user actions
            // inside the main application.
            final Context context = ctx;
            sWorkerQueue.removeMessages(0);
            sWorkerQueue.post(new Runnable() {
                @Override
                public void run() {
                    final ContentResolver r = context.getContentResolver();
                    final Cursor c = r.query(CourseDataProvider.CONTENT_URI, null, null, null,
                            null);
                    final int count = c.getCount();

                    // We disable the data changed observer temporarily since each of the updates
                    // will trigger an onChange() in our data observer.
                    r.unregisterContentObserver(sDataObserver);
                    for (int i = 0; i < count; ++i) {
                        final Uri uri = ContentUris.withAppendedId(CourseDataProvider.CONTENT_URI, i);
                        final ContentValues values = new ContentValues();
                        values.put(CourseDataProvider.Columns.HOUR, new Random().nextInt(24));
                        values.put(CourseDataProvider.Columns.MINUTE, new Random().nextInt(60));
                        r.update(uri, values, null, null);
                    }
                    r.registerContentObserver(CourseDataProvider.CONTENT_URI, true, sDataObserver);
                    final AppWidgetManager mgr = AppWidgetManager.getInstance(context);
                    final ComponentName cn = new ComponentName(context, CourseProvider.class);
                    mgr.notifyAppWidgetViewDataChanged(mgr.getAppWidgetIds(cn), R.id.weather_list);
                }
            });
        } else if (action.equals(CLICK_ACTION)) {
            final String day = intent.getStringExtra(EXTRA_DAY_ID);
            final String formatStr = ctx.getResources().getString(R.string.toast_format_string);
            Toast.makeText(ctx, String.format(formatStr, day), Toast.LENGTH_SHORT).show();
        }

        super.onReceive(ctx, intent);
    }

    private RemoteViews buildLayout(Context context, int appWidgetId) {
        RemoteViews rv;
        // Specify the service to provide data for the collection widget.  Note that we need to
        // embed the appWidgetId via the data otherwise it will be ignored.
        final Intent intent = new Intent(context, WeatherWidgetService.class);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
        rv = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
        rv.setRemoteAdapter(R.id.weather_list, intent);

        // Set the empty view to be displayed if the collection is empty.  It must be a sibling
        // view of the collection view.
        rv.setEmptyView(R.id.weather_list, R.id.empty_view);

        // Bind a click listener template for the contents of the weather list.  Note that we
        // need to update the intent's data if we set an extra, since the extras will be
        // ignored otherwise.
        final Intent onClickIntent = new Intent(context, CourseProvider.class);
        onClickIntent.setAction(CourseProvider.CLICK_ACTION);
        onClickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        onClickIntent.setData(Uri.parse(onClickIntent.toUri(Intent.URI_INTENT_SCHEME)));
        final PendingIntent onClickPendingIntent = PendingIntent.getBroadcast(context, 0,
                onClickIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        rv.setPendingIntentTemplate(R.id.weather_list, onClickPendingIntent);

        // Bind the click intent for the refresh button on the widget
        final Intent refreshIntent = new Intent(context, CourseProvider.class);
        refreshIntent.setAction(CourseProvider.REFRESH_ACTION);
        final PendingIntent refreshPendingIntent = PendingIntent.getBroadcast(context, 0,
                refreshIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        rv.setOnClickPendingIntent(R.id.refresh, refreshPendingIntent);

        // Restore the minimal header
        rv.setTextViewText(R.id.city_name, context.getString(R.string.city_name));
        return rv;
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // Update each of the widgets with the remote adapter
        for (int appWidgetId : appWidgetIds) {
            //TODO METre A JOUr LE BORDEL
            RemoteViews layout = buildLayout(context, appWidgetId);
            appWidgetManager.updateAppWidget(appWidgetId, layout);

        }
        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager,
                                          int appWidgetId, Bundle newOptions) {

    }
}
