package com.blueparrott.connectsample;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import androidx.core.app.NotificationCompat;
import android.util.Log;

import com.blueparrott.blueparrottsdk.BPHeadsetListener;

/*
 * Utilities methods for SDK Connection Sample
 */
public class Utils {

    private static final String LOGTAG = "BPSDKSample.Utils";
    private static final int HEADSET_NOTIFICATION_ID = 100;
    public static final String NOTIFICATION_CHANNEL_ID = "10001";


    /*
     * Check if there is a BT headset connected
     */
    static boolean isBluetoothHeadsetConnected() {
        try {
            return BluetoothAdapter.getDefaultAdapter().getProfileConnectionState(android.bluetooth.BluetoothProfile.HEADSET) == android.bluetooth.BluetoothProfile.STATE_CONNECTED;
        } catch (Exception exc) {
            //should do a fuller error handling implementation for bluetooth on/off etc.
            Log.d(LOGTAG, "Problem accessing Bluetooth Adapter");
        }
        return false;
    }


    /*
     * Return text string for use in captions that represents state of button (used in notifications, on display)
     */
    public static int getMessageForButtonState(int state) {
        switch (state) {
            case SdkConnectionService.BUTTON_STATE_DISCONNECTED:
                return R.string.button_state_disconnected;
            case SdkConnectionService.BUTTON_STATE_CONNECTING:
                return R.string.button_state_connecting;
            case SdkConnectionService.BUTTON_STATE_CONNECTED:
                return R.string.button_state_connected;
            case SdkConnectionService.BUTTON_STATE_DOWN:
                return R.string.button_state_down;
            case SdkConnectionService.BUTTON_STATE_UP:
                return R.string.button_state_up;
            default:
                return R.string.button_state_disconnected;
        }
    }

    /*
     * Return drawable for use in notification that represents current parrott button
     */
    public static int getNotificationIconForAudioState(int state) {
        switch (state) {
            case SdkConnectionService.BUTTON_STATE_DISCONNECTED:
                return android.R.drawable.radiobutton_off_background;

            case SdkConnectionService.BUTTON_STATE_CONNECTED:
                return android.R.drawable.radiobutton_on_background;

            case SdkConnectionService.BUTTON_STATE_DOWN:
                return R.drawable.ic_stat_parrottheadinv;

            case SdkConnectionService.BUTTON_STATE_UP:
                return R.drawable.ic_stat_parrotthead;

            default:
                return R.drawable.ic_stat_parrotthead;
        }
    }


    /**
     * Create a notification based on the current sdk/button state
     */
    public static Notification buildNotification(Context context, int state) {

        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder mBuilder;

        //creates an intent to launch the app
        Intent resultIntent = new Intent(context, MainActivity.class);
        resultIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        //build the notification
        mBuilder = new NotificationCompat.Builder(context);
        mBuilder.setSmallIcon(Utils.getNotificationIconForAudioState(state));
        mBuilder.setContentTitle("BlueParrott SDK")
                .setContentText(context.getString(Utils.getMessageForButtonState(state)))
                .setAutoCancel(false);


        //use channel for oreo
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, "BLUEPARROTT_SDK_DEMO", importance);
            notificationChannel.setVibrationPattern(null);
            notificationChannel.setSound(null, null);
            assert mNotificationManager != null;
            mBuilder.setChannelId(NOTIFICATION_CHANNEL_ID);
            mNotificationManager.createNotificationChannel(notificationChannel);
        }
        return mBuilder.build();
    }


    /**
     * Create a notification based on the current audio state
     */
    public static void createNotification(Context context, int state) {

        Notification n=buildNotification(context,state);
        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(HEADSET_NOTIFICATION_ID , n);
    }


    /*
     * Remove notification from the notification bar
     */
    public static void cancelHeadsetStatusNotification(Context context) {
        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(HEADSET_NOTIFICATION_ID);
    }


    protected static String getStatusDescription(int progressCode) {
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

    protected static String getUpdateErrorDescription(int errorCode) {
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

    protected static String getConnectErrorDescription(int errorCode) {
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
                return "BLE Connection requires Location Permission";
            default:
                return "Unknown error connecting to Parrott Button";
        }

    }

}
