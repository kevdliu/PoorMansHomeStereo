package com.twinblade.poormanshomestereo.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.InputType;
import android.util.Patterns;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

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

        Button manual = (Button) root.findViewById(R.id.manual_add);
        manual.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showManualDialog();
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
                mAdapter.notifyDataSetChanged();
            }
        });

        new SpeakerDiscovery().execute();

        return root;
    }

    private ControllerActivity getController() {
        return ((ControllerActivity) getActivity());
    }

    private void showManualDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getController());
        builder.setTitle("Speaker IP Address");

        final EditText input = new EditText(getController());
        input.setInputType(InputType.TYPE_CLASS_TEXT); //TODO: ALLOW ONLY NUMBERS AND PERIODS
        builder.setView(input);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String entry = input.getText().toString();
                if (!entry.equals("") && Patterns.IP_ADDRESS.matcher(entry).matches()) {
                    if (mAdapter != null) {
                        mAdapter.addSpeaker(entry);
                    }
                } else {
                    Toast.makeText(getController(), "Invalid input", Toast.LENGTH_SHORT).show();
                }
            }
        });

        final AlertDialog dialog = builder.create();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        dialog.show();

        input.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_NULL || actionId == EditorInfo.IME_ACTION_DONE
                        || actionId == EditorInfo.IME_ACTION_NEXT) {
                    String entry = input.getText().toString();
                    if (!entry.equals("") && Patterns.IP_ADDRESS.matcher(entry).matches()) {
                        if (mAdapter != null) {
                            mAdapter.addSpeaker(entry);
                        }
                    } else {
                        Toast.makeText(getController(), "Invalid input", Toast.LENGTH_SHORT).show();
                    }
                    return true;
                }

                return false;
            }
        });

        input.requestFocus();
    }

    private class SpeakerDiscovery extends AsyncTask<Void, Void, ArrayList<String>> {

        @Override
        protected void onPreExecute() {
            mLoadingView.setVisibility(View.VISIBLE);
            mLoadingView.animate().withLayer().alpha(1).setDuration(250).start();
        }

        @Override
        protected ArrayList<String> doInBackground(Void... params) {
            ArrayList<String> speakers = new ArrayList<>();
            try {
                speakers = Utils.findSpeakers(getController());
            } catch (UnknownHostException | SocketException e) {
                e.printStackTrace();
            }

            String currentSpeaker = getController().getSelectedSpeaker();
            if (!speakers.contains(currentSpeaker)) {
                getController().selectSpeaker(null);
            }

            return speakers;
        }

        @Override
        protected void onPostExecute(ArrayList<String> speakers) {
            mLoadingView.animate().withLayer().alpha(0).setDuration(250).withEndAction(new Runnable() {
                @Override
                public void run() {
                    mLoadingView.setVisibility(View.GONE);
                }
            }).start();

            mAdapter.updateData(speakers);
        }
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);

        if (!hidden && mSpeakerList != null) {
            mSpeakerList.invalidateViews();
        }
    }
}
