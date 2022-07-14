package com.blueparrott.blueparrottsdkdemo;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import com.blueparrott.blueparrottsdk.BPHeadset;
import com.blueparrott.blueparrottsdk.BPHeadsetListener;
import com.blueparrott.blueparrottsdk.BPSdk;
import com.blueparrott.blueparrottsdk.IBPHeadsetListener;
import com.vxisdk.blueparrottsdkdemo.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import static android.graphics.Color.parseColor;

/**
 * Demonstration app for BlueParrott Headset SDK
 * Connects to headset, enables the SDK and adds a listener to respond to events
 * This is a simple demo.  In practice, for PTT and other apps, you should consider
 * a background Service to retain the connection to the BPHeadset and respond to its
 * events
 */

public class BpSDKDemo extends AppCompatActivity { //implements BPHeadsetListener {

    BPHeadset headset;
    TextView tvConnectStatus;
    Button btnConnect;
    TextView tvSDKStatus;
    Button btnEnableSDK;
    Button btnSetCustomMode;
    ScrollView scScrollView;
    Spinner spConnectMethod;
    static final public String LOGTAG = "BlueParrottSDKDemo";

    TextView tvLog;

    ActivityResultLauncher<String[]> mPermissionResultLauncher;
    boolean isScanPermissionGranted=false; //required for Android 12 if using BLE
    boolean isFineLocationGranted=false; // required for android 11 and before if using BLE
    boolean isConnectPermissionGranted=false; // required for Android 12 for both connection methods

    LinearLayout llButtons;


