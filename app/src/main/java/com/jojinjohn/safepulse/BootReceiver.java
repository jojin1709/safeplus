package com.jojinjohn.safepulse;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.VpnService;
import android.os.Build;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "SafePulseBoot";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null) return;

        boolean isBoot = Intent.ACTION_BOOT_COMPLETED.equals(action)
                || "android.intent.action.QUICKBOOT_POWERON".equals(action);

        if (!isBoot) return;

        AppSettings.migrateDefaults(context);
        if (!AppSettings.isBootStartEnabled(context)) return;

        try {
            Intent serviceIntent = new Intent(context, AdBlockVpnService.class)
                    .setAction(AdBlockVpnService.ACTION_START);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to start VPN service on boot", e);
        }
    }
}
