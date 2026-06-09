package com.example.hotspotguard;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import rikka.shizuku.Shizuku;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_PERMISSIONS = 1001;
    private static final int SHIZUKU_REQUEST_CODE = 1002;

    private Button btnStart;
    private Button btnStop;
    private TextView statusText;

    private final Shizuku.OnRequestPermissionResultListener REQUEST_PERMISSION_RESULT_LISTENER = this::onRequestPermissionsResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnStart = findViewById(R.id.btnStart);
        btnStop = findViewById(R.id.btnStop);
        statusText = findViewById(R.id.statusText);

        Shizuku.addRequestPermissionResultListener(REQUEST_PERMISSION_RESULT_LISTENER);

        btnStart.setOnClickListener(v -> {
            if (checkPermissions()) {
                startMonitoringService();
            }
        });

        btnStop.setOnClickListener(v -> stopMonitoringService());
        
        updateUI(HotspotMonitorService.isRunning);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Shizuku.removeRequestPermissionResultListener(REQUEST_PERMISSION_RESULT_LISTENER);
    }

    private boolean checkPermissions() {
        if (!Shizuku.pingBinder()) {
            Toast.makeText(this, "Shizuku is not running!", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
            Shizuku.requestPermission(SHIZUKU_REQUEST_CODE);
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_CODE_PERMISSIONS);
                return false;
            }
        }
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
             ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_STATE}, REQUEST_CODE_PERMISSIONS);
             return false;
        }

        return true;
    }

    private void onRequestPermissionsResult(int requestCode, int grantResult) {
        if (grantResult == PackageManager.PERMISSION_GRANTED) {
            if (checkPermissions()) {
                startMonitoringService();
            }
        } else {
            Toast.makeText(this, "Shizuku Permission denied!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (checkPermissions()) {
                    startMonitoringService();
                }
            } else {
                Toast.makeText(this, "Permission denied!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startMonitoringService() {
        Intent serviceIntent = new Intent(this, HotspotMonitorService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
        updateUI(true);
    }

    private void stopMonitoringService() {
        Intent serviceIntent = new Intent(this, HotspotMonitorService.class);
        stopService(serviceIntent);
        updateUI(false);
    }

    private void updateUI(boolean isRunning) {
        if (isRunning) {
            statusText.setText("Status: Running");
            btnStart.setEnabled(false);
            btnStop.setEnabled(true);
        } else {
            statusText.setText("Status: Stopped");
            btnStart.setEnabled(true);
            btnStop.setEnabled(false);
        }
    }
}
