package com.mysay.blueparrottsdkaudiodemo;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 *
 * Enhanced Demonstration app for BlueParrott Headset SDK
 * Demonstrates typical life cycle for an audio app, in terms of connecting to the BlueParrott SDK
 * and recording and playing back audio over Bluetooth
 *
 * This activity is the Main User interface and integrates with background services to manage the
 * SDK/Connection to the BlueParrott Button, and also the audio recording and playback
 *
 * The app carries out the following functions to illustrate a typical cycle require for an audio/ptt app
 * - automatically maintain connection the BP Button whenever the phone boots or a headset connects
 * - automatically disconnect from the BP Button whenever the headset disconnects
 * - intercept button event Up/Down to trigger a simple 'echo' function - records some audio, while the button is pressed
 * and then plays it back when the button is released
 * - the app uses two services (one for the button connect management, one for the audio handling) and works in the
 * background or foreground.
 * - When in the foreground, the UI is updated to stay in synch with the Parrott Button
 * - In background, or foreground, the Notification bar will contain a Notification that indicates the current state
 * of the app (button connected, recording, playing etc.)
 */

public class MainActivity extends AppCompatActivity {

    static final public String LOGTAG = "BPSDKAudioDemo";


    BroadcastReceiver audioStateReceiver;

    TalkButton btnTalk;
    ActivityResultLauncher<String[]> mPermissionResultLauncher;
    boolean isScanPermissionGranted=false; //required for Android 12 if using BLE
    boolean isFineLocationGranted=false; // required for android 11 and before if using BLE
    boolean isConnectPermissionGranted=false; // required for Android 12 for both connection methods
    boolean isRecordPermissionGranted =false;

