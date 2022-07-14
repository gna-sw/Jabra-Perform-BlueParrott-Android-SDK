package com.mysay.blueparrottsdkaudiodemo;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;

import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.blueparrott.blueparrottsdk.BPHeadset;
import com.blueparrott.blueparrottsdk.BPHeadsetListener;
import com.blueparrott.blueparrottsdk.BPSdk;
import com.blueparrott.blueparrottsdk.IBPHeadsetListener;
/*
 * Service that maintains connection to the SDK/BlueParrott Button
 * Monitors for Headset Connection Changes and automatically connects to headset SDK
 *
 */
public class SdkConnectionService extends Service implements IBPHeadsetListener {

    private static final String LOGTAG = "BPAudioDemo.SdkConn";

    Handler handler = new Handler();

    //handle to the BPHeadet sdk instance
    private BPHeadset headsetSdk;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v(LOGTAG, "Starting Service");

        if (Utils.isBluetoothHeadsetConnected()) {
            Log.v(LOGTAG, "Headset is connected");

            if (!(headsetSdk.connected())) {

                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Log.v(LOGTAG, "About to connect to headsetSDK from onStartCommand");
                        headsetSdk.connect();
                    }
                }, 1000);//delay 1000 while bluetooth completes its business
            }
        } else {
            Log.v(LOGTAG, "No Headset is connected.");
        }

        return Service.START_STICKY;  //used for services that are expliictly started and stopped as needed//service will restart automatically
    }


    //when service is created (should only be once)
    @Override
    public void onCreate() {
        Log.v(LOGTAG, "onCreate()");
        super.onCreate();

        //get headsetsdk reference
        headsetSdk = (BPHeadset) BPSdk.getBPHeadset(this);
        Log.v(LOGTAG, "Headset is  " + headsetSdk.connected());

        //listen for headset events
        headsetSdk.addListener(SdkConnectionService.this);

        //listen for bluetooth state changes (when headset is connected, disconnected)
        IntentFilter intentFilter = new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED);
        registerReceiver(btReceiver, intentFilter);


    }

    @Override
    public void onDestroy() {
        headsetSdk.removeListener(SdkConnectionService.this);
        IntentFilter intentFilter = new IntentFilter(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED);
        unregisterReceiver(btReceiver);
    }


    @Override
    public void onConnectProgress(int progressCode) {
        Log.v(LOGTAG,getStatusDescription(progressCode));
    }

    @Override
    public void onConnect() {
        Log.d(LOGTAG, "onConnect()");
        headsetSdk.enableSDKMode();

        //we are connected so show that in the notification bar
        Utils.createNotification(this, RecPlayAudioService.AUDIO_STATE_IDLE);

    }

    @Override
    public void onConnectFailure(int reasonCode) {
        //connection has failed, ensure we clear the notification bar
        Log.d(LOGTAG,getConnectErrorDescription(reasonCode));
        Utils.cancelHeadsetStatusNotification(this);
    }


    @Override
    public void onDisconnect() {
        Log.d(LOGTAG, "onDisconnect()");
        //we are disconnected - clear notification bar
        Utils.cancelHeadsetStatusNotification(this);
    }

    @Override
    public void onModeUpdate() {

    }

    @Override
    public void onModeUpdateFailure(int reasonCode) {

    }

    @Override
    public void onButtonDown(int buttonId) {
        Log.d(LOGTAG, "ButtonDown");
    }

    @Override
    public void onButtonUp(int buttonId) {
        Log.d(LOGTAG, "ButtonUp");
    }

    @Override
    public void onTap(int buttonId) {

    }

    @Override
    public void onDoubleTap(int buttonId) {

    }

    @Override
    public void onLongPress(int buttonId) {

    }

    @Override
    public void onProximityChange(int status) {

    }

    @Override
    public void onValuesRead() {

    }

    @Override
    public void onEnterpriseValuesRead() {

    }



    /*
     * Detect Bluetooth Headset State change, so we can trigger connection to the BlueParrott Headset SDK when a headset connects
     */
    private final BroadcastReceiver btReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();
            Log.d(LOGTAG,"action="+action);

            switch (action) {
                //a headset has been connected
                case BluetoothDevice.ACTION_ACL_CONNECTED:
                    Log.v(LOGTAG, "BluetoothDevice.ACTION_ACL_CONNECTED");
                    if (headsetSdk.getConnectedState() == BluetoothProfile.STATE_DISCONNECTED) {
                        Log.v(LOGTAG, "Connecting to headsetSDK");
                        headsetSdk.connect();
                    }

                default:
                    break;
            }
        }
    };



    private String getConnectErrorDescription(int errorCode) {

        switch (errorCode) {
            case BPHeadsetListener.CONNECT_ERROR_UPDATE_ANDROID:
                return "Classic connection requires Android Kit Kat or greater";
            case BPHeadsetListener.CONNECT_ERROR_BLUETOOTH_NOT_AVAILABLE:
                return "Bluetooth Not Available - turn on Bluetooth";
            case BPHeadsetListener.CONNECT_ERROR_ALREADY_CONNECTED:
                return "BlueParrott Button already connected";
            case BPHeadsetListener.CONNECT_ERROR_ALREADY_CONNECTING:
                return "Already Connecting";
            case BPHeadsetListener.CONNECT_ERROR_NO_HEADSET_CONNECTED:
                return "No Headset Connected";
            case BPHeadsetListener.CONNECT_ERROR_HEADSET_NOT_SUPPORTED:
                return "Headset does not support BlueParrott Button";
            case BPHeadsetListener.CONNECT_ERROR_UPDATE_YOUR_FIRMWARE:
                return "Your headset firmware is out of date - Please update to the latest version";
            case BPHeadsetListener.CONNECT_ERROR_UPDATE_YOUR_SDK_APP:
                return "The BlueParrott SDK is out of date - please update to the latest version";
            case BPHeadsetListener.CONNECT_ERROR_HEADSET_DISCONNECTED:
                return "Headset disconnected unexpectedly";
            case BPHeadsetListener.CONNECT_ERROR_TIMEOUT:
                return "Timeout connecting to BlueParrott Button";
            case BPHeadsetListener.CONNECT_ERROR_BLE_REQUIRES_PERMISSION:
                return "BLE Connection requires Android Lollipop or greater";
            default:
                return "Unknown error connecting to BlueParrott Button";
        }

    }

    public String getStatusDescription(int progressCode) {

        switch (progressCode) {
            case BPHeadsetListener.PROGRESS_STARTED:
                return "Connection Process Started";
            case BPHeadsetListener.PROGRESS_FOUND_CLASSIC_HEADSET:
                return "Found Headset";
            case BPHeadsetListener.PROGRESS_REUSING_CONNECTION:
                return "Reusing existing connection";
            case BPHeadsetListener.PROGRESS_BLE_SCANNING:
                return "Scanning for BLE service";
            case BPHeadsetListener.PROGRESS_FOUND_BP_SERVICE:
                return "Found BlueParrott BLE service";
            case BPHeadsetListener.PROGRESS_CONNECTING_TO_BLE:
                return "Connecting to BLE service";
            case BPHeadsetListener.PROGRESS_READING_HEADSET_VALUES:
                return "Reading headset values";
            case BPHeadsetListener.PROGRESS_USING_BT_CLASSIC:
                return "Attempting connect over classic Bluetooth";
            default:
                return "Unknown status code";
        }

    }


    private String getUpdateErrorDescription(int errorCode) {

        switch (errorCode) {
            case BPHeadsetListener.UPDATE_ERROR_NOT_CONNECTED:
                return "Parrot Button is not connected - must call connect first";
            case BPHeadsetListener.UPDATE_ERROR_TIMEOUT:
                return "Error updating - timed out";
            case BPHeadsetListener.UPDATE_ERROR_WRITE_FAILED:
                return "Could not write to headset - BLE write failed";
            default:
                return "Unknown error updating BlueParrott Button";
        }

    }


}
