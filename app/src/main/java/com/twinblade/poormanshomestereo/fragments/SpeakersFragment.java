package com.twinblade.poormanshomestereo.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.twinblade.poormanshomestereo.ControllerActivity;
import com.twinblade.poormanshomestereo.R;
import com.twinblade.poormanshomestereo.adapters.SpeakersAdapter;

public class SpeakersFragment extends Fragment {

    private SpeakersAdapter mAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_speakers, container, false);

        ListView speakerList = (ListView) root.findViewById(R.id.speakers_list);
        mAdapter = new SpeakersAdapter(getController());
        speakerList.setAdapter(mAdapter);

        speakerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                getController().selectSpeaker(position);
                mAdapter.refreshList();
            }
        });

        return root;
    }

    public void updateList() {
        if (mAdapter != null) {
            mAdapter.refreshList();
        }
    }

    private ControllerActivity getController() {
        return ((ControllerActivity) getActivity());
    }
}
