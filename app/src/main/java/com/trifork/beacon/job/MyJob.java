package com.trifork.beacon.job;

import android.app.PendingIntent;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.trifork.beacon.receiver.BluetoothLEBroadcastReceiver;

/**
 * Created by KRK on 12/12/2017.
 */

public class MyJob extends JobService {
    private static final String TAG = "KRK";
    boolean jobCancelled = false;
    boolean needsReschedule = true;

    // Called by the Android system when it's time to run the job
    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        Log.d(TAG, "Job started!");
        // We need 'jobParameters' so we can call 'jobFinished'
        startWorkOnNewThread(jobParameters); // Services do NOT run on a separate thread

        return true;
    }

    private void startWorkOnNewThread(final JobParameters jobParameters) {
        new Thread(new Runnable() {
            public void run() {
                doWork(jobParameters);
            }
        }).start();
    }

    private void doWork(JobParameters jobParameters) {
        // If the job has been cancelled, stop working; the job will be rescheduled.
        if (jobCancelled)
            return;
        try {
            registerBLEReceiver();
        } catch (Exception e) { }

        Log.d(TAG, "Job finished!");
        jobFinished(jobParameters, needsReschedule);
    }

    // Called if the job was cancelled before being finished
    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        Log.d(TAG, "Job cancelled before being completed.");
        jobCancelled = true;
        jobFinished(jobParameters, needsReschedule);
        return needsReschedule;
    }

    private void registerBLEReceiver() {
        Log.d(TAG, "Registered BLEReceiver once again.");
        ScanSettings settings = (new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)).build();
        //List<ScanFilter> filters = getScanFilters(); // Make a scan filter matching the beacons I care about
        BluetoothManager bluetoothManager =
                (BluetoothManager) this.getApplicationContext().getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
        Intent intent = new Intent(this, BluetoothLEBroadcastReceiver.class);
        intent.putExtra("o-scan", true);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        bluetoothAdapter.getBluetoothLeScanner().startScan(null, settings, pendingIntent);
    }
}
