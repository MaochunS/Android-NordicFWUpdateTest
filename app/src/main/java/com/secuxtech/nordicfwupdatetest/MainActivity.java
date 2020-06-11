package com.secuxtech.nordicfwupdatetest;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.ActivityManager;
import android.app.LoaderManager;
import android.app.NotificationManager;
import android.bluetooth.BluetoothManager;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.DocumentsContract;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import java.io.File;

import no.nordicsemi.android.dfu.DfuProgressListener;
import no.nordicsemi.android.dfu.DfuProgressListenerAdapter;
import no.nordicsemi.android.dfu.DfuServiceController;
import no.nordicsemi.android.dfu.DfuServiceInitiator;
import no.nordicsemi.android.dfu.DfuServiceListenerHelper;

public class MainActivity extends AppCompatActivity
{
    private static final int SELECT_FILE_REQ = 1;
    private static final int SELECT_INIT_FILE_REQ = 2;
    private final String TAG = "NotdicFWBurningTest";

    private String  mFilePath;
    private Uri     mFileStreamUri;

    private final DfuProgressListener dfuProgressListener = new DfuProgressListenerAdapter() {
        @Override
        public void onDeviceConnecting(@NonNull final String deviceAddress) {
            Log.i(TAG, "dfu_status_connecting");
        }

        @Override
        public void onDfuProcessStarting(@NonNull final String deviceAddress) {

            Log.i(TAG, "dfu_status_starting");
        }

        @Override
        public void onEnablingDfuMode(@NonNull final String deviceAddress) {

            Log.i(TAG, "dfu_status_switching_to_dfu");
        }

        @Override
        public void onFirmwareValidating(@NonNull final String deviceAddress) {

            Log.i(TAG, "dfu_status_validating");
        }

        @Override
        public void onDeviceDisconnecting(@NonNull final String deviceAddress) {

            Log.i(TAG, "dfu_status_disconnecting");
        }

        @Override
        public void onDfuCompleted(@NonNull final String deviceAddress) {

            Log.i(TAG, "dfu_status_completed");
        }

        @Override
        public void onDfuAborted(@NonNull final String deviceAddress) {

            Log.i(TAG, "dfu_status_abort");
        }

        @Override
        public void onProgressChanged(@NonNull final String deviceAddress, final int percent,
                                      final float speed, final float avgSpeed,
                                      final int currentPart, final int partsTotal) {

            Log.i(TAG, "dfu_status_inprogress " + percent);
        }

        @Override
        public void onError(@NonNull final String deviceAddress, final int error, final int errorType, final String message) {
            Log.i(TAG, "dfu_status_error " + message);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android M Permission check
            if (this.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }
        }

        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {


        }else{
            Toast.makeText(this, "The phone DOES NOT support BLE!", Toast.LENGTH_SHORT).show();
            finish();
        }

        DfuServiceInitiator.createDfuNotificationChannel(this);
        DfuServiceListenerHelper.registerProgressListener(this, dfuProgressListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        DfuServiceListenerHelper.unregisterProgressListener(this, dfuProgressListener);
    }

    private void openFileChooser() {
        final Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType(DfuService.MIME_TYPE_ZIP);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            // file browser has been found on the device
            startActivityForResult(intent, 1);
        }

    }

    private void updateFW(String address, String devName){
        final DfuServiceInitiator starter = new DfuServiceInitiator(address)
                .setDeviceName(devName)
                .setKeepBond(true);

        starter.setUnsafeExperimentalButtonlessServiceInSecureDfuEnabled(true);

        starter.setPrepareDataObjectDelay(300L);
        starter.setZip(mFileStreamUri, null);

        final DfuServiceController controller = starter.start(this, DfuService.class);
    }

    public void onButtonClick(View v) {
        openFileChooser();
    }


    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK)
            return;

        switch (requestCode) {
            case SELECT_FILE_REQ: {

                final Uri uri = data.getData();
                mFileStreamUri = uri;

                updateFW("C0:35:A3:F0:FA:70", "DfuTarg");

                break;
            }

            default:
                break;
        }
    }

}
