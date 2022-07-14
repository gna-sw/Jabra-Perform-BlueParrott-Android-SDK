package com.mysay.blueparrottsdkaudiodemo;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.FileProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.blueparrott.blueparrottsdk.BPHeadset;
import com.blueparrott.blueparrottsdk.BPHeadsetListener;
import com.blueparrott.blueparrottsdk.BPSdk;
import com.blueparrott.blueparrottsdk.IBPHeadsetListener;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;



/*
 * Implements simple echo service to illustrate handling of  typical audio  (e.g. PTT App) from the BP Button
 * Not intended to be a fully featured app, this just shows one approach to managing the recording of audio over SCO
 * when the BP button is pressed
 */

public class RecPlayAudioService extends Service implements IBPHeadsetListener {

    private static final String LOGTAG = "BPAudioDemo.RecPlay";

    //broadcast information about audio state (recording, playing etc.) to the UI
    static final public String BP_AUDIO_STATE_BROADCAST = "com.blueparrott.demo.audiostatebroadcast";
    static final public String AUDIO_STATE_MESSAGE = "com.blueparrott.demo.audiostatemessage";
    public static final int AUDIO_STATE_IDLE = 0;
    public static final int AUDIO_STATE_WAITING_FOR_SCO = 1;
    public static final int AUDIO_STATE_RECORDING = 2;
    public static final int AUDIO_STATE_PLAYING = 3;

    //audio settings
    static final int channelConfiguration = AudioFormat.CHANNEL_CONFIGURATION_MONO;
    static final int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
    AudioManager audioManager;
    AudioRecord audioRecord;
    AudioTrack audioTrack;
    int mFrequency = 44100;
    int mRecBufSize, mPlayBufSize;
    File mPcmFile;
    boolean isRecording = false;
    boolean isPlaying = false;
    RecordThread recordThread;


    boolean mHeadsetConnected = false;
    LocalBroadcastManager broadcaster;

    //handle to BP Headset
    private BPHeadset headset;

    //Receiver to monitor headset connect/disconnects (required to restart audio unit for different frequencies based on audio path changes)
    HeadsetChangeReceiver headsetChangeReceiver;

    //Standard onBind method returns handle to localBinder
    private final RecPlayAudioService.AudioBinder localBinder = new RecPlayAudioService.AudioBinder();//create binder - TODO why do we need to override standard binder
    @Override
    public IBinder onBind(Intent intent) {
        return localBinder;
    }
    public class AudioBinder extends Binder {
        public RecPlayAudioService getService() {
            return RecPlayAudioService.this;
        }
    }


    /*
     * Get a handle to the headset and setup globals needed later
     */
    @Override
    public void onCreate() {
        super.onCreate();

        //get a handle to the BlueParrott SDK instance
        headset = BPSdk.getBPHeadset(this);

        //add a listener so we can intercept button events
        headset.addListener(this);

        //setup a receiver for ACL_CONNECTS and ACL_DISCONNECTS - so that we can change the audio recording frequency when a headset is connected/disconnected
        registerForHeadsetStateChange();

        //setup a localbroadcaster that we will use to tell the UI about state changed
        broadcaster = LocalBroadcastManager.getInstance(this);
    }


    /*
     * When the service is started
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v(LOGTAG, "Starting Audio Service");

        //initialize the audio unit (recorder/player) based on current audio path
        initAudio();

        return Service.START_STICKY;  //used for services that are expliictly started and stopped as needed//service will restart automatically
    }

    /*
     * Setup the audio components for record and playback
     */
    public void initAudio() {

        Log.d(LOGTAG, "(Re)Initialising Audio");
        mHeadsetConnected = Utils.isBluetoothHeadsetConnected();

        //setup the file to use as a temp file
        try {
            mPcmFile = File.createTempFile("tempaudio.pcm", null, this.getCacheDir());
        } catch (IOException e) {
            Toast.makeText(RecPlayAudioService.this,"Couldn't Create TempFile",Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }

        //setup the recorder and calculate the frequency
        audioRecord = getAudioRecord();

        //setup the player
        mPlayBufSize = AudioTrack.getMinBufferSize(mFrequency, channelConfiguration, audioEncoding);
        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, mFrequency, channelConfiguration, audioEncoding, mPlayBufSize, AudioTrack.MODE_STREAM);

        //get a handle to the Android audio manager
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
    }


    /*
     * Get AudioRecorder
     */
    private AudioRecord getAudioRecord() {

        // According to android docs this rate should be available on all devices, so if all else fails we will use this
        mRecBufSize = 8000;
        mFrequency = 44100;

        //For bluetooth - just use 8000
        if (Utils.isBluetoothHeadsetConnected()) {
            Log.d(LOGTAG, "Setting up audio recorder to use Bluetooth");
            mRecBufSize = 8000;
            mFrequency = 8000;

        } else {
            Log.d(LOGTAG, "Setting up audio recorder to use built in mic/wired headset");
            // Iterate through possible rates and when you get one, return - TODO see  http://s tackoverflow.com/questions/8043387/android-audiorecord-supported-sampling-rates
            int[] possibleSampleRates = new int[]{44100, 32000, 22050, 16000, 11025, 8000};
            for (int testSampleRate : possibleSampleRates) {
                int candidateBufferSize = AudioRecord.getMinBufferSize(testSampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);// * 2;
                if (candidateBufferSize > 0) {
                    mRecBufSize = candidateBufferSize;
                    mFrequency = testSampleRate;
                    break;
                }
            }
        }

        //Make the audiorecorder
        AudioRecord audioRecorder = new AudioRecord(MediaRecorder.AudioSource.VOICE_COMMUNICATION, mFrequency, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, mRecBufSize); //THIS ADDITIONAL BUFFER SIZE IS IMPORTANT * 10 //need a bigger buffer to compensate for tiny blocking in the thread
        return audioRecorder;
    }


