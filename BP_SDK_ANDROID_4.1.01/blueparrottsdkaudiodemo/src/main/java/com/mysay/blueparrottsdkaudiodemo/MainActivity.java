package com.mysay.blueparrottsdkaudiodemo;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;



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
    int PERMISSION_ALL = 1;
    TalkButton btnTalk;

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

        getPermission();
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

    /*
    * Android now requires prompting the user for permissions
    * If using older BLE only headsets, you must include a request for ACCESS_FINE_LOCATION
    * For newer headsets supporting the AT commands firmware, you do NOT need any special permissions here
    */
    public boolean getPermission() {
        // Send a request to request permissions from the user and return in the Activity's onRequestPermissionsResult()

        String[] PERMISSIONS = {
                Manifest.permission.RECORD_AUDIO,           //ONLY required for audio recording - not required for BP button to work
                Manifest.permission.WRITE_EXTERNAL_STORAGE, //ONLY required for writing to temp audio file - not require for BP button to work
                Manifest.permission.ACCESS_FINE_LOCATION            //ONLY required for older BP headsets that connect via BLE - remove if only targetting headsets that connect over the
        };

        if (!Utils.hasPermissions(this, PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
            return false;
        }

        return true;

    }

    /**
     * Return from the permision request UI and handle any issues
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean everythingisOK = true;
        if (requestCode == PERMISSION_ALL) {
            for (int i = 0; i < permissions.length; i++) {
                String permission = permissions[i];
                int grantResult = grantResults[i];

                if (permission.equals(Manifest.permission.RECORD_AUDIO)) {
                    if (grantResult == PackageManager.PERMISSION_GRANTED) {
                        Log.d(LOGTAG, "Permission granted for audio");
                    } else {
                        Toast.makeText(this, "Cannot record audio", Toast.LENGTH_LONG).show();
                        everythingisOK = false;
                    }
                } else if (permission.equals(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    if (grantResult == PackageManager.PERMISSION_GRANTED) {
                        Log.d(LOGTAG, "Permission granted for storage");

                    } else {
                        Toast.makeText(this, "Permission is required - cannot write audio to file", Toast.LENGTH_LONG).show();
                        everythingisOK = false;
                    }
                } else if (permission.equals(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    if (grantResult == PackageManager.PERMISSION_GRANTED) {
                        //do nothing
                        Log.d(LOGTAG, "Permission granted for location");
                    } else {
                        everythingisOK = false;
                        Toast.makeText(this, "Cannot target older headsets that use BLE only", Toast.LENGTH_LONG).show();
                    }
                }
            }
            if (!everythingisOK)
                Toast.makeText(this, "Go to App Settings and grant permissions.", Toast.LENGTH_LONG).show();
        }

    }

}
