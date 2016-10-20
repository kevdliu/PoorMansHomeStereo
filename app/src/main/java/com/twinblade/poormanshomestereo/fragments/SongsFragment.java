package com.twinblade.poormanshomestereo.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.twinblade.poormanshomestereo.Constants;
import com.twinblade.poormanshomestereo.ControllerActivity;
import com.twinblade.poormanshomestereo.R;
import com.twinblade.poormanshomestereo.adapters.SongsAdapter;

public class SongsFragment extends BaseFragment {

    @Override
    public void onCreate(Bundle saved) {
        super.onCreate(saved);

        mSongCursor = getController().getSongCursor();
        mAdapter = new SongsAdapter((ControllerActivity) getActivity(), mSongCursor);
        getController().listenForUpdates(Constants.FRAGMENT_SONGS);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_songs, container, false);

        mSongList = (ListView) root.findViewById(R.id.songs_list);
        mSongList.setAdapter(mAdapter);
        registerForInteraction();

        return root;
    }

    /**
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
                Log.e("PMHS", "HIGHLIGHT " + currentSong.getTitle());
                titleView.setTextColor(Color.parseColor("#2196F3"));
                artistAlbum.setTextColor(Color.parseColor("#2196F3"));
            } else {
                Log.e("PMHS", "SKIP " + song.getTitle());
                titleView.setTextColor(Color.BLACK);
                artistAlbum.setTextColor(Color.BLACK);
            }
        }
    }
     */
}
