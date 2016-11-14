package com.twinblade.poormanshomestereo.adapters;

import android.graphics.Color;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.twinblade.poormanshomestereo.ControllerActivity;
import com.twinblade.poormanshomestereo.R;

import java.util.ArrayList;
import java.util.LinkedHashMap;

public class SpeakersAdapter extends BaseAdapter {

    private final ControllerActivity mActivity;
    private final LayoutInflater mInflater;

    private ArrayList<String> mSpeakerAddresses = new ArrayList<>();
    private LinkedHashMap<String, String> mSpeakersMap = new LinkedHashMap<>();

    public SpeakersAdapter(ControllerActivity activity) {
        mInflater = LayoutInflater.from(activity);
        mActivity = activity;
    }

    public void updateData(LinkedHashMap<String, String> speakers) {
        mSpeakersMap = speakers;
        mSpeakerAddresses = new ArrayList<>(speakers.keySet());
        notifyDataSetChanged();
    }

    public void addSpeaker(String ip, String name) {
        mSpeakersMap.put(ip, name);
        mSpeakerAddresses.add(ip);
        notifyDataSetChanged();
    }

    public LinkedHashMap<String, String> getDiscoveredSpeakers() {
        return mSpeakersMap;
    }

    public String getSpeakerName(String ip) {
        return mSpeakersMap.get(ip);
    }

    @Override
    public int getCount() {
        return mSpeakersMap.size();
    }

    @Override
    public Object getItem(int i) {
        return mSpeakerAddresses.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        if (view == null) {
            view = mInflater.inflate(R.layout.row_speakers, viewGroup, false);
        }

        String address = mSpeakerAddresses.get(i);

        TextView nameView = (TextView) view.findViewById(R.id.name);
        nameView.setText(mSpeakersMap.get(address));

        TextView addressView = (TextView) view.findViewById(R.id.address);
        addressView.setText(address);

        if (TextUtils.equals(address, mActivity.getSelectedSpeaker())) {
            nameView.setTextColor(Color.parseColor("#2196F3"));
            addressView.setTextColor(Color.parseColor("#2196F3"));
        } else {
            nameView.setTextColor(Color.BLACK);
            addressView.setTextColor(Color.BLACK);
        }

        return view;
    }
}
