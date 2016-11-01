package com.twinblade.poormanshomestereo.fragments;

import android.database.Cursor;
import android.database.MatrixCursor;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.twinblade.poormanshomestereo.Constants;
import com.twinblade.poormanshomestereo.ControllerActivity;
import com.twinblade.poormanshomestereo.R;
import com.twinblade.poormanshomestereo.Song;
import com.twinblade.poormanshomestereo.Utils;
import com.twinblade.poormanshomestereo.adapters.SongsAdapter;

import java.util.ArrayList;

public class QueueFragment extends BaseFragment {

    @Override
    public void onCreate(Bundle saved) {
        super.onCreate(saved);

        mAdapter = new SongsAdapter((ControllerActivity) getActivity(), mSongCursor);
        getController().listenForUpdates(Constants.FRAGMENT_QUEUE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_songs, container, false);

        mSongList = (ListView) root.findViewById(R.id.songs_list);
        mSongList.setAdapter(mAdapter);
        registerForInteraction();

        updateQueueCursor();
        mAdapter.changeCursor(mSongCursor);

        return root;
    }

    @Override
    public void onViewCreated(View view, Bundle saved) {
        super.onViewCreated(view, saved);

        mSongList.post(new Runnable() {
            @Override
            public void run() {
                int position = getController().getCurrentSongQueueIndex();
                mSongList.requestFocusFromTouch();
                mSongList.setSelection(position);
            }
        });
    }

    private void updateQueueCursor() {
        ArrayList<Song> songQueue = getController().getSongQueue();
        MatrixCursor cursor = new MatrixCursor(Constants.SONG_COLUMNS);
        for (Song song : songQueue) {
           Object[] row = new Object[]{song.getId(),
                   song.getTitle(),
                   song.getArtist(),
                   song.getAlbum(),
                   song.getAlbumId(),
                   song.getFile().getAbsolutePath()};
           cursor.addRow(row);
        }

        mSongCursor = cursor;
    }

    @Override
    void registerForInteraction() {
        registerForContextMenu(mSongList);
        mSongList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                getController().playSongAtQueueIndex(position);
            }
        });
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;

        Cursor cursor = (Cursor) mAdapter.getItem(info.position);
        Song song = Utils.getSongFromCursor(cursor);
        menu.setHeaderTitle(song.getTitle());

        menu.add(Menu.NONE, Constants.MENU_REMOVE_FROM_QUEUE, Menu.NONE, "Remove from Queue");
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();

        switch (item.getItemId()) {
            case Constants.MENU_REMOVE_FROM_QUEUE:
                //TODO: update currently playing song
                getController().removeSongFromQueue(info.position);
                updateQueueCursor();
                mAdapter.changeCursor(mSongCursor);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }
}
