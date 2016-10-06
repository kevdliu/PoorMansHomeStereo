package com.twinblade.poormanshomestereo;

import android.Manifest;
import android.support.v4.app.Fragment;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.annotation.IdRes;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.widget.Toast;

import com.roughike.bottombar.BottomBar;
import com.roughike.bottombar.OnTabSelectListener;
import com.twinblade.poormanshomestereo.fragments.QueueFragment;
import com.twinblade.poormanshomestereo.fragments.SearchFragment;
import com.twinblade.poormanshomestereo.fragments.SongsFragment;
import com.twinblade.poormanshomestereo.fragments.SpeakersFragment;

import java.util.ArrayList;
import java.util.List;

public class ControllerActivity extends AppCompatActivity
        implements ControllerService.SpeakerUpdateListener {

    private ControllerService mService;
    private Cursor mSongCursor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
        } else {
            init();
        }
    }

    public Cursor getSongCursor() {
        return mSongCursor;
    }

    public void addSongToQueue(Song song) {
        if (mService != null) {
            mService.addSongToQueue(song);
        }
    }

    public void removeSongFromQueue(int index) {
        if (mService != null) {
            mService.removeSongFromQueue(index);
        }
    }

    public void playSongNext(Song song) {
        if (mService != null) {
            mService.playSongNext(song);
        }
    }

    public void replaceQueue(ArrayList<Song> queue, int playIndex) {
        if (mService != null) {
            mService.replaceQueue(queue, playIndex);
        }
    }

    public void playSongAtQueueIndex(int playIndex) {
        if (mService != null) {
            mService.playSongAtQueueIndex(playIndex);
        }
    }

    public ArrayList<Song> getSongQueue() {
        if (mService != null) {
            return mService.getSongQueue();
        } else {
            return new ArrayList<>();
        }
    }

    private void init() {
        setContentView(R.layout.activity_controller);

        new SongIndexTask().execute();
    }

    private void initFragments() {
        final FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction initTransaction = fragmentManager.beginTransaction();

        SpeakersFragment speakersFragment = new SpeakersFragment();
        SongsFragment songsFragment = new SongsFragment();
        SearchFragment searchFragment = new SearchFragment();
        QueueFragment queueFragment = new QueueFragment();

        initTransaction.add(R.id.fragment_container, speakersFragment, Constants.FRAGMENT_SPEAKERS);
        initTransaction.add(R.id.fragment_container, songsFragment, Constants.FRAGMENT_SONGS);
        initTransaction.add(R.id.fragment_container, searchFragment, Constants.FRAGMENT_SEARCH);
        initTransaction.add(R.id.fragment_container, queueFragment, Constants.FRAGMENT_QUEUE);
        initTransaction.commit();
        fragmentManager.executePendingTransactions();

        BottomBar navBar = (BottomBar) findViewById(R.id.nav_bar);
        navBar.setOnTabSelectListener(new OnTabSelectListener() {
            @Override
            public void onTabSelected(@IdRes int tabId) {
                switch (tabId) {
                    case R.id.tab_speakers:
                        showFragmentByTag(Constants.FRAGMENT_SPEAKERS);
                        break;
                    case R.id.tab_queue:
                        showFragmentByTag(Constants.FRAGMENT_QUEUE);
                        break;
                    case R.id.tab_songs:
                        showFragmentByTag(Constants.FRAGMENT_SONGS);
                        break;
                    case R.id.tab_search:
                        showFragmentByTag(Constants.FRAGMENT_SEARCH);
                        break;
                }
            }
        });
    }

    private void showFragmentByTag(String tag) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();

        List<Fragment> fragments = fragmentManager.getFragments();
        for (Fragment fragment : fragments) {
            String fragmentTag = fragment.getTag();
            if (TextUtils.equals(tag, fragmentTag)) {
                transaction.show(fragment);
            } else {
                transaction.hide(fragment);
            }
        }

        transaction.commit();
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
    protected void onResume() {
        super.onResume();
        Intent intent = new Intent(this, ControllerService.class);
        bindService(intent, mConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mService != null) {
            unbindService(mConnection);
        }
    }

    private class SongIndexTask extends AsyncTask<Void, Integer, Cursor> {

        private ProgressDialog mDialog;

        @Override
        protected void onPreExecute() {
            mDialog = new ProgressDialog(ControllerActivity.this);
            mDialog.setMessage("Indexing...");
            mDialog.setIndeterminate(true);
            mDialog.setCancelable(false);
            mDialog.show();
        }

        @Override
        protected Cursor doInBackground(Void... params) {
            ContentResolver cr = ControllerActivity.this.getContentResolver();

            Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";
            Cursor cursor = cr.query(uri,
                    Constants.SONG_COLUMNS,
                    MediaStore.Audio.Media.IS_MUSIC + " != ?",
                    new String[] {"0"},
                    sortOrder);

            return cursor;
        }

        @Override
        protected void onPostExecute(Cursor result) {
            mDialog.dismiss();

            mSongCursor = result;
            initFragments();
        }
    }

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            ControllerService.LocalBinder binder = (ControllerService.LocalBinder) service;
            mService = binder.getService();
            mService.setSpeakerUpdateListener(ControllerActivity.this);
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            mService.removeSpeakerUpdateListener();
            mService = null;
        }
    };

    @Override
    public void onSpeakerStatusUpdate(String status) {

    }

    @Override
    public void onSpeakerSeekPositionUpdate(long position) {

    }
}
