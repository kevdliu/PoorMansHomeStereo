package com.twinblade.poormanshomestereo.fragments;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.twinblade.poormanshomestereo.ControllerActivity;
import com.twinblade.poormanshomestereo.R;
import com.twinblade.poormanshomestereo.Utils;
import com.twinblade.poormanshomestereo.adapters.SpeakersAdapter;

import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class SpeakersFragment extends Fragment {

    private SpeakersAdapter mAdapter;
    private ListView mSpeakerList;
    private LinearLayout mLoadingView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_speakers, container, false);

        Button refresh = (Button) root.findViewById(R.id.refresh);
        refresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new SpeakerDiscovery().execute();
            }
        });

        mLoadingView = (LinearLayout) root.findViewById(R.id.loading);
        mSpeakerList = (ListView) root.findViewById(R.id.speakers_list);
        mAdapter = new SpeakersAdapter(getController(), new ArrayList<String>());
        mSpeakerList.setAdapter(mAdapter);

        mSpeakerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                getController().selectSpeaker((String) mAdapter.getItem(position));
                mSpeakerList.invalidateViews();
            }
        });

        new SpeakerDiscovery().execute();

        return root;
    }

    private ControllerActivity getController() {
        return ((ControllerActivity) getActivity());
    }

    private class SpeakerDiscovery extends AsyncTask<Void, Void, ArrayList<String>> {

        private Handler mHandler;
        private HandlerThread mHandlerThread;

        @Override
        protected void onPreExecute() {
            mLoadingView.setVisibility(View.VISIBLE);
            mLoadingView.animate().withLayer().alpha(1).setDuration(250).start();

            mHandlerThread = new HandlerThread("SpeakerDiscoveryTimeout");
            mHandlerThread.start();
            mHandler = new Handler(mHandlerThread.getLooper());
        }

        @Override
        protected ArrayList<String> doInBackground(Void... params) {
            ArrayList<String> speakers = new ArrayList<>();
            try {
                speakers = Utils.findSpeakers(getController(), mHandler);

                String currentSpeaker = getController().getSelectedSpeaker();
                if (!speakers.contains(currentSpeaker)) {
                    getController().selectSpeaker(null);
                }
            } catch (UnknownHostException | SocketException e) {
                e.printStackTrace();
            }

            return speakers;
        }

        @Override
        protected void onPostExecute(ArrayList<String> speakers) {
            mHandlerThread.quit();
            mLoadingView.animate().withLayer().alpha(0).setDuration(250).withEndAction(new Runnable() {
                @Override
                public void run() {
                    mLoadingView.setVisibility(View.GONE);
                }
            }).start();

            mAdapter.updateData(speakers);
        }
    }
}
