package com.twinblade.poormanshomestereo;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
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
import okhttp3.Response;

import static fi.iki.elonen.NanoHTTPD.newFixedLengthResponse;

public class ControllerService extends Service {

    private final IBinder mBinder = new LocalBinder();
    private ControllerServer mControllerServer;
    private OkHttpClient mHttpClient;
    private CommandReceiver mCommandReceiver;
    private PowerManager.WakeLock mWakeLock;
    private WifiManager.WifiLock mWifiLock;

    private UpdateListener mUpdateListener;

    private ArrayList<Song> mSongQueue = new ArrayList<>();
    private int mSongQueueIndex = 0;

    private String mSpeakerName;
    private String mSpeakerAddress;
    private String mSpeakerState = Constants.SPEAKER_STATE_STOPPED;

    @Override
    public void onCreate() {
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getClass().getCanonicalName());
        mWakeLock.acquire();

        WifiManager wm = (WifiManager) getSystemService(WIFI_SERVICE);
        mWifiLock = wm.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, getClass().getCanonicalName());
        mWifiLock.acquire();

        mHttpClient = new OkHttpClient();

        try {
            mControllerServer = new ControllerServer();
            mControllerServer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

        mCommandReceiver = new CommandReceiver();
        IntentFilter filter = new IntentFilter(Constants.INTENT_STOP_CONTROLLER_SERVICE);
        filter.addAction(Constants.INTENT_SPEAKER_NEXT_SONG);
        filter.addAction(Constants.INTENT_SPEAKER_TOGGLE_PLAYBACK);
        registerReceiver(mCommandReceiver, filter);

        postNotification();
    }

    private void postNotification() {
        Intent controllerActivity = new Intent(this, ControllerActivity.class);
        PendingIntent controllerActivityPi = PendingIntent.getActivity(this, 0, controllerActivity, 0);

        Intent stopService = new Intent(Constants.INTENT_STOP_CONTROLLER_SERVICE);
        PendingIntent stopServicePi = PendingIntent.getBroadcast(this, 0, stopService, 0);

        Intent nextSong = new Intent(Constants.INTENT_SPEAKER_NEXT_SONG);
        PendingIntent nextSongPi = PendingIntent.getBroadcast(this, 0, nextSong, 0);

        Intent togglePlayback = new Intent(Constants.INTENT_SPEAKER_TOGGLE_PLAYBACK);
        PendingIntent togglePlaybackPi = PendingIntent.getBroadcast(this, 0, togglePlayback, 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);

        if (!mSongQueue.isEmpty() && mSongQueueIndex < mSongQueue.size()) {
            Song song = mSongQueue.get(mSongQueueIndex);
            builder.setContentTitle(song.getTitle());
            builder.setContentText(song.getArtist());
            builder.setSubText("Playing on " + mSpeakerName);
        } else {
            builder.setContentTitle("No music currently playing");
        }

        switch (mSpeakerState) {
            case Constants.SPEAKER_STATE_PLAYING:
                builder.addAction(R.mipmap.ic_pause, "Pause", togglePlaybackPi);
                break;
            case Constants.SPEAKER_STATE_STOPPED:
                builder.addAction(R.mipmap.ic_play, "Play", togglePlaybackPi);
                break;
        }

        builder.addAction(R.mipmap.ic_next, "Next", nextSongPi);
        builder.addAction(0, "Exit", stopServicePi);
        builder.setContentIntent(controllerActivityPi);
        builder.setSmallIcon(R.mipmap.ic_songs);
        startForeground(Constants.CONTROLLER_NOTIFICATION_ID, builder.build());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    public void setUpdateListener(UpdateListener listener) {
        mUpdateListener = listener;
    }

    public void addSongToQueue(Song song) {
        mSongQueue.add(song);
    }

    public void removeSongFromQueue(int index) {
        mSongQueue.remove(index);
    }

    public void playSongNext(Song song) {
        if (mSongQueue.isEmpty()) {
            mSongQueue.add(song);
        } else {
            mSongQueue.add(mSongQueueIndex + 1, song);
        }
    }

    public void replaceQueue(ArrayList<Song> queue, int playIndex) {
        mSongQueue = queue;
        playSongAtQueueIndex(playIndex);
    }

    public void playSongAtQueueIndex(int playIndex) {
        if (playIndex >= mSongQueue.size()) {
            return;
        }

        mSongQueueIndex = playIndex;
        sendCommandToSpeaker(Constants.SPEAKER_COMMAND_PLAY);
        broadcastCurrentSongUpdate();
    }

    public ArrayList<Song> getSongQueue() {
        return mSongQueue;
    }

    public void pauseSong() {
        sendCommandToSpeaker(Constants.SPEAKER_COMMAND_PAUSE);
    }

    public void resumeSong() {
        sendCommandToSpeaker(Constants.SPEAKER_COMMAND_RESUME);
    }

    public void previousSong() {
        if (loadPreviousSong()) {
            sendCommandToSpeaker(Constants.SPEAKER_COMMAND_PLAY);
            broadcastCurrentSongUpdate();
        } else {
            mSpeakerState = Constants.SPEAKER_STATE_STOPPED;
            broadcastSpeakerStateUpdate();
        }
    }

    public void nextSong() {
        if (loadNextSong()) {
            sendCommandToSpeaker(Constants.SPEAKER_COMMAND_PLAY);
            broadcastCurrentSongUpdate();
        } else {
            mSpeakerState = Constants.SPEAKER_STATE_STOPPED;
            broadcastSpeakerStateUpdate();
        }
    }

    public Song getCurrentSong() {
        if (!mSongQueue.isEmpty() && mSongQueueIndex < mSongQueue.size()) {
            return mSongQueue.get(mSongQueueIndex);
        }

        return null;
    }

    public int getCurrentSongQueueIndex() {
        return mSongQueueIndex;
    }

    public String getSelectedSpeaker() {
        return mSpeakerAddress;
    }

    public void selectSpeaker(String address, String name) {
        mSpeakerName = name;
        mSpeakerAddress = address;
        checkForSpeakerUpdate();
    }

    public String getSpeakerState() {
        return mSpeakerState;
    }

    public void broadcastToListener(UpdateListener listener) {
        if (!mSongQueue.isEmpty() && mSongQueueIndex < mSongQueue.size()) {
            listener.onCurrentSongUpdate(mSongQueue.get(mSongQueueIndex));
        } else {
            listener.onCurrentSongUpdate(null);
        }

        listener.onStatusUpdate(mSpeakerState);
    }

    private boolean loadNextSong() {
        if (!mSongQueue.isEmpty() && mSongQueueIndex + 1 < mSongQueue.size()) {
            mSongQueueIndex++;
            return true;
        }

        return false;
    }

    private boolean loadPreviousSong() {
        if (!mSongQueue.isEmpty() && mSongQueueIndex - 1 >= 0) {
            mSongQueueIndex--;
            return true;
        }

        return false;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onDestroy() {
        try {
            unregisterReceiver(mCommandReceiver);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (mControllerServer != null && mControllerServer.isAlive()) {
            mControllerServer.stop();
        }

        if (mWifiLock != null && mWifiLock.isHeld()) {
            mWifiLock.release();
        }

        if (mWakeLock != null && mWakeLock.isHeld()) {
            mWakeLock.release();
        }
    }

    public class ControllerServer extends NanoHTTPD {

        ControllerServer() {
            super(Constants.SERVER_PORT);
        }

        @Override
        public Response serve(IHTTPSession session) {
            if (session.getMethod() == Method.GET && session.getUri().startsWith("/" + Constants.CONTROLLER_FILE_URL)) {
                return serveMediaFile(session);
            } else if (session.getMethod() == Method.POST && session.getUri().startsWith("/" + Constants.CONTROLLER_MSG_URL)) {
                return processMessage(session);
            } else {
                return newFixedLengthResponse(Response.Status.NOT_FOUND, "", "");
            }
        }
    }

    private NanoHTTPD.Response serveMediaFile(NanoHTTPD.IHTTPSession session) {
        if (mSongQueue.isEmpty() || mSongQueueIndex >= mSongQueue.size()) {
            return newFixedLengthResponse(NanoHTTPD.Response.Status.NOT_FOUND, "", "");
        }

        File file = mSongQueue.get(mSongQueueIndex).getFile();
        Map<String, String> reqHeaders = session.getHeaders();
        HashMap<String, String> resHeaders = new HashMap<>();
        resHeaders.put("Accept-Ranges", "bytes");

        try {
            long readStart = 0;
            long readEnd = file.length() - 1;
            NanoHTTPD.Response.Status resCode = NanoHTTPD.Response.Status.OK;
            if (reqHeaders.containsKey("Range")) {
                String rangeStr = reqHeaders.get("Range");
                rangeStr = rangeStr.substring("bytes=".length());
                String[] rangeArr = rangeStr.split("-");
                readStart = Long.valueOf(rangeArr[0]);
                if (rangeArr.length > 1) {
                    readEnd = Long.valueOf(rangeArr[1]) - 1;
                }

                String rangeResStr = "bytes " + readStart + "-" + readEnd + "/" + file.length();
                resHeaders.put("Content-Range", rangeResStr);
                resCode = NanoHTTPD.Response.Status.PARTIAL_CONTENT;
            }
            long contentLength = readEnd - readStart + 1;

            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
            if (bis.skip(readStart) == 0) {
                Log.w(getPackageName(), "Skipped 0 bytes");
            }

            NanoHTTPD.Response res = newFixedLengthResponse(resCode, "audio/mpeg", bis, contentLength);
            for (String headerKey : resHeaders.keySet()) {
                res.addHeader(headerKey, resHeaders.get(headerKey));
            }

            return res;
        } catch (FileNotFoundException e) {
            return newFixedLengthResponse(NanoHTTPD.Response.Status.NOT_FOUND, "", "");
        } catch (IOException e) {
            return newFixedLengthResponse(NanoHTTPD.Response.Status.INTERNAL_ERROR, "", "");
        }
    }

    private NanoHTTPD.Response processMessage(NanoHTTPD.IHTTPSession session) {
        try {
            session.parseBody(new HashMap<String, String>());
        } catch (IOException | NanoHTTPD.ResponseException e) {
            e.printStackTrace();
            return newFixedLengthResponse(NanoHTTPD.Response.Status.INTERNAL_ERROR, "", "");
        }

        Map<String, List<String>> params = session.getParameters();
        if (params.containsKey(Constants.SPEAKER_STATE)) {
            final String state = params.get(Constants.SPEAKER_STATE).get(0);

            switch (state) {
                case Constants.SPEAKER_STATE_END_OF_SONG:
                    nextSong();
                    break;

                case Constants.SPEAKER_STATE_PLAYING:
                case Constants.SPEAKER_STATE_STOPPED:
                    mSpeakerState = state;
                    broadcastSpeakerStateUpdate();
                    break;
            }
        }

        if (params.containsKey(Constants.SPEAKER_REQUEST)) {
            final String request = params.get(Constants.SPEAKER_REQUEST).get(0);

            switch (request) {
                case Constants.SPEAKER_REQUEST_PAUSE:
                    pauseSong();
                    break;
                case Constants.SPEAKER_REQUEST_RESUME:
                    resumeSong();
                    break;
                case Constants.SPEAKER_REQUEST_NEXT_SONG:
                    nextSong();
                    break;
                case Constants.SPEAKER_REQUEST_PREV_SONG:
                    previousSong();
                    break;
            }
        }

        return newFixedLengthResponse(NanoHTTPD.Response.Status.OK, "", "");
    }

    private void sendCommandToSpeaker(String cmd) {
        if (mSpeakerAddress == null || mSpeakerAddress.equals("")) {
            return;
        }

        FormBody.Builder builder = new FormBody.Builder();
        builder.add(Constants.SPEAKER_COMMAND, cmd);
        RequestBody body = builder.build();

        Request request = new Request.Builder()
                .url("http://" + mSpeakerAddress + ":" + Constants.SERVER_PORT + "/" + Constants.SPEAKER_COMMAND_URL)
                .post(body)
                .build();

        Call call = mHttpClient.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                updateFromSpeakerResponse(response);
            }
        });
    }

    private void checkForSpeakerUpdate() {
        if (mSpeakerAddress == null || mSpeakerAddress.equals("")) {
            return;
        }

        Request request = new Request.Builder()
                .url("http://" + mSpeakerAddress + ":" + Constants.SERVER_PORT + "/" + Constants.SPEAKER_STATE_URL)
                .build();

        Call call = mHttpClient.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                updateFromSpeakerResponse(response);
            }
        });
    }

    private void updateFromSpeakerResponse(Response response) throws IOException {
        try {
            JSONObject json = new JSONObject(response.body().string());

            if (json.has(Constants.SPEAKER_STATE)) {
                mSpeakerState = json.getString(Constants.SPEAKER_STATE);
                broadcastSpeakerStateUpdate();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void broadcastCurrentSongUpdate() {
        if (mUpdateListener != null) {
            mUpdateListener.onCurrentSongUpdate(mSongQueue.get(mSongQueueIndex));
        }

        postNotification();
    }

    private void broadcastSpeakerStateUpdate() {
        if (mUpdateListener != null) {
            mUpdateListener.onStatusUpdate(mSpeakerState);
        }

        postNotification();
    }

    private class CommandReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Constants.INTENT_STOP_CONTROLLER_SERVICE)) {
                stopSelf();
            } else if (intent.getAction().equals(Constants.INTENT_SPEAKER_NEXT_SONG)) {
                nextSong();
            } else if (intent.getAction().equals(Constants.INTENT_SPEAKER_TOGGLE_PLAYBACK)) {
                switch (mSpeakerState) {
                    case Constants.SPEAKER_STATE_PLAYING:
                        pauseSong();
                        break;
                    case Constants.SPEAKER_STATE_STOPPED:
                        resumeSong();
                        break;
                }
            }
        }
    }

    public class LocalBinder extends Binder {
        ControllerService getService() {
            return ControllerService.this;
        }
    }

    public interface UpdateListener {
        void onStatusUpdate(String state);
        void onCurrentSongUpdate(Song song);
    }
}
