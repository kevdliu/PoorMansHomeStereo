package com.twinblade.poormanshomestereo;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.journeyapps.barcodescanner.BarcodeEncoder;

public class SpeakerActivity extends AppCompatActivity
        implements SpeakerService.UpdateListener, View.OnClickListener {

    private SpeakerService mService;

    private ImageView mAlbumCover;
    private ImageView mPlayPause;
    private TextView mTitle;
    private TextView mIpAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        setContentView(R.layout.activity_speaker);

        mAlbumCover = (ImageView) findViewById(R.id.album_cover);
        mTitle = (TextView) findViewById(R.id.title);
        mIpAddress = (TextView) findViewById(R.id.ip_address);

        mPlayPause = (ImageView) findViewById(R.id.play_pause);
        ImageView back = (ImageView) findViewById(R.id.back);
        ImageView next = (ImageView) findViewById(R.id.next);
        Button qrGen = (Button) findViewById(R.id.qr_gen);
        ImageView seekBack = (ImageView) findViewById(R.id.seek_back);
        ImageView seekForward = (ImageView) findViewById(R.id.seek_forward);

        mPlayPause.setOnClickListener(this);
        back.setOnClickListener(this);
        next.setOnClickListener(this);
        qrGen.setOnClickListener(this);
        seekBack.setOnClickListener(this);
        seekForward.setOnClickListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();

        Intent intent = new Intent(this, SpeakerService.class);
        startService(intent);
        bindService(intent, mConnection, 0);

        mIpAddress.setText(Utils.getWifiIpAddress(this));
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mService != null) {
            unbindService(mConnection);
        }
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

    @Override
    public void onCurrentSongUpdate(final Song song) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTitle.setText(song.getTitle());
                mAlbumCover.setImageBitmap(song.getAlbumCover());
            }
        });
    }

    @Override
    public void onStatusUpdate(String status) {
        switch (status) {
            case Constants.SPEAKER_STATUS_PLAYING:
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mPlayPause.setImageResource(R.mipmap.ic_pause);
                    }
                });
                break;

            case Constants.SPEAKER_STATUS_STOPPED:
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mPlayPause.setImageResource(R.mipmap.ic_play);
                    }
                });
                break;
        }
    }

    private final ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            SpeakerService.LocalBinder binder = (SpeakerService.LocalBinder) service;
            mService = binder.getService();
            mService.setUpdateListener(SpeakerActivity.this);
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
                    mService.requestSkipNext();
                }
                break;
            case R.id.back:
                if (mService != null) {
                    mService.requestSkipPrevious();
                }
                break;
            case R.id.qr_gen:
                showQrDialog();
                break;
            case R.id.seek_back:
                if (mService != null) {
                    mService.requestSeekBack();
                }
                break;
            case R.id.seek_forward:
                if (mService != null) {
                    mService.requestSeekForward();
                }
                break;
        }
    }
}

