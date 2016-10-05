package com.twinblade.poormanshomestereo;

import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.util.HashMap;

public class ControllerActivity extends AppCompatActivity
        implements ControllerService.MessageListener, Button.OnClickListener {

    private ControllerService mService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
        } else {
            init();
        }
    }

    private void init() {
        setContentView(R.layout.activity_controller);

        Button play = (Button) findViewById(R.id.play);
        play.setOnClickListener(this);

        Intent service = new Intent(this, ControllerService.class);
        startService(service);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            init();
        } else {
            Toast.makeText(this, "Required permissions denied", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(this, ControllerService.class);
        bindService(intent, mConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mService != null) {
            unbindService(mConnection);
        }
    }

    public static HashMap<String, File> getFileMap() {
        HashMap<String, File> fileMap = new HashMap<>();
        updateFileMapRecursive(fileMap, Constants.DOWNLOADS_DIR.getAbsolutePath());
        return fileMap;
    }

    private static void updateFileMapRecursive(HashMap<String, File> fileMap, String dirName) {
        File directory = new File(dirName);

        File[] files = directory.listFiles();
        for (File file : files) {
            if (file.isFile() && isVideo(file.getName())) {
                fileMap.put(Integer.toString(file.getName().hashCode()), file);
            } else if (file.isDirectory()) {
                updateFileMapRecursive(fileMap, file.getAbsolutePath());
            }
        }
    }

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            ControllerService.LocalBinder binder = (ControllerService.LocalBinder) service;
            mService = binder.getService();
            mService.setMessageListener(ControllerActivity.this);
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            mService = null;
        }
    };

    @Override
    public void onMessage(String msg) {

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.play:
                if (mService != null) {
                    mService.sendMessageToSpeaker(Constants.SPEAKER_COMMAND_PLAY, null);
                }
                break;
        }
    }
}
