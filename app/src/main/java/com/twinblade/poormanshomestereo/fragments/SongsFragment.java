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
        mAdapter = new SongsAdapter((ControllerActivity) getActivity(), mSongCursor, SongsAdapter.MATCH.SONG_ID);
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
}