    /*
     * Broadcast the audio state to the UI
     */
    public void broadcastAudioState(int audioState) {

        Utils.createNotification(this, audioState);
        Intent intent = new Intent(BP_AUDIO_STATE_BROADCAST);
        intent.putExtra(AUDIO_STATE_MESSAGE, audioState);
        broadcaster.sendBroadcast(intent);
    }


    /*
     * Start Recording Thread
     */
    public void startRecording() {
        Log.d(LOGTAG, "starting Recording");

        //if we are connected to a headset, we need to wait for the SCO channel to open before starting recording
        if (Utils.isBluetoothHeadsetConnected()) {

            //Setup a receiver to notify us when SCO is connected
            registerReceiver(new BroadcastReceiver() {

                @Override
                public void onReceive(Context context, Intent intent) {
                    int state = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, -1);
                    Log.d(LOGTAG, "Audio SCO state: " + state);

                    //once  we have a connected SCO device, start the recording
                    if (AudioManager.SCO_AUDIO_STATE_CONNECTED == state) {
                        broadcastAudioState(AUDIO_STATE_RECORDING);
                        isRecording = true;
                        recordThread = new RecordThread();
                        recordThread.start();
                        unregisterReceiver(this);
                    }

                }
            }, new IntentFilter(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED));

            Log.d(LOGTAG, "Starting SCO Wait");
            broadcastAudioState(AUDIO_STATE_WAITING_FOR_SCO);
            audioManager.startBluetoothSco();//this will trigger the reciever in all cirumstances once SCO is started (or restarted)

        } else {
            //there is no need to wait for SCO because no headset is connected, just start recording now
            Log.d(LOGTAG,"No Headset Detected justing going ahead and recording");
            isRecording = true;
            broadcastAudioState(AUDIO_STATE_RECORDING);
            recordThread = new RecordThread();
            recordThread.start();
        }
    }

    /*
     * End the recording
     */
    public void stopRecording() {
        Log.d(LOGTAG, "Stopping recording");

        // close the SCO channel - could wait until after playing to do this alternatively
        if (Utils.isBluetoothHeadsetConnected()) {
            audioManager.stopBluetoothSco();
        }

        // wind down the thread and get in sync with it
        isRecording = false;
        try {
            if (recordThread != null) recordThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // start the playing thread
        startPlaying();
    }

    /*
     * Play the contents of the temp file
     */
    public void startPlaying() {
        Log.d(LOGTAG, "starting playback");

        //let the ui know whats going on
        broadcastAudioState(AUDIO_STATE_PLAYING);

        //start playback
        isPlaying = true;
        new PlayThread().start();
    }

    @Override
    public void onConnectProgress(int progressCode) {}

    @Override
    public void onConnect() {

    }

    @Override
    public void onConnectFailure(int reasonCode) {

    }

    @Override
    public void onDisconnect() {
        if (isRecording) {
            stopRecording();
        }
    }

    @Override
    public void onModeUpdate() {

    }

    @Override
    public void onModeUpdateFailure(int reasonCode) {

    }

    /*
     * When user presses Parott Button start the record
     */
    @Override
    public void onButtonDown(int buttonId) {
        startRecording();
    }

    /*
     * When user unpresses Parrott Button, finih recording and trigger playback (echo)
     */
    @Override
    public void onButtonUp(int buttonId) {
        stopRecording();
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
     * Setup receiver to be notified when bluetooth connect/disconnect received so we can adjust the audio settings
     */
    private void registerForHeadsetStateChange() {
        headsetChangeReceiver = new HeadsetChangeReceiver();
        IntentFilter fltr = new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED);
        fltr.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        registerReceiver(headsetChangeReceiver, fltr);
    }

    /*
     * Receiver to track changes in headset state we can adjust audio
     */
    public class HeadsetChangeReceiver extends BroadcastReceiver {
        public void onReceive(Context context, final Intent intent) {

            boolean newStatus = Utils.isBluetoothHeadsetConnected();
            if (newStatus != mHeadsetConnected) {
                initAudio();
            }

        }

    }


    /*
     * Record thread that records audio and puts to a temp file
     */
    class RecordThread extends Thread {
        public void run() {
            try {
                byte[] buffer = new byte[mRecBufSize];
                BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(mPcmFile), mRecBufSize);

                audioRecord.startRecording();

                while (isRecording) {

                    int bufferReadResult = audioRecord.read(buffer, 0, mRecBufSize);
                    byte[] tmpBuf = new byte[bufferReadResult];
                    System.arraycopy(buffer, 0, tmpBuf, 0, bufferReadResult);
                    bos.write(tmpBuf, 0, tmpBuf.length);
                }
                bos.flush();
                bos.close();

                audioRecord.stop();

            } catch (Throwable t) {
                Log.d(LOGTAG, "error:" + t.getMessage());
            }
        }
    }

    /*
     * Play thread that plays back contents of temp file
     */
    class PlayThread extends Thread {
        public void run() {
            try {
                sleep(500);//wait a bit (could play a little soundpool here)
                byte[] buffer = new byte[mPlayBufSize];
                BufferedInputStream bis = new BufferedInputStream(new FileInputStream(mPcmFile), mPlayBufSize);

                audioTrack.play();

                int readSize = -1;
                while (isPlaying && (readSize = bis.read(buffer)) != -1) {
                    audioTrack.write(buffer, 0, readSize);
                }
                audioTrack.stop();

                bis.close();
                broadcastAudioState(AUDIO_STATE_IDLE);
            } catch (Throwable t) {
                Log.d(LOGTAG, "error:" + t.getMessage());
            }
        }
    }

}
