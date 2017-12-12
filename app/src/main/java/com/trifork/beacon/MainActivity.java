package com.trifork.beacon;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanSettings;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.util.Log;

import com.trifork.beacon.job.MyJob;
import com.trifork.beacon.receiver.BluetoothLEBroadcastReceiver;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    private static final String UUID = "ab4ee505-2afc-47f5-b5a6-448a034023ec";
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    private String TAG = "KRK";
    private Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = this;

        requestGPS();
        registerBLEReceiver();
        registerJob();
    }

    private void registerJob() {
        ComponentName componentName = new ComponentName(this, MyJob.class);
        JobInfo jobInfo = new JobInfo.Builder(12, componentName)
                .setPersisted(true) // Runs across device reboot
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY) // TODO: (KRK) Find another alternative to this permission
                .build();

        JobScheduler jobScheduler = (JobScheduler)getSystemService(JOB_SCHEDULER_SERVICE);
        int resultCode = jobScheduler.schedule(jobInfo);
        if (resultCode == JobScheduler.RESULT_SUCCESS) {
            Log.d(TAG, "Job scheduled!");
        } else {
            Log.d(TAG, "Job not scheduled");
        }
    }

    private void registerBLEReceiver() {
        ScanSettings settings = (new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)).build();
        List<ScanFilter> filters = getScanFilters(); // Make a scan filter matching the beacons I care about
        BluetoothManager bluetoothManager =
                (BluetoothManager) mContext.getApplicationContext().getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
        Intent intent = new Intent(mContext, BluetoothLEBroadcastReceiver.class);
        intent.putExtra("o-scan", true);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(mContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        bluetoothAdapter.getBluetoothLeScanner().startScan(null, settings, pendingIntent);
        Log.d(TAG, "Registered BLEReceiver.");
    }

    private List<ScanFilter> getScanFilters() {
        ArrayList<ScanFilter> filters = new ArrayList<>();
        ScanFilter scanFilter = new ScanFilter.Builder().setServiceUuid(ParcelUuid.fromString(UUID)).build();
        filters.add(scanFilter);
        return filters;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void requestGPS() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android M Permission check
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("This app needs location access");
                builder.setMessage("Please grant location access so this app can detect beacons.");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
                    }
                });
                builder.show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "coarse location permission granted");
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Functionality limited");
                    builder.setMessage("Since location access has not been granted, this app will not be able to discover beacons when in the background.");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                        @Override
                        public void onDismiss(DialogInterface dialog) {
                        }

                    });
                    builder.show();
                }
                return;
            }
        }
    }
}

