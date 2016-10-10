package com.twinblade.poormanshomestereo;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v7.app.NotificationCompat;
import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;
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

    private UpdateListener mUpdateListener;

    private ArrayList<Song> mSongQueue = new ArrayList<>();
    private int mSongQueueIndex = 0;

    private String mSpeakerIp = "";

    @Override
    public void onCreate() {
        mHttpClient = new OkHttpClient();

        try {
            mControllerServer = new ControllerServer();
            mControllerServer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

        mCommandReceiver = new CommandReceiver();
        registerReceiver(mCommandReceiver, new IntentFilter(Constants.INTENT_STOP_CONTROLLER_SERVICE));

        Intent controllerActivity = new Intent(this, ControllerActivity.class);
        PendingIntent controllerActivityPi = PendingIntent.getActivity(this, 0, controllerActivity, 0);

        Intent stopService = new Intent(Constants.INTENT_STOP_CONTROLLER_SERVICE);
        PendingIntent stopServicePi = PendingIntent.getBroadcast(this, 0, stopService, 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setContentTitle("Controller Service Running");
        builder.addAction(0, "Stop", stopServicePi);
        builder.setContentIntent(controllerActivityPi);
        builder.setSmallIcon(R.mipmap.ic_songs);
        startForeground(0, builder.build());

        final Handler handler = new Handler();
        new Thread() {
            @Override
            public void run() {
                // TODO: DO IN ASYNC TASK
                findSpeakers(handler);
            }
        }.start();
    }

    public void setUpdateListener(UpdateListener listener) {
        mUpdateListener = listener;
    }

    public void removeUpdateListener() {
        mUpdateListener = null;
    }

    public void addSongToQueue(Song song) {
        mSongQueue.add(song);
    }

    public void removeSongFromQueue(int index) {
        mSongQueue.remove(index);
    }

    public void playSongNext(Song song) {
        mSongQueue.add(mSongQueueIndex, song);
    }

    public void replaceQueue(ArrayList<Song> queue, int playIndex) {
        mSongQueue = queue;
        playSongAtQueueIndex(playIndex);
    }

    public void playSongAtQueueIndex(int playIndex) {
        if (mSongQueueIndex >= mSongQueue.size()) {
            return;
        }

        mSongQueueIndex = playIndex;
        sendCommandToSpeaker(Constants.SPEAKER_COMMAND_PLAY, 0);

        if (mUpdateListener != null) {
            mUpdateListener.onCurrentSongUpdate(mSongQueue.get(mSongQueueIndex));
        }
    }

    public ArrayList<Song> getSongQueue() {
        return mSongQueue;
    }

    public void playSong() {
        sendCommandToSpeaker(Constants.SPEAKER_COMMAND_PLAY, 0);
    }

    public void pauseSong() {
        sendCommandToSpeaker(Constants.SPEAKER_COMMAND_PAUSE, 0);
    }

    public void resumeSong() {
        sendCommandToSpeaker(Constants.SPEAKER_COMMAND_RESUME, 0);
    }

    public void backSong() {
        if (loadPreviousSong()) {
            sendCommandToSpeaker(Constants.SPEAKER_COMMAND_PLAY, 0);

            if (mUpdateListener != null) {
                mUpdateListener.onCurrentSongUpdate(mSongQueue.get(mSongQueueIndex));
            }
        }
    }

    public void nextSong() {
        if (loadNextSong()) {
            sendCommandToSpeaker(Constants.SPEAKER_COMMAND_PLAY, 0);

            if (mUpdateListener != null) {
                mUpdateListener.onCurrentSongUpdate(mSongQueue.get(mSongQueueIndex));
            }
        }
    }

    public void seekSong(int positionMs) {
        sendCommandToSpeaker(Constants.SPEAKER_COMMAND_SEEK, positionMs);
    }

    public void findSpeakers(Handler handler) {
        try {
            ArrayList<String> speakers = Utils.findSpeakers(this, handler);
            if (!speakers.isEmpty()) {
                mSpeakerIp = speakers.get(0);
            }
        } catch (UnknownHostException | SocketException e) {
            e.printStackTrace();
        }
    }

    private void checkForSpeakerUpdate() {
        Request request = new Request.Builder()
                .url("http://" + mSpeakerIp + ":" + Constants.SERVER_PORT + "/" + Constants.SPEAKER_STATUS_URL)
                .build();

        Call call = mHttpClient.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    JSONObject json = new JSONObject(response.body().string());

                    if (json.has(Constants.SPEAKER_STATUS)) {
                        String status = json.getString(Constants.SPEAKER_STATUS);

                        if (mUpdateListener != null) {
                            mUpdateListener.onStatusUpdate(status);
                        }
                    }

                    if (json.has(Constants.SPEAKER_STATUS_POSITION)) {
                        String seek = json.getString(Constants.SPEAKER_STATUS_POSITION);

                        if (mUpdateListener != null) {
                            mUpdateListener.onSeekPositionUpdate(Long.valueOf(seek));
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
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
        }

        if (mControllerServer != null && mControllerServer.isAlive()) {
            mControllerServer.stop();
        }
    }

    public class ControllerServer extends NanoHTTPD {

        public ControllerServer() {
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
            bis.skip(readStart);

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
        Map<String, List<String>> params = session.getParameters();
        if (params.containsKey(Constants.SPEAKER_STATUS)) {
            final String status = params.get(Constants.SPEAKER_STATUS).get(0);

            switch (status) {
                case Constants.SPEAKER_STATUS_END_OF_SONG:
                    nextSong();
                    break;

                case Constants.SPEAKER_STATUS_PLAYING:
                case Constants.SPEAKER_STATUS_STOPPED:
                    if (mUpdateListener != null) {
                        mUpdateListener.onStatusUpdate(status);
                    }
                    break;
            }
        }

        return newFixedLengthResponse(NanoHTTPD.Response.Status.OK, "", "");
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

    private void sendCommandToSpeaker(String cmd, long seek) {
        FormBody.Builder builder = new FormBody.Builder();
        builder.add(Constants.SPEAKER_COMMAND, cmd);
        builder.add(Constants.SPEAKER_COMMAND_SEEK, Long.toString(seek));
        RequestBody body = builder.build();

        Request request = new Request.Builder()
                .url("https://" + mSpeakerIp + ":" + Constants.SERVER_PORT + "/" + Constants.SPEAKER_COMMAND_URL)
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
                //
            }
        });
    }

    private class CommandReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Constants.INTENT_STOP_CONTROLLER_SERVICE)) {
                stopSelf();
            }
        }
    }

    public class LocalBinder extends Binder {
        ControllerService getService() {
            return ControllerService.this;
        }
    }

    public interface UpdateListener {
        void onStatusUpdate(String status);
        void onSeekPositionUpdate(long position);
        void onCurrentSongUpdate(Song song);
    }
}
