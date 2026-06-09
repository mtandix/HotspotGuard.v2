package com.example.hotspotguard;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.telephony.TelephonyCallback;
import android.telephony.TelephonyDisplayInfo;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.ServiceCompat;

public class HotspotMonitorService extends Service {

    private static final String TAG = "HotspotMonitor";
    private static final String CHANNEL_ID = "HotspotMonitorChannel";
    public static boolean isRunning = false;

    private TelephonyManager telephonyManager;
    private TelephonyCallbackImpl telephonyCallback;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        isRunning = true;

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Hotspot Guard Running")
                .setContentText("Monitoring 5G network status...")
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentIntent(pendingIntent)
                .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceCompat.startForeground(this, 1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(1, notification);
        }

        registerNetworkCallback();

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isRunning = false;
        unregisterNetworkCallback();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void registerNetworkCallback() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            telephonyCallback = new TelephonyCallbackImpl();
            telephonyManager.registerTelephonyCallback(getMainExecutor(), telephonyCallback);
        }
    }

    private void unregisterNetworkCallback() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && telephonyManager != null && telephonyCallback != null) {
            telephonyManager.unregisterTelephonyCallback(telephonyCallback);
        }
    }

    class TelephonyCallbackImpl extends TelephonyCallback implements TelephonyCallback.DisplayInfoListener {
        @Override
        public void onDisplayInfoChanged(TelephonyDisplayInfo telephonyDisplayInfo) {
            checkNetworkType(telephonyDisplayInfo);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Hotspot Monitor Service Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }
    
    private void checkNetworkType(TelephonyDisplayInfo displayInfo) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            int overrideNetworkType = displayInfo.getOverrideNetworkType();
            int networkType = displayInfo.getNetworkType();
            
            Log.d(TAG, "Network type: " + networkType + ", override: " + overrideNetworkType);
            
            boolean is5G = (networkType == TelephonyManager.NETWORK_TYPE_NR) ||
                           (overrideNetworkType == TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_NSA) ||
                           (overrideNetworkType == TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_ADVANCED);
                           
            if (is5G) {
                HotspotController.turnOnHotspot();
            } else {
                HotspotController.turnOffHotspot();
            }
        }
    }
}
