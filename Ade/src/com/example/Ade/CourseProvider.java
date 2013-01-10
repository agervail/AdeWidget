package com.example.Ade;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.widget.RemoteViews;
import android.widget.Toast;

public class CourseProvider extends AppWidgetProvider {
    public static String CLICK_ACTION = "com.example.android.weatherlistwidget.CLICK";
    public static String REFRESH_ACTION = "com.example.android.weatherlistwidget.REFRESH";
    public static String EXTRA_DAY_ID = "com.example.android.weatherlistwidget.day";

    private static HandlerThread sWorkerThread;
    private static Handler sWorkerQueue;


    private boolean mIsLargeLayout = true;

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
                    //A FAIRE EN CAS DE RECEPTION des DONNEES DE L

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

    private RemoteViews buildLayout(Context context, int appWidgetId, boolean largeLayout) {
        RemoteViews rv;
        // Specify the service to provide data for the collection widget.  Note that we need to
        // embed the appWidgetId via the data otherwise it will be ignored.
        final Intent intent = new Intent(context, WeatherWidgetService.class);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
        rv = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
        rv.setRemoteAdapter(appWidgetId, R.id.weather_list, intent);

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
        for (int i = 0; i < appWidgetIds.length; ++i) {
            //TODO METre A JOUr LE BORDEL
            RemoteViews layout = buildLayout(context, appWidgetIds[i], mIsLargeLayout);
            appWidgetManager.updateAppWidget(appWidgetIds[i], layout);

        }
        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager,
                                          int appWidgetId, Bundle newOptions) {

    }
}
