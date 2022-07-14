package com.blueparrott.connectsample;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import androidx.core.content.ContextCompat;

import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.blueparrott.blueparrottsdk.BPHeadset;
import com.blueparrott.blueparrottsdk.BPHeadsetListener;
import com.blueparrott.blueparrottsdk.BPSdk;
import com.blueparrott.blueparrottsdk.IBPHeadsetListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import static com.blueparrott.blueparrottsdk.BPHeadsetListener.CONNECT_ERROR_BLE_REQUIRES_PERMISSION;
import static com.blueparrott.blueparrottsdk.BPHeadsetListener.CONNECT_ERROR_CLASSIC_REQUIRES_PERMISSION;

/**
 * Sample Application that automatically connects to the Blueparrott SDK when a new headset is detected
 * Maintains connection when app is in foreground or background
 * MainActivity UI gets updated from BPHeadsetListener events
 * Notification bar will get updated from BPHeadsetListener in the SKdkConnectionService
 *
 */
public class MainActivity extends AppCompatActivity implements IBPHeadsetListener {
    static final public String LOGTAG = "BPConnectSample";
    private TextView textView;
    private BPHeadset headsetSdk;
    TextView tvLog;
    ScrollView scScrollView;

    ActivityResultLauncher<String[]> mPermissionResultLauncher;
    boolean isScanPermissionGranted=false; //required for Android 12 if using BLE
    boolean isFineLocationGranted=false; // required for android 11 and before if using BLE
    boolean isConnectPermissionGranted=false; // required for Android 12 for both connection methods


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //set up the UI elements
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

            }
        });

        checkPermissions();

        textView = findViewById(R.id.btnTalk);
        tvLog = findViewById(R.id.tvLog);
        tvLog.setMovementMethod(new ScrollingMovementMethod());
        scScrollView = (ScrollView) this.findViewById(R.id.scScrollView);

        //Get handle to the blueparrott SDK and create a listener for headset events
        headsetSdk = (BPHeadset) BPSdk.getBPHeadset(this);
        headsetSdk.addListener(this);
    }

    @Override
    protected void onStart () {
        super.onStart();
        //update the UI - it may have changed while we are in the background
        if(headsetSdk.connected()){
            textView.setText(R.string.button_state_connected);
        }
    }

    @Override
    protected void onDestroy() {
        //stop listening for headset events
        headsetSdk.removeListener(this);
        super.onDestroy();
    }

    /*
     * Bind to the SDK
     */
    @Override
    public void onResume() {
        super.onResume();
        //start sdk connection service and bind to it
        Intent headsetServiceIntent = new Intent(this, SdkConnectionService.class);
        startService(headsetServiceIntent);
    }


    @Override
    public void onConnectProgress(int progressCode) {
        textView.setText(R.string.button_state_connecting);
        logStatus("Connecting : code: " + progressCode);
    }

    @Override
    public void onConnect() {
        textView.setText(R.string.button_state_connected);
        logStatus("Connected");
    }

    @Override
    public void onConnectFailure(int reasonCode) {
        if( (reasonCode == CONNECT_ERROR_BLE_REQUIRES_PERMISSION) ||  (reasonCode == CONNECT_ERROR_CLASSIC_REQUIRES_PERMISSION))  {
            Log.d(LOGTAG, "Missing permissions required");
            logStatus("Bluetooth permissions required");
            checkPermissions();//and resume which will kick things off
        } else {
            logStatus(Utils.getConnectErrorDescription(reasonCode));


        }
    }

    @Override
    public void onDisconnect() {
        textView.setText(R.string.button_state_disconnected);
        logStatus("Disconnected");
    }

    @Override
    public void onModeUpdate() {

    }

    @Override
    public void onModeUpdateFailure(int reasonCode) {

    }

    @Override
    public void onButtonDown(int buttonId) {
        textView.setText(R.string.button_state_down);
        logStatus("Button Pressed");
    }

    @Override
    public void onButtonUp(int buttonId) {
        textView.setText(R.string.button_state_up);
        logStatus("Button Released");

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
        logStatus("Proximity Change ="+status);

    }

    @Override
    public void onValuesRead() {

    }

    @Override
    public void onEnterpriseValuesRead() {

    }


    /*
     * Log progress to the to Screen
     */
    public void logStatus(String s) {
        String time = new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTime());
        tvLog.append(time + " " + s + "\n");
        scScrollView.post(new Runnable() {
            @Override
            public void run() {
                scScrollView.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });
    }


    public void checkPermissions() {

        List<String> permissionRequest=new ArrayList<String>();

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
