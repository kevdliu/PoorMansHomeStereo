package com.twinblade.poormanshomestereo;

import android.Manifest;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
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
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.roughike.bottombar.BottomBar;
import com.roughike.bottombar.OnTabSelectListener;
import com.twinblade.poormanshomestereo.fragments.QueueFragment;
import com.twinblade.poormanshomestereo.fragments.SearchFragment;
import com.twinblade.poormanshomestereo.fragments.SongsFragment;
import com.twinblade.poormanshomestereo.fragments.SpeakersFragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ControllerActivity extends AppCompatActivity
        implements ControllerService.UpdateListener, View.OnClickListener {

    private ControllerService mService;
    private Cursor mSongCursor;
    private ContentResolver mContentResolver;

    private BottomBar mBottomBar;
    private ImageView mAlbumCover;
    private TextView mTitle;
    private ImageView mPlayPause;

    private HashMap<String, ControllerService.UpdateListener> mUpdateListeners = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= 23 &&
                checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
        } else {
            initViews();
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
        }

        return new ArrayList<>();
    }

    public Song getCurrentSong() {
        if (mService != null) {
            return mService.getCurrentSong();
        }

        return null;
    }

    public int getCurrentSongQueueIndex() {
        if (mService != null) {
            return mService.getCurrentSongQueueIndex();
        }

        return 0;
    }

    public String getSelectedSpeaker() {
        if (mService != null) {
            return mService.getSelectedSpeaker();
        }

        return null;
    }

    public void selectSpeaker(String speaker) {
        if (mService != null) {
            mService.selectSpeaker(speaker);
        }
    }

    public void addUpdateListener(String tag, ControllerService.UpdateListener listener) {
        mUpdateListeners.put(tag, listener);

        if (mService != null) {
            mService.broadcastToListener(this);
        }
    }

    private void initViews() {
        setContentView(R.layout.activity_controller);

        mContentResolver = getContentResolver();
        mAlbumCover = (ImageView) findViewById(R.id.album_cover);
        mTitle = (TextView) findViewById(R.id.title);

        mPlayPause = (ImageView) findViewById(R.id.play_pause);
        ImageView back = (ImageView) findViewById(R.id.back);
        ImageView next = (ImageView) findViewById(R.id.next);
        RelativeLayout controller = (RelativeLayout) findViewById(R.id.controller);

        mPlayPause.setOnClickListener(this);
        back.setOnClickListener(this);
        next.setOnClickListener(this);
        controller.setOnClickListener(this);

        new SongIndexTask().execute();
    }

    private void initFragments() {
        FragmentManager fragmentManager = getSupportFragmentManager();
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

        mBottomBar = (BottomBar) findViewById(R.id.nav_bar);
        mBottomBar.setOnTabSelectListener(new OnTabSelectListener() {
            @Override
            public void onTabSelected(@IdRes int tabId) {
                int currentPosition = mBottomBar.getCurrentTabPosition();
                int newPosition = mBottomBar.findPositionForTabWithId(tabId);

                int animIn = 0;
                int animOut = 0;
                if (currentPosition < newPosition) {
                    animIn = R.anim.slide_left_in;
                    animOut = R.anim.slide_left_out;
                } else if (currentPosition > newPosition) {
                    animIn = R.anim.slide_right_in;
                    animOut = R.anim.slide_right_out;
                }

                switch (tabId) {
                    case R.id.tab_speakers:
                        showFragmentByTag(Constants.FRAGMENT_SPEAKERS, animIn, animOut);
                        break;
                    case R.id.tab_queue:
                        showFragmentByTag(Constants.FRAGMENT_QUEUE, animIn, animOut);
                        break;
                    case R.id.tab_songs:
                        showFragmentByTag(Constants.FRAGMENT_SONGS, animIn, animOut);
                        break;
                    case R.id.tab_search:
                        showFragmentByTag(Constants.FRAGMENT_SEARCH, animIn, animOut);
                        break;
                }
            }
        });
    }

    private void showFragmentByTag(String tag, int animIn, int animOut) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.setCustomAnimations(animIn, animOut);

        List<Fragment> fragments = fragmentManager.getFragments();
        for (Fragment fragment : fragments) {
            String fragmentTag = fragment.getTag();
            if (TextUtils.equals(tag, fragmentTag)) {
                transaction.show(fragment);
            } else if (!fragment.isHidden()) {
                transaction.hide(fragment);
            }
        }

        transaction.commit();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            bindService();
            initViews();
        } else {
            Toast.makeText(this, "Required permissions denied", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.play_pause:
                if (mService != null) {
                    String status = mService.getSpeakerState();

                    if (TextUtils.equals(status, Constants.SPEAKER_STATUS_PLAYING)) {
                        ((ImageView) view).setImageResource(R.mipmap.ic_play);
                        mService.pauseSong();
                    } else if (TextUtils.equals(status, Constants.SPEAKER_STATUS_STOPPED)) {
                        ((ImageView) view).setImageResource(R.mipmap.ic_pause);
                        mService.resumeSong();
                    }
                }
                break;

            case R.id.next:
                if (mService != null) {
                    mService.nextSong();
                }
                break;

            case R.id.back:
                if (mService != null) {
                    mService.previousSong();
                }
                break;

            case R.id.controller:
                mBottomBar.selectTabWithId(R.id.tab_queue);
                break;
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

    @Override
    public void onResume() {
        super.onResume();

        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                bindService();
            }
        } else {
            bindService();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        unbindService(mConnection);
    }

    private void bindService() {
        Intent service = new Intent(this, ControllerService.class);
        startService(service);
        bindService(service, mConnection, 0);
    }

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            ControllerService.LocalBinder binder = (ControllerService.LocalBinder) service;
            mService = binder.getService();
            mService.setUpdateListener(ControllerActivity.this);
            mService.broadcastToListener(ControllerActivity.this);
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            mService = null;
        }
    };

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

    @Override
    public void onCurrentSongUpdate(Song song) {
        if (song != null) {
            new AlbumCoverLoader().execute(song.getAlbumId());
            mTitle.setText(song.getTitle());
        } else {
            mAlbumCover.setImageResource(R.mipmap.ic_songs);
            mTitle.setText("<Title>");
        }

        for (ControllerService.UpdateListener listener : mUpdateListeners.values()) {
            listener.onCurrentSongUpdate(song);
        }
    }

    /**
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.controller, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            //
        }
        return super.onOptionsItemSelected(item);
    }
    */

    private class AlbumCoverLoader extends AsyncTask<String, Void, Bitmap> {

        @Override
        protected Bitmap doInBackground(String... params) {
            String albumId = params[0];

            Cursor cursor = mContentResolver.query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                    new String[] {MediaStore.Audio.Albums.ALBUM_ART},
                    MediaStore.Audio.Albums._ID + " = ?",
                    new String[] {albumId},
                    null);

            if (cursor != null && cursor.moveToFirst()) {
                String path = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART));
                cursor.close();

                return BitmapFactory.decodeFile(path);
            }

            return null;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (bitmap != null) {
                mAlbumCover.setImageBitmap(bitmap);
            }
        }
    }
}
