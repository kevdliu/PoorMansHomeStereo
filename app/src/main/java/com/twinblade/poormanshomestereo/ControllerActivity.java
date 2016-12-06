package com.twinblade.poormanshomestereo;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.preference.PreferenceManager;
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
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.roughike.bottombar.BottomBar;
import com.roughike.bottombar.OnTabSelectListener;
import com.twinblade.poormanshomestereo.fragments.BaseFragment;
import com.twinblade.poormanshomestereo.fragments.QueueFragment;
import com.twinblade.poormanshomestereo.fragments.SearchFragment;
import com.twinblade.poormanshomestereo.fragments.SongsFragment;
import com.twinblade.poormanshomestereo.fragments.SpeakersFragment;
import com.wooplr.spotlight.SpotlightConfig;
import com.wooplr.spotlight.prefs.PreferencesManager;
import com.wooplr.spotlight.utils.SpotlightSequence;

import io.fabric.sdk.android.Fabric;
import pl.droidsonroids.gif.GifDrawable;
import pl.droidsonroids.gif.GifImageView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class ControllerActivity extends AppCompatActivity
        implements ControllerService.UpdateListener, View.OnClickListener, View.OnLongClickListener {

    private ControllerService mService;
    private Cursor mSongCursor;

    private BottomBar mBottomBar;
    private ImageView mAlbumCover;
    private TextView mTitle;
    private ImageView mPlayPause;

    private CommandReceiver mReceiver;

    private boolean mShowNotificationGuide = false;
    private final HashSet<String> mListeningFragments = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());

        if (Build.VERSION.SDK_INT >= 23 &&
                checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    Constants.STORAGE_PERMISSION_REQUEST_ID);
        } else {
            initViews();
        }
    }

    private void showGuide() {
        SpotlightConfig config = new SpotlightConfig();
        config.setIntroAnimationDuration(250);
        config.setLineAnimationDuration(250);
        config.setMaskColor(Color.argb(200, 0, 0, 0));
        config.setPerformClick(false);

        Resources res = getResources();

        SpotlightSequence seq = SpotlightSequence.getInstance(this, config);
        seq.addSpotlight(findViewById(R.id.tab_speakers),
                R.string.guide_controller_title,
                R.string.guide_controller_text,
                res.getString(R.string.guide_controller_title));
        seq.addSpotlight(findViewById(R.id.tab_speaker_mode),
                R.string.guide_speaker_mode_title,
                R.string.guide_speaker_mode_text,
                res.getString(R.string.guide_speaker_mode_title));
        seq.addSpotlight(findViewById(R.id.next),
                R.string.guide_next_title,
                R.string.guide_next_text,
                res.getString(R.string.guide_next_title));
        seq.addSpotlight(findViewById(R.id.back),
                R.string.guide_back_title,
                R.string.guide_back_text,
                res.getString(R.string.guide_back_title));
        seq.startSequence();

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        mShowNotificationGuide = sp.getBoolean(Constants.SP_SHOW_GUIDE_NOTIFICATION, true);
    }

    private void showNotificationGuideDialogIfNeeded() {
        if (!mShowNotificationGuide) {
            return;
        }

        GifDrawable gifDrawable;
        try {
            gifDrawable = new GifDrawable(getAssets(), "guide_notification.gif");
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(this);
        View root = inflater.inflate(R.layout.dialog_guide, null);

        GifImageView gifView = (GifImageView) root.findViewById(R.id.gif);
        gifView.setImageDrawable(gifDrawable);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.guide_notification_title);
        builder.setView(root);
        builder.setPositiveButton("OK", null);
        builder.show();

        mShowNotificationGuide = false;
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        sp.edit().putBoolean(Constants.SP_SHOW_GUIDE_NOTIFICATION, false).apply();
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

        showNotificationGuideDialogIfNeeded();
    }

    public void playSongAtQueueIndex(int playIndex) {
        if (mService != null) {
            mService.playSongAtQueueIndex(playIndex);
        }

        showNotificationGuideDialogIfNeeded();
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

    public void selectSpeaker(String ip, String name) {
        if (mService != null) {
            mService.selectSpeaker(ip, name);
        }
    }

    public void listenForUpdates(String tag) {
        mListeningFragments.add(tag);

        if (mService != null) {
            mService.broadcastToListener();
        }
    }

    public void displayQueue() {
        mBottomBar.selectTabWithId(R.id.tab_queue);
    }

    private void initViews() {
        setContentView(R.layout.activity_controller);

        mAlbumCover = (ImageView) findViewById(R.id.album_cover);
        mTitle = (TextView) findViewById(R.id.title);

        mPlayPause = (ImageView) findViewById(R.id.play_pause);
        ImageView back = (ImageView) findViewById(R.id.back);
        ImageView next = (ImageView) findViewById(R.id.next);
        RelativeLayout controller = (RelativeLayout) findViewById(R.id.controller);

        mPlayPause.setOnClickListener(this);
        back.setOnClickListener(this);
        back.setOnLongClickListener(this);
        back.setLongClickable(true);
        next.setOnClickListener(this);
        next.setLongClickable(true);
        next.setOnLongClickListener(this);
        controller.setOnClickListener(this);

        showGuide();

        new SongIndexTask().execute();
    }

    private void initFragments() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction initTransaction = fragmentManager.beginTransaction();

        SpeakersFragment speakersFragment = new SpeakersFragment();
        SongsFragment songsFragment = new SongsFragment();
        SearchFragment searchFragment = new SearchFragment();
        QueueFragment queueFragment = new QueueFragment();

        initTransaction.add(R.id.fragment_container, searchFragment, Constants.FRAGMENT_SEARCH);
        initTransaction.add(R.id.fragment_container, songsFragment, Constants.FRAGMENT_SONGS);
        initTransaction.add(R.id.fragment_container, queueFragment, Constants.FRAGMENT_QUEUE);
        initTransaction.add(R.id.fragment_container, speakersFragment, Constants.FRAGMENT_SPEAKERS);

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
                    case R.id.tab_speaker_mode:
                        Intent service = new Intent(ControllerActivity.this, ControllerService.class);
                        stopService(service);
                        finish();

                        Intent speakerMode = new Intent(ControllerActivity.this, SpeakerActivity.class);
                        speakerMode.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(speakerMode);
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
                transaction.attach(fragment);
            } else if (!fragment.isHidden()) {
                transaction.detach(fragment);
            }
        }

        transaction.commit();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == Constants.STORAGE_PERMISSION_REQUEST_ID) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                bindService();
                initViews();
            } else {
                Toast.makeText(this, "Required permissions denied", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.play_pause:
                if (mService != null) {
                    String state = mService.getSpeakerState();

                    if (TextUtils.equals(state, Constants.SPEAKER_STATE_PLAYING)) {
                        ((ImageView) view).setImageResource(R.mipmap.ic_play);
                        mService.pauseSong();
                    } else if (TextUtils.equals(state, Constants.SPEAKER_STATE_STOPPED)) {
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
                displayQueue();
                break;
        }
    }

    @Override
    public boolean onLongClick(View view) {
        switch (view.getId()) {
            case R.id.next:
                if (mService != null) {
                    mService.seekForward();
                }
                return true;
            case R.id.back:
                if (mService != null) {
                    mService.seekBack();
                }
                return true;
            default:
                return false;
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
            return cr.query(uri,
                    Constants.SONG_COLUMNS,
                    MediaStore.Audio.Media.IS_MUSIC + " != ?",
                    new String[] {"0"},
                    sortOrder);
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

        if (mReceiver == null) {
            mReceiver = new CommandReceiver();
        }
        IntentFilter filter = new IntentFilter(Constants.INTENT_EXIT_CONTROLLER_ACTIVITY);
        registerReceiver(mReceiver, filter);

        // stop the speaker service when the controller starts
        Intent service = new Intent(this, SpeakerService.class);
        stopService(service);
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mService != null) {
            unbindService(mConnection);
        }

        try {
            unregisterReceiver(mReceiver);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void bindService() {
        Intent service = new Intent(this, ControllerService.class);
        startService(service);
        bindService(service, mConnection, 0);
    }

    private final ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            ControllerService.LocalBinder binder = (ControllerService.LocalBinder) service;
            mService = binder.getService();
            mService.setUpdateListener(ControllerActivity.this);
            mService.broadcastToListener();
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            mService = null;
        }
    };

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

    @Override
    public void onCurrentSongUpdate(final Song song) {
        if (song != null) {
            new AlbumCoverLoader().execute(song.getAlbumId());
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mTitle.setText(song.getTitle());
                }
            });
        } else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mAlbumCover.setImageResource(R.mipmap.ic_songs);
                    mTitle.setText(getResources().getText(R.string.default_title));
                }
            });
        }

        for (String tag : mListeningFragments) {
            BaseFragment fragment = (BaseFragment) getSupportFragmentManager().findFragmentByTag(tag);
            if (fragment != null) {
                fragment.onCurrentSongUpdate(song);
            }
        }
    }

    private class AlbumCoverLoader extends AsyncTask<String, Void, Bitmap> {

        @Override
        protected Bitmap doInBackground(String... params) {
            String albumId = params[0];
            return Utils.getAlbumCover(getContentResolver(), albumId);
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (bitmap != null) {
                mAlbumCover.setImageBitmap(bitmap);
            }
        }
    }

    private class CommandReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Constants.INTENT_EXIT_CONTROLLER_ACTIVITY)) {
                finish();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.credits:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);

                StringBuilder credits = new StringBuilder();
                credits.append(getResources().getString(R.string.icon_credits));
                credits.append(TextUtils.join(",\n", Constants.ICON_AUTHORS));
                credits.append("\n");
                credits.append(getResources().getString(R.string.lib_credits));
                credits.append(TextUtils.join(",\n", Constants.LIB_SRCS));
                builder.setMessage(credits);

                builder.setTitle(getResources().getString(R.string.menu_credits));
                builder.setPositiveButton("OK", null);

                builder.show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