    boolean isWriteExternalGranted =false;
    /*
     * Create a connection for binding to the headset/button connection management service
     */
    private ServiceConnection mSdkServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
        }
        public void onServiceDisconnected(ComponentName className) {
        }
    };

    /*
     * Create a connection for binding to the audio (record/playback) service
     */
    public RecPlayAudioService mAudioService;
    private ServiceConnection mAudioServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mAudioService = ((RecPlayAudioService.AudioBinder) service).getService();
        }
        public void onServiceDisconnected(ComponentName className) { //FIXME does this ever get called ?
            mAudioService = null;
        }
    };


    /*
     * Set everything up
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        //set up the UI
        setContentView(R.layout.activity_main);


        //Set Up Permission Launcher
        mPermissionResultLauncher=registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), new ActivityResultCallback<Map<String, Boolean>>() {
            @Override
            public void onActivityResult(Map<String, Boolean> result) {
                if (result.get(Manifest.permission.BLUETOOTH_SCAN)!=null){
                    isScanPermissionGranted=result.get(Manifest.permission.BLUETOOTH_SCAN); //Only required for BLE connection
                };
                if (result.get(Manifest.permission.BLUETOOTH_CONNECT)!=null){
                    isConnectPermissionGranted=result.get(Manifest.permission.BLUETOOTH_CONNECT);
                };
                if (result.get(Manifest.permission.ACCESS_FINE_LOCATION)!=null){
                    isFineLocationGranted=result.get(Manifest.permission.ACCESS_FINE_LOCATION);//Only required for BLE connection on Pre Android 12 handsets
                }
                if (result.get(Manifest.permission.RECORD_AUDIO)!=null){
                    isRecordPermissionGranted=result.get(Manifest.permission.RECORD_AUDIO);
                }
                if (result.get(Manifest.permission.WRITE_EXTERNAL_STORAGE)!=null){
                    isWriteExternalGranted=result.get(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                }

            }
        });

        checkPermissions();

        btnTalk = findViewById(R.id.btnTalk);
        btnTalk.setText(Utils.getMessageForAudioState(RecPlayAudioService.AUDIO_STATE_IDLE));
        btnTalk.setBackground(getResources().getDrawable(Utils.getBackgroundDrawableForAudioState(RecPlayAudioService.AUDIO_STATE_IDLE)));

        // tell the audio service to start the record/play cycle
        btnTalk.setOnTouchListener(new View.OnTouchListener() {


            @Override

            public boolean onTouch(View view, MotionEvent motionEvent) {
                view.performClick();
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        mAudioService.startRecording();
                        break;
                    case MotionEvent.ACTION_UP:
                        mAudioService.stopRecording();
                        break;
                }
                return true;
            }
        });


        // when the audio service indicates a state change (playing, recording etc.), update the UI
        audioStateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int state = intent.getIntExtra(RecPlayAudioService.AUDIO_STATE_MESSAGE, RecPlayAudioService.AUDIO_STATE_IDLE);
                btnTalk.setText(Utils.getMessageForAudioState(state));
                btnTalk.setBackground(getResources().getDrawable(Utils.getBackgroundDrawableForAudioState(state)));
            }
        };


    }

    /*
     * Register for any audio state (play/record) state changes so we can update the UI
     */
    @Override
    protected void onStart() {
        super.onStart();

        //Register broadcast receiver to be notified from the AudioService when there is a change in the recording/playback state
        LocalBroadcastManager.getInstance(this).registerReceiver((audioStateReceiver), new IntentFilter(RecPlayAudioService.BP_AUDIO_STATE_BROADCAST));

    }

    /*
     * Unregister for any audio state (play/record) state changes
     */
    @Override
    protected void onStop() {

        //Unregister from the receiver that broadcasts recording/playback changes
        LocalBroadcastManager.getInstance(this).unregisterReceiver(audioStateReceiver);

        super.onStop();
    }

    /*
     * Bind to the SDK and Audio services
     */
    @Override
    public void onResume() {

        super.onResume();

        //start sdk connection service and bind to it
        Intent headsetServiceIntent = new Intent(this, SdkConnectionService.class);
        startService(headsetServiceIntent);
        bindService(new Intent(this, SdkConnectionService.class), mSdkServiceConnection, Context.BIND_AUTO_CREATE);


        //start audio service and bind to it
        Intent audioServiceIntent = new Intent(this, RecPlayAudioService.class);
        startService(audioServiceIntent);
        bindService(new Intent(this, RecPlayAudioService.class), mAudioServiceConnection, Context.BIND_AUTO_CREATE);

    }

    /*
     * Unbind from the services
     */
    @Override
    public void onPause() {
        super.onPause();

        // Detach our existing connection to the service
        unbindService(mSdkServiceConnection);
        unbindService(mAudioServiceConnection);

    }


    public void checkPermissions() {

        List<String> permissionRequest=new ArrayList<String>();
        isRecordPermissionGranted= ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED;

        if (!isRecordPermissionGranted) {
            permissionRequest.add(Manifest.permission.RECORD_AUDIO);
        }
        isWriteExternalGranted=ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)==PackageManager.PERMISSION_GRANTED;

        if (!isWriteExternalGranted) {
            permissionRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

            isScanPermissionGranted= ContextCompat.checkSelfPermission(this,
                    Manifest.permission.BLUETOOTH_SCAN)==PackageManager.PERMISSION_GRANTED;
            isConnectPermissionGranted= ContextCompat.checkSelfPermission(this,
                    Manifest.permission.BLUETOOTH_CONNECT)==PackageManager.PERMISSION_GRANTED;

            if (!isScanPermissionGranted) {
                //ONLY REQUIRED IF USING FORCE BLE OR HEADSET THAT DOES NOT SUPPORT AT COMMANDS
                permissionRequest.add(Manifest.permission.BLUETOOTH_SCAN);
            }

            if (!isConnectPermissionGranted) {
                permissionRequest.add(Manifest.permission.BLUETOOTH_CONNECT);
            }
        } else {
            //only required for BLE on 11
            isFineLocationGranted= ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED;
            if (!isFineLocationGranted) {
                permissionRequest.add(Manifest.permission.ACCESS_FINE_LOCATION);
            }
        }

        if (!permissionRequest.isEmpty()){
            mPermissionResultLauncher.launch(permissionRequest.toArray(new String[0]));
        }

    }

}
