package com.jojinjohn.safepulse;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.VpnService;
import android.os.Build;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;
        AppSettings.migrateDefaults(context);
        if (!AppSettings.isBootStartEnabled(context)) return;
        if (VpnService.prepare(context) != null) return;

        Intent serviceIntent = new Intent(context, AdBlockVpnService.class)
                .setAction(AdBlockVpnService.ACTION_START);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }
    }
}
