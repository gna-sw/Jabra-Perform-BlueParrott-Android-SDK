package com.blueparrott.connectsample;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
//import android.support.v7.app.AppCompatActivity;
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
import java.util.Calendar;

import static com.blueparrott.blueparrottsdk.BPHeadsetListener.CONNECT_ERROR_BLE_REQUIRES_PERMISSION;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //set up the UI elements
        setContentView(R.layout.activity_main);
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
        if (reasonCode == CONNECT_ERROR_BLE_REQUIRES_PERMISSION) {
            Log.d(LOGTAG, "ble permission required");
            logStatus("BLE permission required");
            getBLEPermission();//and resume which will kick things off
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


    /*
     * If supporting older BLE only headsets (B350, B450, C400), you must include a request for ACCESS_FINE_LOCATION
     * For newer headsets supporting the AT commands firmware, you do NOT need any special permissions here
     */
    public boolean getBLEPermission() {
        // Send a request to request permissions from the user and return in the Activity's onRequestPermissionsResult()
        //ONLY required for older BP headsets that connect via BLE - remove if only targetting headsets that connect over the classic connection (AT commands)
        //Note that in this demonstration app, it tries to connect without the permission (using AT commands) and only if it fails, will it automatically prompt the user
        String[] BLE_PERMISSIONS = {Manifest.permission.ACCESS_FINE_LOCATION};

        boolean showRationale = shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION);
        if (showRationale) {
            Toast.makeText(this, "Permission was denied already, leaving you alone", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, BLE_PERMISSIONS, 1);
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
        for (int i = 0; i < permissions.length; i++) {
            String permission = permissions[i];
            int grantResult = grantResults[i];
            if (permission.equals(Manifest.permission.ACCESS_FINE_LOCATION)) {
                if (grantResult == PackageManager.PERMISSION_GRANTED) {
                    //do nothing
                    Log.d(LOGTAG, "Permission granted for location");
                } else {
                    everythingisOK = false;
                    Toast.makeText(this, "Cannot target older headsets that use BLE only", Toast.LENGTH_LONG).show();
                    logStatus("Error: Headset is BLE only");
                }
            }
        }
        if (!everythingisOK) {
            Toast.makeText(this, "Go to App Settings and grant 'location' permission - required for Bluetooth connection to BlueParrott Button", Toast.LENGTH_LONG).show();
        }
    }

}
