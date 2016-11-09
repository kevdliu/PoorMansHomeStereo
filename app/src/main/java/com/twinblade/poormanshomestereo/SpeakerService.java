package com.twinblade.poormanshomestereo;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

public class SpeakerService extends Service {

    private final IBinder mBinder = new LocalBinder();

    private SpeakerServer mSpeakerServer;
    private UdpServer mUdpServer;
    private CommandReceiver mCommandReceiver;
    private MediaPlayer mMediaPlayer;
    private OkHttpClient mHttpClient;

    private PowerManager.WakeLock mWakeLock;
    private WifiManager.WifiLock mWifiLock;

    private UpdateListener mUpdateListener;

    private String mControllerIP;

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

        mHttpClient = new OkHttpClient();

        mCommandReceiver = new CommandReceiver();
        registerReceiver(mCommandReceiver, new IntentFilter(Constants.INTENT_STOP_SPEAKER_SERVICE));

        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mMediaPlayer.start();
                sendMessageToController(Constants.SPEAKER_STATUS, Constants.SPEAKER_STATUS_PLAYING);
            }
        });
        mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                sendMessageToController(Constants.SPEAKER_STATUS, Constants.SPEAKER_STATUS_END_OF_SONG);
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

        Intent speakerActivity = new Intent(this, SpeakerActivity.class);
        PendingIntent speakerActivityPi = PendingIntent.getActivity(this, 0, speakerActivity, 0);

        Intent stopService = new Intent(Constants.INTENT_STOP_SPEAKER_SERVICE);
        PendingIntent stopServicePi = PendingIntent.getBroadcast(this, 0, stopService, 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_queue)
                .setContentTitle("Speaker Service Running")
                .setContentIntent(speakerActivityPi)
                .addAction(0, "Stop", stopServicePi);

        startForeground(Constants.SPEAKER_NOTIFICATION_ID, builder.build());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }


    @Override
    public void onDestroy() {
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

    public class SpeakerServer extends NanoHTTPD {

        SpeakerServer() {
            super(Constants.SERVER_PORT);
        }

        @Override
        public Response serve(IHTTPSession session) {
            // TODO: Clean up
            if (session.getMethod() == Method.POST && session.getUri().startsWith("/" + Constants.SPEAKER_COMMAND_URL)) {
                try {
                    session.parseBody(new HashMap<String, String>());
                } catch (IOException | ResponseException e) {
                    return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "", "");
                }

                Map<String, List<String>> params = session.getParameters();
                mControllerIP = session.getRemoteIpAddress();

                if (!params.containsKey(Constants.SPEAKER_COMMAND)) {
                    return newFixedLengthResponse(Response.Status.BAD_REQUEST, "", "");
                }

                String command = params.get(Constants.SPEAKER_COMMAND).get(0);
                switch (command) {
                    case Constants.SPEAKER_COMMAND_PLAY:
                        String url = "http://" + mControllerIP + ":" + Constants.SERVER_PORT + "/" + Constants.CONTROLLER_FILE_URL;

                        mMediaPlayer.reset();
                        try {
                            mMediaPlayer.setDataSource(url);
                            mMediaPlayer.prepare(); //TODO: ASYNC MAYBE?
                            mMediaPlayer.start();

                            if (mUpdateListener != null) {
                                new MetadataLoader().execute(url);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        break;
                    case Constants.SPEAKER_COMMAND_PAUSE:
                        mMediaPlayer.pause();
                        break;
                    case Constants.SPEAKER_COMMAND_RESUME:
                        mMediaPlayer.start();
                        break;
                    default:
                        return newFixedLengthResponse(Response.Status.BAD_REQUEST, "", "");
                }

                if (mUpdateListener != null) {
                    mUpdateListener.onStatusUpdate(getPlaybackState());
                }

                try {
                    String state = getStateJson();
                    return newFixedLengthResponse(state);
                } catch (JSONException e) {
                    e.printStackTrace();
                    return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "", "");
                }
            } else if (session.getMethod() == Method.GET && session.getUri().startsWith("/" + Constants.SPEAKER_STATUS_URL)) {
                try {
                    String state = getStateJson();
                    return newFixedLengthResponse(state);
                } catch (JSONException e) {
                    e.printStackTrace();
                    return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "", "");
                }
            } else {
                return newFixedLengthResponse(Response.Status.NOT_FOUND, "", "");
            }
        }
    }

    private String getStateJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put(Constants.SPEAKER_STATUS, getPlaybackState());
        return json.toString();
    }

    private String getPlaybackState() {
        return mMediaPlayer.isPlaying() ? Constants.SPEAKER_STATUS_PLAYING : Constants.SPEAKER_STATUS_STOPPED;
    }

    public void requestTogglePlayback() {
        if (TextUtils.equals(getPlaybackState(), Constants.SPEAKER_STATUS_PLAYING)) {
            sendMessageToController(Constants.SPEAKER_REQUEST, Constants.SPEAKER_REQUEST_PAUSE);
        } else {
            sendMessageToController(Constants.SPEAKER_REQUEST, Constants.SPEAKER_REQUEST_RESUME);
        }
    }

    public void requestSkipNext() {
        sendMessageToController(Constants.SPEAKER_REQUEST, Constants.SPEAKER_REQUEST_SKIP_NEXT);
    }

    public void requestSkipPrevious() {
        sendMessageToController(Constants.SPEAKER_REQUEST, Constants.SPEAKER_REQUEST_SKIP_PREVIOUS);
    }

    private void sendMessageToController(String key, String msg) {
        FormBody.Builder builder = new FormBody.Builder();
        builder.add(key, msg);
        RequestBody body = builder.build();

        Request request = new Request.Builder()
                .url("http://" + mControllerIP + ":" + Constants.SERVER_PORT + "/" + Constants.CONTROLLER_MSG_URL)
                .post(body)
                .build();

        Log.e("PMHS", "Sending " + request.body().toString());
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

            Song song = Utils.getSongFromUrl(url);
            if (mUpdateListener != null) {
                mUpdateListener.onCurrentSongUpdate(song);
            }

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
            if (intent.getAction().equals(Constants.INTENT_STOP_SPEAKER_SERVICE)) {
                stopSelf();
            }
        }
    }

    public class LocalBinder extends Binder {
        SpeakerService getService() {
            return SpeakerService.this;
        }
    }

    public interface UpdateListener {
        void onStatusUpdate(String status);
        void onCurrentSongUpdate(Song song);
    }

    public void setUpdateListener(UpdateListener listener) {
        mUpdateListener = listener;
    }
}