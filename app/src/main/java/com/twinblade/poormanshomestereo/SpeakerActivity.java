package com.twinblade.poormanshomestereo;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class SpeakerActivity extends AppCompatActivity {
    private SpeakerService mSpeakerService;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_speaker);
        //mSpeakerService = new SpeakerService(this);
        Intent intent = new Intent(this, SpeakerService.class);
        startService(intent);

    }
}

