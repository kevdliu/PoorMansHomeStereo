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

public class SpeakersAdapter extends BaseAdapter {

    private ControllerActivity mActivity;
    private LayoutInflater mInflater;
    private ArrayList<String> mDiscoveredSpeakers;

    public SpeakersAdapter(ControllerActivity activity, ArrayList<String> speakers) {
        mInflater = LayoutInflater.from(activity);
        mActivity = activity;
        mDiscoveredSpeakers = speakers;
    }

    public void updateData(ArrayList<String> speakers) {
        mDiscoveredSpeakers = speakers;
        notifyDataSetChanged();
    }

    public void addSpeaker(String speaker) {
        mDiscoveredSpeakers.add(speaker);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return mDiscoveredSpeakers.size();
    }

    @Override
    public Object getItem(int i) {
        return mDiscoveredSpeakers.get(i);
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

        String address = mDiscoveredSpeakers.get(i);
        TextView addressView = (TextView) view.findViewById(R.id.address);
        addressView.setText(address);

        if (TextUtils.equals(address, mActivity.getSelectedSpeaker())) {
            addressView.setTextColor(Color.parseColor("#2196F3"));
        } else {
            addressView.setTextColor(Color.BLACK);
        }

        return view;
    }
}
