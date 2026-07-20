package com.jojinjohn.safepulse;

import android.content.Intent;
import android.net.VpnService;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

public class SafePulseTileService extends TileService {

    @Override
    public void onStartListening() {
        super.onStartListening();
        updateTileState();
    }

    @Override
    public void onClick() {
        super.onClick();

        if (AdBlockVpnService.isRunning()) {
            Intent stopIntent = new Intent(this, AdBlockVpnService.class)
                    .setAction(AdBlockVpnService.ACTION_STOP);
            startService(stopIntent);
        } else {
            if (VpnService.prepare(this) != null) {
                Intent settingsIntent = new Intent(this, MainActivity.class);
                settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivityAndCollapse(settingsIntent);
                return;
            }
            Intent startIntent = new Intent(this, AdBlockVpnService.class)
                    .setAction(AdBlockVpnService.ACTION_START);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(startIntent);
            } else {
                startService(startIntent);
            }
        }

        updateTileState();
    }

    private void updateTileState() {
        Tile tile = getQsTile();
        if (tile == null) return;

        boolean running = AdBlockVpnService.isRunning();
        tile.setState(running ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        tile.setLabel(running ? "SafePulse ON" : "SafePulse OFF");
        tile.setSubtitle(running ? "Blocking active" : "Protection off");
        tile.updateTile();
    }
}
