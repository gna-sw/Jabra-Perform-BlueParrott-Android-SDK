package com.blueparrott.connectsample;

import android.app.Notification;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.blueparrott.blueparrottsdk.BPHeadset;
import com.blueparrott.blueparrottsdk.BPSdk;
import com.blueparrott.blueparrottsdk.IBPHeadsetListener;

/*
 * Service that maintains connection to the SDK/BlueParrott Button
 * Monitors for Headset Connection Changes and automatically connects to headset SDK
 *
 */
public class SdkConnectionService extends Service implements IBPHeadsetListener {
    private static final String LOGTAG = "BPHeadsetSDCConnSvc";
    public static final int BUTTON_STATE_DISCONNECTED = 0;
    public static final int BUTTON_STATE_CONNECTING = 1;
    public static final int BUTTON_STATE_CONNECTED = 2;
    public static final int BUTTON_STATE_DOWN = 3;
    public static final int BUTTON_STATE_UP = 4;

    //handle to the BPHeadet sdk instance
    private BPHeadset headsetSdk;

    public class SDKBinder extends Binder {
        public SdkConnectionService getService() {
            return SdkConnectionService.this;
        }
    }

    private final SDKBinder localBinder = new SDKBinder();//create binder

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return localBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v(LOGTAG, "Starting Service");

        //Is there a bluetooth headset connected ? if so consider doing SDK connect.
        if (Utils.isBluetoothHeadsetConnected()) {
            Log.v(LOGTAG, "Headset is connected");

            //when services launches if the SDK is not already connected, connect
            if (!(headsetSdk.connected())) {
                Log.v(LOGTAG, "About to connect to headsetSDK from onStartCommand");
                headsetSdk.connect();
            }
        } else {
            Log.v(LOGTAG, "No Headset is connected.");
        }

        return Service.START_STICKY;  //used for services that are expliictly started and stopped as needed//service will restart automatically
    }

    private static final int HEADSET_NOTIFICATION_ID = 100;

    //when service is created (should only be once)
    @Override
    public void onCreate() {
        super.onCreate();

        //get headsetsdk reference
        headsetSdk = (BPHeadset) BPSdk.getBPHeadset(this);

        //listen for headset events
        headsetSdk.addListener(SdkConnectionService.this);

        //listen for bluetooth state changes (when headset is connected, disconnected)
        IntentFilter intentFilter = new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED);
        registerReceiver(btReceiver, intentFilter);

        //ensure this service is started in Foreground (required for Android OREO and above, otherwise service gets taken down
        Notification n = Utils.buildNotification(this, BUTTON_STATE_DISCONNECTED);
        startForeground(HEADSET_NOTIFICATION_ID, n);
    }

    @Override
    public void onDestroy() {

        Log.d(LOGTAG,"Destroying SdkConnectionService");
        //tidy up - remove listener
        headsetSdk.removeListener(SdkConnectionService.this);

        //remove the receiver for bluetooth headset connect state changes
        unregisterReceiver(btReceiver);
    }

    @Override
    public void onConnectProgress(int progressCode) {

        //during connect cycle (e.g. steps in BLE connect or Classic Connect, optionally update the user with progress
        Log.v(LOGTAG, Utils.getStatusDescription(progressCode));
        Utils.createNotification(this, BUTTON_STATE_CONNECTING);
    }

    @Override
    public void onConnect() {

    }

    @Override
    public void onConnectFailure(int reasonCode) {

        //connection has failed, ensure we clear the notification bar
        Log.d(LOGTAG, Utils.getConnectErrorDescription(reasonCode));
        Utils.cancelHeadsetStatusNotification(this);
    }


    @Override
    public void onDisconnect() {
        Log.d(LOGTAG, "onDisconnect()");
        Utils.createNotification(this, BUTTON_STATE_DISCONNECTED);
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
        //Let User know button has been pressed by updating the notification bar
        Utils.createNotification(this, BUTTON_STATE_DOWN);
//        sendtoKodiak(true);
        doBroadCast(false);
    }

    @Override
    public void onButtonUp(int buttonId) {
        Log.d(LOGTAG, "ButtonUp");
//        //Let User know button has been released by updating the notification bar
        Utils.createNotification(this, BUTTON_STATE_UP);
//        sendtoKodiak(false);
        doBroadCast(true);

    }

    private void doBroadCast(boolean up){
        String intentAction=up?"com.blueparrott.action.BUTTON_UP":"com.blueparrott.action.BUTTON_DOWN";
        Intent i = new Intent(intentAction);
        sendBroadcast(i);
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

        //when successfully connected to SDK, ensure we are in SDK mode so we are notified of SDK button press events
        headsetSdk.enableSDKMode();

        //we are connected so show that in the notification bar
        Utils.createNotification(this, BUTTON_STATE_CONNECTED);
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


}
