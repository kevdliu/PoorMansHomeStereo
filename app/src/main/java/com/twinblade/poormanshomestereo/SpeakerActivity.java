package com.twinblade.poormanshomestereo;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class SpeakerActivity extends AppCompatActivity implements SpeakerService.UpdateListener {
    private SpeakerService mSpeakerService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        setContentView(R.layout.activity_speaker);
        Intent intent = new Intent(this, SpeakerService.class);
        startService(intent);
        bindService(intent, mConnection, 0);

        final ImageView play_pause = (ImageView) findViewById(R.id.play_pause);
        play_pause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e("PMHS", "Play_pause clicked");
                if (mSpeakerService != null) {
                    // Get media player state
                    mSpeakerService.sendPlayPause();
                }
            }
        });
        final ImageView previous = (ImageView) findViewById(R.id.previous);
        previous.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e("PMHS", "Previous clicked");
                if (mSpeakerService != null) {
                    // Get media player state
                    mSpeakerService.sendSkipPrevious();
                }
            }
        });
        final ImageView next = (ImageView) findViewById(R.id.next);
        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e("PMHS", "Next clicked");
                if (mSpeakerService != null) {
                    // Get media player state
                    mSpeakerService.sendSkipNext();
                }
            }
        });

    }

    @Override
    public void onPause() {
        super.onPause();

        if (mSpeakerService != null) {
            unbindService(mConnection);
        }
    }


    @Override
    public void onCurrentSongUpdate(final Song song) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView songInfo = (TextView) findViewById(R.id.current_song_info);
                songInfo.setText(song.getTitle() + " by " + song.getArtist());
                Log.e("PMHS", "Setting text to " + songInfo.getText());
            }
        });

    }

    @Override
    public void onStatusUpdate(final String status) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ImageView play_pause = (ImageView) findViewById(R.id.play_pause);

                if (Constants.SPEAKER_STATUS_PLAYING.equals(status)) {
                    Log.e("PMHS", "Setting to pause...");
                    play_pause.setImageResource(R.mipmap.ic_pause);
                } else if (Constants.SPEAKER_STATUS_STOPPED.equals(status)) {
                    Log.e("PMHS", "Setting to pplay...");
                    play_pause.setImageResource(R.mipmap.ic_play);
                }
            }
        });

    }


    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            SpeakerService.LocalBinder binder = (SpeakerService.LocalBinder) service;
            mSpeakerService = binder.getService();
            mSpeakerService.setUpdateListener(SpeakerActivity.this);
        }

        public void onServiceDisconnected(ComponentName className) {
            mSpeakerService = null;
        }
    };
}

