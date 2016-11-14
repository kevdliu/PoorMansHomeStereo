package com.twinblade.poormanshomestereo;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.journeyapps.barcodescanner.BarcodeEncoder;

public class SpeakerActivity extends AppCompatActivity implements View.OnClickListener {

    private SpeakerService mService;

    // private ImageView mPlayPause;
    // private TextView mSongTitle;
    private TextView mSpeakerName;
    private TextView mIpAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        setContentView(R.layout.activity_speaker);

        View controller = findViewById(R.id.controller);
        controller.setVisibility(View.GONE);

        // mSongTitle = (TextView) findViewById(R.id.title);
        mSpeakerName = (TextView) findViewById(R.id.speaker_name);
        mIpAddress = (TextView) findViewById(R.id.ip_address);

        // mPlayPause = (ImageView) findViewById(R.id.play_pause);
        // ImageView back = (ImageView) findViewById(R.id.back);
        // ImageView next = (ImageView) findViewById(R.id.next);
        Button qrGen = (Button) findViewById(R.id.qr_gen);
        Button setName = (Button) findViewById(R.id.set_name);
        Button exit = (Button) findViewById(R.id.exit);

        // mPlayPause.setOnClickListener(this);
        // back.setOnClickListener(this);
        // next.setOnClickListener(this);
        qrGen.setOnClickListener(this);
        setName.setOnClickListener(this);
        exit.setOnClickListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();

        Intent intent = new Intent(this, SpeakerService.class);
        startService(intent);
        bindService(intent, mConnection, 0);

        mIpAddress.setText(Utils.getWifiIpAddress(this));
        mSpeakerName.setText(getSpeakerName());
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mService != null) {
            unbindService(mConnection);
        }
    }

    private String getSpeakerName() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        return sp.getString(Constants.SPEAKER_PROPERTY_NAME, Build.MODEL);
    }

    private void showSetNameDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Speaker Name");

        final EditText input = new EditText(this);
        input.setText(getSpeakerName());
        builder.setView(input);

        builder.setNegativeButton("Cancel", null);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String entry = input.getText().toString();
                SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(SpeakerActivity.this);
                sp.edit().putString(Constants.SPEAKER_PROPERTY_NAME, entry).apply();
                mSpeakerName.setText(entry);
            }
        });

        final AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null){
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }
        dialog.show();

        input.requestFocus();
    }

    private void showQrDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        ImageView qrView = new ImageView(this);
        BarcodeEncoder encoder = new BarcodeEncoder();
        try {
            Bitmap qrCode = encoder.encodeBitmap(Utils.getWifiIpAddress(this), BarcodeFormat.QR_CODE, 500, 500);
            qrView.setImageBitmap(qrCode);

            builder.setView(qrView);
            builder.setNegativeButton("Done", null);
            builder.show();
        } catch (WriterException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error generating QR code", Toast.LENGTH_SHORT).show();
        }
    }

    /**
    @Override
    public void onCurrentSongUpdate(final Song song) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mSongTitle.setText(song.getTitle());
            }
        });
    }

    @Override
    public void onStatusUpdate(String state) {
        switch (state) {
            case Constants.SPEAKER_STATE_PLAYING:
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mPlayPause.setImageResource(R.mipmap.ic_pause);
                    }
                });
                break;

            case Constants.SPEAKER_STATE_STOPPED:
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mPlayPause.setImageResource(R.mipmap.ic_play);
                    }
                });
                break;
        }
    }
    */

    private final ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            SpeakerService.LocalBinder binder = (SpeakerService.LocalBinder) service;
            mService = binder.getService();
            // mService.setUpdateListener(SpeakerActivity.this);
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            mService = null;
        }
    };

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.play_pause:
                if (mService != null) {
                    mService.requestTogglePlayback();
                }
                break;

            case R.id.next:
                if (mService != null) {
                    mService.requestNextSong();
                }
                break;

            case R.id.back:
                if (mService != null) {
                    mService.requestPreviousSong();
                }
                break;

            case R.id.qr_gen:
                showQrDialog();
                break;

            case R.id.set_name:
                showSetNameDialog();
                break;

            case R.id.exit:
                Intent service = new Intent(this, SpeakerService.class);
                stopService(service);
                finish();
                break;
        }
    }
}

