package com.jojinjohn.safepulse;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.VpnService;
import android.os.Build;
import android.widget.RemoteViews;

public class SafePulseWidget extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_safe_pulse);

            boolean running = AdBlockVpnService.isRunning();

            views.setTextViewText(R.id.widget_status, running ? "Protection ON" : "Protection OFF");
            views.setInt(R.id.widget_icon, "setImageResource", running ? R.drawable.ic_nav_shield : R.drawable.ic_notification);
            views.setInt(R.id.widget_container, "setBackgroundColor", running ? 0xFF4F46E5 : 0xFF64748B);

            Intent intent = new Intent(context, AdBlockVpnService.class);
            intent.setAction(running ? AdBlockVpnService.ACTION_STOP : AdBlockVpnService.ACTION_START);
            PendingIntent pendingIntent;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                pendingIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            } else {
                pendingIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            }
            views.setOnClickPendingIntent(R.id.widget_container, pendingIntent);

            Intent launchIntent = new Intent(context, MainActivity.class);
            PendingIntent launchPending = PendingIntent.getActivity(context, 1, launchIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            views.setOnClickPendingIntent(R.id.widget_open, launchPending);

            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }

    @Override
    public void onEnabled(Context context) {
    }

    @Override
    public void onDisabled(Context context) {
    }
}
