package com.mysay.blueparrottsdkaudiodemo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;


/*
 *
 * Broadcast receiver that is fired when the phone boots (see manifest)
 * This starts the SDK Connection Service and the AudioService so that everything will work in the background
 * even if the App is never run in the foreground
 *
 */
public class StartServicesOnBoot extends BroadcastReceiver {

    private static final String LOGTAG = "BPAudioDemo.StartOnBoot";

    public void onReceive(Context c, Intent arg1) {
        Log.d(LOGTAG, "launching SDK Demo services");

        //Start connection to SDK
        Intent intent = new Intent(c, SdkConnectionService.class);
        c.startService(intent);

        //Start the audio record/playback
        Intent intent2 = new Intent(c, RecPlayAudioService.class);
        c.startService(intent2);


    }

}

