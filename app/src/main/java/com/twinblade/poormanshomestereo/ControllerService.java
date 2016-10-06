package com.twinblade.poormanshomestereo;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.text.TextUtils;

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
    private Handler mHandler;

    private SpeakerUpdateListener mSpeakerUpdateListener;

    private ArrayList<Song> mSongQueue = new ArrayList<>();
    private int mSongQueueIndex = 0;

    @Override
    public void onCreate() {
        mHandler = new Handler();
        mHttpClient = new OkHttpClient();

        try {
            mControllerServer = new ControllerServer();
            mControllerServer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setSpeakerUpdateListener(SpeakerUpdateListener listener) {
        mSpeakerUpdateListener = listener;
    }

    public void removeSpeakerUpdateListener() {
        mSpeakerUpdateListener = null;
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
        mSongQueueIndex = playIndex;
        sendCommandToSpeaker(Constants.SPEAKER_COMMAND_PLAY, -1);
    }

    public ArrayList<Song> getSongQueue() {
        return mSongQueue;
    }

    private void checkForSpeakerUpdate() {
        Request request = new Request.Builder()
                .url("http://192.168.1.217:6969/state.json")
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
                        mSpeakerUpdateListener.onSpeakerStatusUpdate(status);
                    }

                    if (json.has(Constants.SPEAKER_STATUS_SEEK)) {
                        String seek = json.getString(Constants.SPEAKER_STATUS_SEEK);
                        mSpeakerUpdateListener.onSpeakerSeekPositionUpdate(Long.valueOf(seek));
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
        if (mControllerServer != null && mControllerServer.isAlive()) {
            mControllerServer.stop();
        }
    }

    public class ControllerServer extends NanoHTTPD {

        public ControllerServer() {
            super(Constants.CONTROLLER_SERVER_PORT);
        }

        @Override
        public Response serve(IHTTPSession session) {
            if (session.getMethod() == Method.GET && session.getUri().startsWith("/res.mp3")) {
                return serveMediaFile(session);
            } else if (session.getMethod() == Method.POST && session.getUri().startsWith("/msg")) {
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
            if (TextUtils.equals(status, Constants.SPEAKER_STATUS_END_OF_SONG)) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (loadNextSong()) {
                            sendCommandToSpeaker(Constants.SPEAKER_COMMAND_PLAY, -1);
                        }
                    }
                });
            }
        }

        return newFixedLengthResponse(NanoHTTPD.Response.Status.OK, "", "");
    }

    public boolean loadNextSong() {
        if (!mSongQueue.isEmpty() && mSongQueueIndex + 1 < mSongQueue.size()) {
            mSongQueueIndex++;
            return true;
        }

        return false;
    }

    public void sendCommandToSpeaker(String cmd, long seek) {
        FormBody.Builder builder = new FormBody.Builder();
        builder.add(Constants.SPEAKER_COMMAND, cmd);
        if (seek != -1) {
            builder.add(Constants.SPEAKER_COMMAND_SEEK, Long.toString(seek));
        }
        RequestBody body = builder.build();

        Request request = new Request.Builder()
                .url("https://en.wikipedia.org/w/index.php")
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

    public class LocalBinder extends Binder {
        ControllerService getService() {
            return ControllerService.this;
        }
    }

    public interface SpeakerUpdateListener {
        public void onSpeakerStatusUpdate(String status);
        public void onSpeakerSeekPositionUpdate(long position);
    }
}
