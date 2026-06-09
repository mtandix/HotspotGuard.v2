package com.example.hotspotguard;

import android.util.Log;

import rikka.shizuku.Shizuku;

public class HotspotController {

    private static final String TAG = "HotspotController";

    public static void turnOnHotspot() {
        executeCommand("cmd tethering start-tethering 0");
    }

    public static void turnOffHotspot() {
        executeCommand("cmd tethering stop-tethering 0");
    }

    private static void executeCommand(String command) {
        if (!Shizuku.pingBinder()) {
            Log.e(TAG, "Shizuku is not running");
            return;
        }

        try {
            Shizuku.newProcess(new String[]{"sh", "-c", command}, null, null).waitFor();
            Log.d(TAG, "Executed: " + command);
        } catch (Exception e) {
            Log.e(TAG, "Error executing command", e);
        }
    }
}
