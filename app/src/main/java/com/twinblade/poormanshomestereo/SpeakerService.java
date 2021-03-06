package com.twinblade.poormanshomestereo;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import fi.iki.elonen.NanoHTTPD;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

import static fi.iki.elonen.NanoHTTPD.newFixedLengthResponse;

public class SpeakerService extends Service {

    private final IBinder mBinder = new LocalBinder();

    private SpeakerServer mSpeakerServer;
    private UdpServer mUdpServer;
    private CommandReceiver mCommandReceiver;
    private MediaPlayer mMediaPlayer;
    private OkHttpClient mHttpClient;
    private AudioManager mAudioManager;

    private PowerManager.WakeLock mWakeLock;
    private WifiManager.WifiLock mWifiLock;

    private UpdateListener mUpdateListener;

    private String mControllerIP;
    private Song mCurrentSong;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getClass().getCanonicalName());
        mWakeLock.acquire();

        WifiManager wm = (WifiManager) getSystemService(WIFI_SERVICE);
        mWifiLock = wm.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, getClass().getCanonicalName());
        mWifiLock.acquire();

        mHttpClient = new OkHttpClient.Builder()
                .connectTimeout(Constants.SPEAKER_CONNECTION_TIMEOUT, TimeUnit.SECONDS)
                .build();

        mCommandReceiver = new CommandReceiver();
        IntentFilter filter = new IntentFilter(Constants.INTENT_SPEAKER_TOGGLE_PLAYBACK);
        filter.addAction(Constants.INTENT_SPEAKER_NEXT_SONG);
        filter.addAction(Constants.INTENT_SPEAKER_PREV_SONG);
        registerReceiver(mCommandReceiver, filter);

        mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mMediaPlayer.start();
                sendMessageToController(Constants.SPEAKER_STATE, Constants.SPEAKER_STATE_PLAYING);
            }
        });
        mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                sendMessageToController(Constants.SPEAKER_STATE, Constants.SPEAKER_STATE_END_OF_SONG);
            }
        });

        try {
            mSpeakerServer = new SpeakerServer();
            mSpeakerServer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

        mUdpServer = new UdpServer();
        mUdpServer.start();

        postNotification();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    private void postNotification() {
        Intent speakerActivity = new Intent(this, SpeakerActivity.class);
        PendingIntent speakerActivityPi = PendingIntent.getActivity(this, 0, speakerActivity, 0);

        Intent prevSong = new Intent(Constants.INTENT_SPEAKER_PREV_SONG);
        PendingIntent prevSongPi = PendingIntent.getBroadcast(this, 0, prevSong, 0);

        Intent nextSong = new Intent(Constants.INTENT_SPEAKER_NEXT_SONG);
        PendingIntent nextSongPi = PendingIntent.getBroadcast(this, 0, nextSong, 0);

        Intent togglePlayback = new Intent(Constants.INTENT_SPEAKER_TOGGLE_PLAYBACK);
        PendingIntent togglePlaybackPi = PendingIntent.getBroadcast(this, 0, togglePlayback, 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);

        if (mCurrentSong != null) {
            builder.setContentTitle(mCurrentSong.getTitle());
            builder.setContentText(mCurrentSong.getArtist());
            builder.setSubText("Playing here");
        } else {
            builder.setContentTitle("No music currently playing");
            builder.setContentText("");
        }

        builder.addAction(R.mipmap.ic_back, "Previous", prevSongPi);

        switch (getPlaybackState()) {
            case Constants.SPEAKER_STATE_PLAYING:
                builder.addAction(R.mipmap.ic_pause, "Pause", togglePlaybackPi);
                break;
            case Constants.SPEAKER_STATE_STOPPED:
                builder.addAction(R.mipmap.ic_play, "Play", togglePlaybackPi);
                break;
        }

        builder.addAction(R.mipmap.ic_next, "Next", nextSongPi);
        builder.setContentIntent(speakerActivityPi);
        builder.setSmallIcon(R.mipmap.ic_speaker_mode);
        startForeground(Constants.SPEAKER_NOTIFICATION_ID, builder.build());
    }

    @Override
    public void onDestroy() {
        sendMessageToController(Constants.SPEAKER_STATE, Constants.SPEAKER_STATE_STOPPED);

        try {
            unregisterReceiver(mCommandReceiver);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (mUdpServer != null) {
            mUdpServer.stop();
        }

        if (mSpeakerServer != null) {
            mSpeakerServer.stop();
        }

        if (mMediaPlayer != null) {
            mMediaPlayer.release();
        }

        if (mWifiLock != null && mWifiLock.isHeld()) {
            mWifiLock.release();
        }

        if (mWakeLock != null && mWakeLock.isHeld()) {
            mWakeLock.release();
        }
    }

    private void playSong() {
        String url = "http://" + mControllerIP + ":" + Constants.SERVER_PORT + "/" + Constants.CONTROLLER_FILE_URL;

        mMediaPlayer.reset();
        try {
            mMediaPlayer.setDataSource(url);
            mMediaPlayer.prepare(); //TODO: ASYNC MAYBE?
            mMediaPlayer.start();

            new MetadataLoader().execute(url);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public class SpeakerServer extends NanoHTTPD {

        SpeakerServer() {
            super(Constants.SERVER_PORT);
        }

        @Override
        public Response serve(IHTTPSession session) {
            if (session.getMethod() == Method.POST && session.getUri().startsWith("/" + Constants.SPEAKER_COMMAND_URL)) {
                return processControllerCommand(session);
            } else if (session.getMethod() == Method.GET && session.getUri().startsWith("/" + Constants.SPEAKER_STATE_URL)) {
                return processStateRequest();
            } else {
                return newFixedLengthResponse(Response.Status.NOT_FOUND, "", "");
            }
        }
    }

    private NanoHTTPD.Response processControllerCommand(NanoHTTPD.IHTTPSession session ){
        try {
            session.parseBody(new HashMap<String, String>());
        } catch (IOException | NanoHTTPD.ResponseException e) {
            return newFixedLengthResponse(NanoHTTPD.Response.Status.INTERNAL_ERROR, "", "");
        }

        Map<String, List<String>> params = session.getParameters();
        mControllerIP = session.getRemoteIpAddress();

        if (!params.containsKey(Constants.SPEAKER_COMMAND)) {
            return newFixedLengthResponse(NanoHTTPD.Response.Status.BAD_REQUEST, "", "");
        }

        String command = params.get(Constants.SPEAKER_COMMAND).get(0);
        switch (command) {
            case Constants.SPEAKER_COMMAND_PLAY:
                playSong();
                break;
            case Constants.SPEAKER_COMMAND_PAUSE:
                mMediaPlayer.pause();
                break;
            case Constants.SPEAKER_COMMAND_RESUME:
                mMediaPlayer.start();
                break;
            case Constants.SPEAKER_COMMAND_VOLUME:
                if (params.containsKey(Constants.SPEAKER_COMMAND_VOLUME_PROPERTY)) {
                    int volume = Integer.valueOf(params.get(Constants.SPEAKER_COMMAND_VOLUME_PROPERTY).get(0));
                    mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0);
                } else {
                    return newFixedLengthResponse(NanoHTTPD.Response.Status.BAD_REQUEST, "", "");
                }
                break;
            case Constants.SPEAKER_COMMAND_SEEK:
                // Note: May not be able to seek for files longer than ~24 hours
                int seekDistance = Integer.parseInt(params.get(Constants.SPEAKER_COMMAND_SEEK_TIME).get(0));
                int currentTime = mMediaPlayer.getCurrentPosition();
                int targetTime = currentTime + seekDistance;
                if (targetTime < 0) {
                    mMediaPlayer.seekTo(0);
                } else if (targetTime > mMediaPlayer.getDuration()) {
                    sendMessageToController(Constants.SPEAKER_REQUEST, Constants.SPEAKER_REQUEST_NEXT_SONG);
                } else {
                    mMediaPlayer.seekTo(targetTime);
                }
                break;
            default:
                return newFixedLengthResponse(NanoHTTPD.Response.Status.BAD_REQUEST, "", "");
        }

        if (mUpdateListener != null) {
            mUpdateListener.onStatusUpdate(getPlaybackState());
        }
        postNotification();

        return processStateRequest();
    }

    private NanoHTTPD.Response processStateRequest() {
        try {
            String state = getStateJson();
            return newFixedLengthResponse(state);
        } catch (JSONException e) {
            e.printStackTrace();
            return newFixedLengthResponse(NanoHTTPD.Response.Status.INTERNAL_ERROR, "", "");
        }
    }
    private String getStateJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put(Constants.SPEAKER_STATE, getPlaybackState());
        json.put(Constants.SPEAKER_PROPERTY_NAME, getSpeakerName());
        json.put(Constants.SPEAKER_PROPERTY_VOLUME, mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
        json.put(Constants.SPEAKER_PROPERTY_MAX_VOLUME, mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
        return json.toString();
    }

    private String getSpeakerName() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        return sp.getString(Constants.SPEAKER_PROPERTY_NAME, Build.MODEL);
    }

    private String getPlaybackState() {
        return mMediaPlayer.isPlaying() ? Constants.SPEAKER_STATE_PLAYING : Constants.SPEAKER_STATE_STOPPED;
    }

    public void requestTogglePlayback() {
        if (TextUtils.equals(getPlaybackState(), Constants.SPEAKER_STATE_PLAYING)) {
            sendMessageToController(Constants.SPEAKER_REQUEST, Constants.SPEAKER_REQUEST_PAUSE);
        } else {
            sendMessageToController(Constants.SPEAKER_REQUEST, Constants.SPEAKER_REQUEST_RESUME);
        }
    }

    public void requestNextSong() {
        sendMessageToController(Constants.SPEAKER_REQUEST, Constants.SPEAKER_REQUEST_NEXT_SONG);
    }

    public void requestPreviousSong() {
        sendMessageToController(Constants.SPEAKER_REQUEST, Constants.SPEAKER_REQUEST_PREV_SONG);
    }

    public void requestSeekForward() {
        sendMessageToController(Constants.SPEAKER_REQUEST, Constants.SPEAKER_REQUEST_SEEK_FORWARD);
    }

    public void requestSeekBack() {
        sendMessageToController(Constants.SPEAKER_REQUEST, Constants.SPEAKER_REQUEST_SEEK_BACK);
    }

    private void sendMessageToController(String key, String msg) {
        FormBody.Builder builder = new FormBody.Builder();
        builder.add(key, msg);
        RequestBody body = builder.build();

        Request request = new Request.Builder()
                .url("http://" + mControllerIP + ":" + Constants.SERVER_PORT + "/" + Constants.CONTROLLER_MSG_URL)
                .post(body)
                .build();

        Call call = mHttpClient.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, okhttp3.Response response) throws IOException {
                //
            }
        });
    }

    private class MetadataLoader extends AsyncTask<String, Void, Integer> {

        @Override
        protected Integer doInBackground(String... params) {
            String url = params[0];

            mCurrentSong = Utils.getSongFromUrl(url);
            if (mUpdateListener != null) {
                mUpdateListener.onCurrentSongUpdate(mCurrentSong);
            }
            postNotification();

            return 0;
        }

        @Override
        protected void onPostExecute(Integer result) {
           //
        }
    }

    public class UdpServer {

        private DatagramSocket mUdpSocket;

        void start() {
            new Thread() {
                @Override
                public void run() {
                    runServer();
                }
            }.start();
        }

        private void runServer() {
            byte[] receiveData = new byte[1024];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

            try {
                mUdpSocket = new DatagramSocket(Constants.BROADCAST_PORT);
                mUdpSocket.setBroadcast(true);

                while (!mUdpSocket.isClosed()) {
                    mUdpSocket.receive(receivePacket);

                    String receivedStr = new String(receiveData);
                    if (receivedStr.trim().equals(Constants.BROADCAST_KEY)) {
                        String myIP = Utils.getWifiIpAddress(SpeakerService.this);
                        byte[] response = (Constants.BROADCAST_RESPONSE_PREFIX + myIP).getBytes();

                        DatagramPacket responsePacket = new DatagramPacket(response, response.length,
                                receivePacket.getAddress(), Constants.BROADCAST_PORT);
                        mUdpSocket.send(responsePacket);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        void stop() {
            if (mUdpSocket != null) {
                mUdpSocket.close();
            }
        }
    }

    private class CommandReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Constants.INTENT_SPEAKER_TOGGLE_PLAYBACK)) {
                requestTogglePlayback();
            } else if (intent.getAction().equals(Constants.INTENT_SPEAKER_NEXT_SONG)) {
                requestNextSong();
            } else if (intent.getAction().equals(Constants.INTENT_SPEAKER_PREV_SONG)) {
                requestPreviousSong();
            }
        }
    }

    public class LocalBinder extends Binder {
        SpeakerService getService() {
            return SpeakerService.this;
        }
    }

    public interface UpdateListener {
        void onStatusUpdate(String state);
        void onCurrentSongUpdate(Song song);
    }

    public void broadcastToListener() {
        if (mUpdateListener != null) {
            mUpdateListener.onCurrentSongUpdate(mCurrentSong);
            mUpdateListener.onStatusUpdate(getPlaybackState());
        }
    }

    public void setUpdateListener(UpdateListener listener) {
        mUpdateListener = listener;
    }
}