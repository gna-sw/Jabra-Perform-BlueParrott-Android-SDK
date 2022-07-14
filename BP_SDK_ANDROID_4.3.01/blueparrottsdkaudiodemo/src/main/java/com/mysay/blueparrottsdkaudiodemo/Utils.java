package com.mysay.blueparrottsdkaudiodemo;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import android.util.Log;

/*
 * Utilities class
 */

public class Utils {

    private static final String LOGTAG = "BPAudioDemo.Utils";
    private static final int HEADSET_NOTIFICATION_ID = 100;
    public static final String NOTIFICATION_CHANNEL_ID = "10001";


    /*
     * Check if there is a BT headset connected
     */
    @SuppressLint("MissingPermission")
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
     * Check a list of permissions to see if user has them
     */
    public static boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    /*
     * Return text string for use in captions that represents current audio state (play, record etc.)
     */
    public static int getMessageForAudioState(int state) {
        switch (state) {
            case RecPlayAudioService.AUDIO_STATE_IDLE:
                return R.string.audio_status_idle;

            case RecPlayAudioService.AUDIO_STATE_WAITING_FOR_SCO:
                return R.string.audio_status_waiting_sco;

            case RecPlayAudioService.AUDIO_STATE_RECORDING:
                return R.string.audio_status_recording;

            case RecPlayAudioService.AUDIO_STATE_PLAYING:
                return R.string.audio_status_playing;

            default:
                return R.string.audio_status_idle;
        }
    }

    /*
     * Return drawable for use in notification that represents current audio state (play, record etc.)
     */
    public static int getNotificationIconForAudioState(int state) {
        switch (state) {
            case RecPlayAudioService.AUDIO_STATE_IDLE:
                return R.drawable.ic_stat_parrotthead;

            case RecPlayAudioService.AUDIO_STATE_WAITING_FOR_SCO:
                return R.drawable.ic_stat_parrottheadinv;

            case RecPlayAudioService.AUDIO_STATE_RECORDING:
                return R.drawable.ic_stat_parrottheadinv;

            case RecPlayAudioService.AUDIO_STATE_PLAYING:
                return R.drawable.ic_stat_parrotthead;

            default:
                return R.drawable.ic_stat_parrotthead;
        }
    }

    /*
     * Return background drawable (correct colour) to be used on Talk Button, depending on audio state (play, record)
     */
    public static int getBackgroundDrawableForAudioState(int state) {
        switch (state) {
            case RecPlayAudioService.AUDIO_STATE_IDLE:
                return R.drawable.talk_button_idle;

            case RecPlayAudioService.AUDIO_STATE_WAITING_FOR_SCO:
                return R.drawable.talk_button_waiting_sco;

            case RecPlayAudioService.AUDIO_STATE_RECORDING:
                return R.drawable.talk_button_recording;

            case RecPlayAudioService.AUDIO_STATE_PLAYING:
                return R.drawable.talk_button_playing;

            default:
                return R.drawable.talk_button_idle;
        }
    }

    /**
     * Create a notification based on the current audio state
     */
    public static void createNotification(Context context, int state) {

        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder mBuilder;

        //creates an intent to launch the app
        Intent resultIntent = new Intent(context, MainActivity.class);
        resultIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent resultPendingIntent = PendingIntent.getActivity(context,
                0 /* Request code */, resultIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        //build the notification
        mBuilder = new NotificationCompat.Builder(context);
        mBuilder.setSmallIcon(Utils.getNotificationIconForAudioState(state));
        mBuilder.setContentTitle("BlueParrott SDK")
                .setContentText(context.getString(Utils.getMessageForAudioState(state)))
                .setAutoCancel(false)
                .setContentIntent(resultPendingIntent);


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
        assert mNotificationManager != null;
        mNotificationManager.notify(HEADSET_NOTIFICATION_ID /* Request Code */, mBuilder.build());
    }

    /*
     * Remove notification from the notification bar
     */
    public static void cancelHeadsetStatusNotification(Context context) {
        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(HEADSET_NOTIFICATION_ID);
    }


}
