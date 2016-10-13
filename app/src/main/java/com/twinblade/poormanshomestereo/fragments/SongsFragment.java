package com.twinblade.poormanshomestereo.fragments;

import android.graphics.Color;
import android.support.v4.app.Fragment;
import android.database.Cursor;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.twinblade.poormanshomestereo.Constants;
import com.twinblade.poormanshomestereo.ControllerActivity;
import com.twinblade.poormanshomestereo.ControllerService;
import com.twinblade.poormanshomestereo.R;
import com.twinblade.poormanshomestereo.Song;
import com.twinblade.poormanshomestereo.Utils;
import com.twinblade.poormanshomestereo.adapters.SongsAdapter;

import java.util.ArrayList;

public class SongsFragment extends Fragment implements ControllerService.UpdateListener {

    public ListView mSongList;
    public SongsAdapter mAdapter;
    public Cursor mSongCursor;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_songs, container, false);

        mSongCursor = getController().getSongCursor();

        mSongList = (ListView) root.findViewById(R.id.songs_list);
        mAdapter = new SongsAdapter((ControllerActivity) getActivity(), mSongCursor);
        mSongList.setAdapter(mAdapter);

        getController().addUpdateListener(this.getClass().getCanonicalName(), this);
        registerForInteraction();

        return root;
    }

    public void updateSongHighlight() {
        Song currentSong = getController().getCurrentSong();

        int firstVisible = mSongList.getFirstVisiblePosition();
        int lastVisible = mSongList.getLastVisiblePosition();
        for (int i = 0; i <= lastVisible - firstVisible; i++) {
            Song song = Utils.getSongFromCursor((Cursor) mAdapter.getItem(firstVisible + i));
            View row = mSongList.getChildAt(i);

            if (song == null || row == null) {
                continue;
            }

            TextView titleView = (TextView) row.findViewById(R.id.title);
            TextView artistAlbum = (TextView) row.findViewById(R.id.artist_album);

            if (currentSong != null && TextUtils.equals(song.getId(), currentSong.getId())) {
                titleView.setTextColor(Color.parseColor("#2196F3"));
                artistAlbum.setTextColor(Color.parseColor("#2196F3"));
            } else {
                titleView.setTextColor(Color.BLACK);
                artistAlbum.setTextColor(Color.BLACK);
            }
        }
    }

    public void registerForInteraction() {
        registerForContextMenu(mSongList);
        mSongList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                ArrayList<Song> queue = new ArrayList<>();
                for (int i = 0; i < mSongCursor.getCount(); i++) {
                    Cursor cursor = (Cursor) mAdapter.getItem(i);
                    queue.add(Utils.getSongFromCursor(cursor));
                }

                getController().replaceQueue(queue, position);
            }
        });
    }

    public ControllerActivity getController() {
        return ((ControllerActivity) getActivity());
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;

        Cursor cursor = (Cursor) mAdapter.getItem(info.position);
        Song song = Utils.getSongFromCursor(cursor);
        menu.setHeaderTitle(song.getTitle());

        menu.add(Menu.NONE, Constants.MENU_PLAY_NEXT, Menu.NONE, "Play Next");
        menu.add(Menu.NONE, Constants.MENU_ADD_TO_QUEUE, Menu.NONE, "Add to Queue");
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        Cursor cursor = (Cursor) mAdapter.getItem(info.position);
        Song song = Utils.getSongFromCursor(cursor);

        switch (item.getItemId()) {
            case Constants.MENU_PLAY_NEXT:
                getController().playSongNext(song);
                return true;
            case Constants.MENU_ADD_TO_QUEUE:
                getController().addSongToQueue(song);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (mAdapter != null) {
            mAdapter.changeCursor(null);
        }
    }

    @Override
    public void onStatusUpdate(String status) {
        //
    }

    @Override
    public void onCurrentSongUpdate(Song song) {
        if (mSongList != null) {
            updateSongHighlight();
        }
    }
}
