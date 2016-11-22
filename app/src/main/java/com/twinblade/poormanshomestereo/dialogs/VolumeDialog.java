package com.twinblade.poormanshomestereo.dialogs;

import android.app.AlertDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Toast;

import com.twinblade.poormanshomestereo.Constants;
import com.twinblade.poormanshomestereo.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class VolumeDialog {

    private SeekBar mVolume;
    private ProgressBar mLoading;
    private OkHttpClient mHttpClient;
    private Context mContext;
    private AlertDialog mDialog;

    private String mAddress;

    public void show(Context context, String address, String speakerName) {
        mContext = context;
        mAddress = address;
        mHttpClient = new OkHttpClient.Builder()
                .connectTimeout(Constants.SPEAKER_CONNECTION_TIMEOUT, TimeUnit.SECONDS)
                .build();

        LayoutInflater inflater = LayoutInflater.from(context);
        View root = inflater.inflate(R.layout.dialog_volume, null);

        mLoading = (ProgressBar) root.findViewById(R.id.loading);
        mVolume = (SeekBar) root.findViewById(R.id.volume);
        mVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                new VolumeExecutor().execute(seekBar.getProgress());
            }
        });
        new VolumeLoader().execute();

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(root);
        builder.setMessage(speakerName);
        builder.setPositiveButton("Done", null);

        mDialog = builder.create();
        mDialog.show();
    }

    private void updateVolumeBar(int progress, int max) {
        mVolume.setProgress(progress);
        mVolume.setMax(max);

        mVolume.setVisibility(View.VISIBLE);
        mVolume.animate().withLayer().alpha(1).setDuration(150).start();
        mLoading.animate().withLayer().alpha(0).setDuration(150).withEndAction(new Runnable() {
            @Override
            public void run() {
                mLoading.setVisibility(View.GONE);
            }
        });
    }

    private Integer[] parseVolumeFromResponse(String body) {
        try {
            JSONObject json = new JSONObject(body);

            if (!json.has(Constants.SPEAKER_PROPERTY_VOLUME) || !json.has(Constants.SPEAKER_PROPERTY_MAX_VOLUME)) {
                return null;
            }

            Integer[] volumeInfo = new Integer[2];
            volumeInfo[0] = json.getInt(Constants.SPEAKER_PROPERTY_VOLUME);
            volumeInfo[1] = json.getInt(Constants.SPEAKER_PROPERTY_MAX_VOLUME);

            return volumeInfo;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    private class VolumeLoader extends AsyncTask<Void, Void, Integer[]> {

        @Override
        protected Integer[] doInBackground(Void... params) {
            Request request = new Request.Builder()
                    .url("http://" + mAddress + ":" + Constants.SERVER_PORT + "/" + Constants.SPEAKER_STATE_URL)
                    .build();

            try {
                Response response = mHttpClient.newCall(request).execute();
                return parseVolumeFromResponse(response.body().string());
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(Integer[] volumeInfo) {
            if (mDialog == null || !mDialog.isShowing()) {
                return;
            }

            if (volumeInfo != null) {
                updateVolumeBar(volumeInfo[0], volumeInfo[1]);
            } else {
                mDialog.dismiss();
                Toast.makeText(mContext, "Failed to get volume information", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private class VolumeExecutor extends AsyncTask<Integer, Void, Integer[]> {

        @Override
        protected void onPreExecute() {
            mLoading.setVisibility(View.VISIBLE);
            mLoading.animate().withLayer().alpha(1).setDuration(150).start();
            mVolume.animate().withLayer().alpha(0).setDuration(150).withEndAction(new Runnable() {
                @Override
                public void run() {
                    mVolume.setVisibility(View.GONE);
                }
            });
        }

        @Override
        protected Integer[] doInBackground(Integer... params) {
            FormBody.Builder builder = new FormBody.Builder();
            builder.add(Constants.SPEAKER_COMMAND, Constants.SPEAKER_COMMAND_VOLUME);
            builder.add(Constants.SPEAKER_COMMAND_VOLUME_PROPERTY, Integer.toString(params[0]));
            RequestBody body = builder.build();

            Request request = new Request.Builder()
                    .url("http://" + mAddress + ":" + Constants.SERVER_PORT + "/" + Constants.SPEAKER_COMMAND_URL)
                    .post(body)
                    .build();

            try {
                Response response = mHttpClient.newCall(request).execute();
                return parseVolumeFromResponse(response.body().string());
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(Integer[] volumeInfo) {
            if (mDialog == null || !mDialog.isShowing()) {
                return;
            }

            if (volumeInfo != null) {
                updateVolumeBar(volumeInfo[0], volumeInfo[1]);
            } else {
                mDialog.dismiss();
                Toast.makeText(mContext, "Failed to set volume", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