    TextView tvEntKey;
    TextView tvEntValue;
    Button btnGetEntSetting;
    Button btnGetallEntSettings;
    Button btnSetEntSetting;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bp_sdkdemo);

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

        //get a handle to the headset
        headset = BPSdk.getBPHeadset(this);

        //add a BPHeadsetListener
        headset.addListener(headsetListener);

        //a spinner containing three connection options (0=auto, 1=classic, 2=ble)
        spConnectMethod= (Spinner) findViewById(R.id.connect_method_spinner);

        //setup the ui for connection/disconnection
        tvConnectStatus = (TextView) findViewById(R.id.tvConnectStatus);
        btnConnect = (Button) findViewById(R.id.btnConnection);
        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!headset.connected()) {
                    logStatus("Connecting");
                    headset.connect(spConnectMethod.getSelectedItemPosition());//0=auto, 1=force classic, 2=force ble
                } else {
                    logStatus("Disconnecting..");
                    headset.disconnect();
                }
            }

        });


        //setup the ui for sdk enable/disable
        tvSDKStatus = (TextView) findViewById(R.id.tvSDKStatus);
        btnEnableSDK = (Button) findViewById(R.id.btnSDK);
        btnEnableSDK.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (!headset.sdkModeEnabled()) {
                    logStatus("Enabling SDK...");
                    headset.enableSDKMode();
                } else {
                    logStatus("Disabling SDK...");
                    headset.disableSDKMode();
                }
            }
        });


        btnSetCustomMode = (Button) findViewById(R.id.btnSetCustomMode);
        btnSetCustomMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!headset.connected()){
                    logStatus("Headset sdk must be connected");
                    return;
                }
                TextView tvCustomMode=(TextView)findViewById(R.id.etCustomModeNumber);
                int mode=Integer.parseInt(tvCustomMode.getText().toString());
                headset.setCustomMode(mode);
            }
        });



        tvLog = (TextView) findViewById(R.id.tvLog);
        tvLog.setMovementMethod(new ScrollingMovementMethod());

        llButtons = (LinearLayout) this.findViewById(R.id.rlButtons);
        scScrollView = (ScrollView) this.findViewById(R.id.scScrollView);

        setButtons();
        logStatus("SDK Version : " + BPSdk.version());


        tvEntKey=findViewById(R.id.etEntKey);
        tvEntValue=findViewById(R.id.etEntValue);
        btnSetEntSetting=findViewById(R.id.btnSetEntSetting);
        btnGetEntSetting=findViewById(R.id.btnGetEntSetting);
        btnGetallEntSettings=findViewById(R.id.btnGetAllEntSettings);

        //read map of all enterprise config values from the headset
        btnGetallEntSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!headset.connected()){
                    logStatus("Headset sdk must be connected");
                    return;
                }
                logStatus("Ent Settings\n"+headset.getConfigValues());
            }
        });

        //read an enterprise config value from the headset
        btnGetEntSetting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!headset.connected()){
                    logStatus("Headset sdk must be connected");
                    return;
                }
                String sKey=tvEntKey.getText().toString();
                if (sKey.isEmpty()){
                    logStatus("You must enter a key");
                    return;
                }
                Integer iKey=Integer.decode(sKey);
                logStatus("Config Value "+iKey+" is "+headset.getConfigValue(iKey));
            }
        });

        //set an enterprise config value on the headset
        btnSetEntSetting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!headset.connected()){
                    logStatus("Headset sdk must be connected");
                    return;
                }
                String sKey=tvEntKey.getText().toString();
                String sValue=tvEntValue.getText().toString();
                if (sKey.isEmpty()){
                    logStatus("You must enter a key");
                    return;
                }
                if (sValue.isEmpty()){
                    logStatus("You must enter a value");
                    return;
                }
                Integer iKey=Integer.decode(sKey);
                logStatus("Setting Key "+iKey+" = " +sValue);
                headset.setConfigValue(iKey,sValue);
            }
        });



    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {

            case R.id.action_toggle_config:
                RelativeLayout relativeLayout=findViewById(R.id.rlEnterpriseSDK);
                relativeLayout.setVisibility(relativeLayout.getVisibility()==View.GONE?View.VISIBLE:View.GONE);
                TextView tvCaption = findViewById(R.id.tvEntCaption);
                tvCaption.setVisibility(relativeLayout.getVisibility());
                item.setTitle(relativeLayout.getVisibility()==View.GONE?"Show Config":"Hide Config");
                return true;

            case R.id.action_toggle_custommode:
                RelativeLayout rlCustomMode=findViewById(R.id.rlCustomMode);
                rlCustomMode.setVisibility(rlCustomMode.getVisibility()==View.GONE?View.VISIBLE:View.GONE);
                TextView tvCustomModeCaption = findViewById(R.id.tvCustomModeCaption);
                tvCustomModeCaption.setVisibility(rlCustomMode.getVisibility());
                item.setTitle(rlCustomMode.getVisibility()==View.GONE?"Show Custom Mode":"Hide Custom Mode");
                return true;


            case R.id.action_clear_log:
                tvLog.setText("");
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void setButtons() {

        if (headset.connected()) {
            tvConnectStatus.setText("Connected");
            spConnectMethod.setEnabled(false);
            btnConnect.setText("Disconnect");
            btnEnableSDK.setEnabled(true);
        } else {
            tvConnectStatus.setText("Disconnected");
            spConnectMethod.setEnabled(true);
            btnConnect.setText("Connect");
            tvSDKStatus.setText("");
            btnEnableSDK.setEnabled(false);
        }

        if (!headset.sdkModeEnabled()) {
            tvSDKStatus.setText("SDK Disabled");
            btnEnableSDK.setText("Enable SDK");
        } else {
            tvSDKStatus.setText("SDK Enabled");
            btnEnableSDK.setText("Disable SDK");
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (headset != null) {
            headset.disconnect();
        }

    }

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





    private String getConnectErrorDescription(int errorCode) {

        switch (errorCode) {
            case BPHeadsetListener.CONNECT_ERROR_UPDATE_ANDROID:
                return "Classic connection requires Android Kit Kat or greater";
            case BPHeadsetListener.CONNECT_ERROR_BLUETOOTH_NOT_AVAILABLE:
                return "Bluetooth Not Available - turn on Bluetooth";
            case BPHeadsetListener.CONNECT_ERROR_ALREADY_CONNECTED:
                return "Parrott Button already connected";
            case BPHeadsetListener.CONNECT_ERROR_ALREADY_CONNECTING:
                return "Already Connecting";
            case BPHeadsetListener.CONNECT_ERROR_NO_HEADSET_CONNECTED:
                return "No Headset Connected";
            case BPHeadsetListener.CONNECT_ERROR_HEADSET_NOT_SUPPORTED:
                return "Headset does not support Parrott Button";
            case BPHeadsetListener.CONNECT_ERROR_UPDATE_YOUR_FIRMWARE:
                return "Your headset firmware is out of date - Please update to the latest version";
            case BPHeadsetListener.CONNECT_ERROR_UPDATE_YOUR_SDK_APP:
                return "The BlueParrott SDK is out of date - please update to the latest version";
            case BPHeadsetListener.CONNECT_ERROR_HEADSET_DISCONNECTED:
                return "Headset disconnected unexpectedly";
            case BPHeadsetListener.CONNECT_ERROR_TIMEOUT:
                return "Timeout connecting to Parrott Button";
            case BPHeadsetListener.CONNECT_ERROR_BLE_REQUIRES_PERMISSION:
                return "BLE Connection requires Fine_Location or Scan Permission";
            case BPHeadsetListener.CONNECT_ERROR_CLASSIC_REQUIRES_PERMISSION:
                return "Classic Connection requires Android Bluetooth Connect Permission";
            default:
                return "Unknown error connecting to Parrott Button";
        }
    }


    public String getStatusDescription(int errorCode) {

        switch (errorCode) {
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
                return "Unknown error updatingParrott Button";
        }

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


    IBPHeadsetListener headsetListener = new BPHeadsetListener(){
        @Override
        public void onConnect() {
            logStatus("Connected");
        }

        @Override
        public void onConnectProgress(int progressCode) {
            logStatus(getStatusDescription(progressCode));
        }

        @Override
        public void onConnectFailure(int errorCode) {
            logStatus("Connection Failed");
            logStatus("Error: " + getConnectErrorDescription(errorCode));
            logStatus("Retry or turn headset off then on");
            setButtons();
        }

        @Override
        public void onDisconnect() {
            logStatus("Disconnected");
            setButtons();
        }

        @Override
        public void onModeUpdate() {
            setButtons();
            logStatus("Mode Updated:"+headset.getMode());
        }

        @Override
        public void onModeUpdateFailure (int reasonCode){
            setButtons();
            logStatus("Mode Update Failed. Reason" +getUpdateErrorDescription(reasonCode));
        }

        @Override
        public void onButtonDown(int buttonId) {
            logStatus("Button Down");
            llButtons.setBackgroundColor(parseColor("#009900"));
        }

        @Override
        public void onButtonUp(int buttonId) {
            logStatus("Button Up");
            llButtons.setBackgroundColor(getResources().getColor(android.R.color.transparent));
        }

        @Override
        public void onTap(int buttonId) {
            logStatus("Tap");
        }

        @Override
        public void onDoubleTap(int buttonId) {
            logStatus("DoubleTap");
            Toast.makeText(BpSDKDemo.this, "You Double Tapped", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onLongPress(int buttonId) {
            logStatus("Long Press");
        }

        @Override
        public void onProximityChange(int status) {
            logStatus("Proximity state "+status);
        }

        @Override
        public void onValuesRead() {
            logStatus("Values Read");
            logStatus("Firmware Version:"+headset.getFirmwareVersion());
            logStatus("Mode:"+headset.getMode());
            logStatus("Proximity State:"+headset.getProximityState());
            setButtons();
        }

        @Override
        public void onEnterpriseValuesRead() {
            logStatus("Enterprise Values Read");
        }

    };

}