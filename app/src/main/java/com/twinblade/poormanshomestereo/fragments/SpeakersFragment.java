package com.twinblade.poormanshomestereo.fragments;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.text.InputType;
import android.text.method.DigitsKeyListener;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.google.zxing.ResultPoint;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;
import com.twinblade.poormanshomestereo.Constants;
import com.twinblade.poormanshomestereo.ControllerActivity;
import com.twinblade.poormanshomestereo.R;
import com.twinblade.poormanshomestereo.Utils;
import com.twinblade.poormanshomestereo.adapters.SpeakersAdapter;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SpeakersFragment extends Fragment implements Button.OnClickListener {

    private boolean mRunInitialSpeakerDiscovery = true;

    private SpeakersAdapter mAdapter;
    private LinearLayout mLoadingView;
    private OkHttpClient mHttpClient;

    @Override
    public void onCreate(Bundle saved) {
        super.onCreate(saved);

        mHttpClient = new OkHttpClient.Builder()
                .connectTimeout(Constants.SPEAKER_CONNECTION_TIMEOUT, TimeUnit.SECONDS)
                .build();
        mAdapter = new SpeakersAdapter(getController());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_speakers, container, false);

        Button auto = (Button) root.findViewById(R.id.auto_add);
        auto.setOnClickListener(this);

        Button manual = (Button) root.findViewById(R.id.manual_add);
        manual.setOnClickListener(this);

        Button qr = (Button) root.findViewById(R.id.qr_capture);
        qr.setOnClickListener(this);

        mLoadingView = (LinearLayout) root.findViewById(R.id.loading);
        final ListView speakerList = (ListView) root.findViewById(R.id.speakers_list);
        speakerList.setAdapter(mAdapter);

        speakerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                String ip = (String) mAdapter.getItem(position);
                getController().selectSpeaker(ip, mAdapter.getSpeakerName(ip));
                mAdapter.notifyDataSetChanged();
            }
        });

        return root;
    }

    @Override
    public void onViewCreated(View view, Bundle saved) {
        super.onViewCreated(view, saved);

        if (mRunInitialSpeakerDiscovery) {
            new SpeakerDiscovery().execute();
        }
    }

    private ControllerActivity getController() {
        return ((ControllerActivity) getActivity());
    }

    private void showManualDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getController());
        builder.setTitle("Speaker IP Address");

        final EditText input = new EditText(getController());
        input.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setKeyListener(DigitsKeyListener.getInstance("0123456789."));
        builder.setView(input);

        builder.setNegativeButton("Cancel", null);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String entry = input.getText().toString();
                if (Patterns.IP_ADDRESS.matcher(entry).matches()) {
                    new SpeakerInfoLoader().execute(entry);
                } else {
                    Toast.makeText(getController(), "Invalid input", Toast.LENGTH_SHORT).show();
                }
            }
        });

        final AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null){
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }
        dialog.show();

        input.requestFocus();
    }

    private void showQrCaptureDialog() {
        if (Build.VERSION.SDK_INT >= 23 &&
                ContextCompat.checkSelfPermission(getController(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] {Manifest.permission.CAMERA},
                    Constants.CAMERA_PERMISSION_REQUEST_ID);
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getController());

        LayoutInflater inflater = LayoutInflater.from(getController());
        View root = inflater.inflate(R.layout.dialog_qr_capture, null);
        final DecoratedBarcodeView barcodeView = (DecoratedBarcodeView) root.findViewById(R.id.qr_view);

        builder.setView(root);
        builder.setNegativeButton("Cancel", null);
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                barcodeView.pause();
            }
        });
        final AlertDialog dialog = builder.create();
        dialog.show();

        barcodeView.resume();
        barcodeView.decodeSingle(new BarcodeCallback() {
            @Override
            public void barcodeResult(BarcodeResult result) {
                String ip = result.getText();
                if (Patterns.IP_ADDRESS.matcher(ip).matches()) {
                    new SpeakerInfoLoader().execute(ip);
                } else {
                    Toast.makeText(getController(), "Invalid IP", Toast.LENGTH_SHORT).show();
                }

                barcodeView.pause();
                dialog.dismiss();
            }

            @Override
            public void possibleResultPoints(List<ResultPoint> resultPoints) {
                //
            }
        });
    }

    private void autoSelectSpeaker(String ip, String name) {
        String selectedSpeaker = getController().getSelectedSpeaker();
        LinkedHashMap<String, String> discoveredSpeakers = mAdapter.getDiscoveredSpeakers();

        if (!discoveredSpeakers.containsKey(ip)) {
            mAdapter.addSpeaker(ip, name);
        }

        if (selectedSpeaker == null || selectedSpeaker.equals("")) {
            getController().selectSpeaker(ip, name);
            mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        if (requestCode == Constants.CAMERA_PERMISSION_REQUEST_ID) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showQrCaptureDialog();
            } else {
                Toast.makeText(getController(), "Camera permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.auto_add:
                new SpeakerDiscovery().execute();
                break;

            case R.id.manual_add:
                showManualDialog();
                break;

            case R.id.qr_capture:
                showQrCaptureDialog();
                break;
        }
    }

    private ArrayList<String> findSpeakers()
            throws SocketException, UnknownHostException {
        ArrayList<String> speakers = new ArrayList<>();

        final DatagramSocket clientSocket = new DatagramSocket(Constants.BROADCAST_PORT);
        clientSocket.setBroadcast(true);
        clientSocket.setSoTimeout(Constants.BROADCAST_RESPONSE_TIMEOUT);
        InetAddress broadcastAddress = InetAddress.getByName(getIpSubnetPrefix() + "255");

        byte[] sendData = Constants.BROADCAST_KEY.getBytes();
        byte[] receiveData = new byte[1024];

        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, broadcastAddress, Constants.BROADCAST_PORT);
        try {
            clientSocket.send(sendPacket);
        } catch (IOException e) {
            e.printStackTrace();
            return speakers;
        }

        while (!clientSocket.isClosed()) {
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            try {
                clientSocket.receive(receivePacket);
            } catch (IOException e) {
                // e.printStackTrace();
                clientSocket.close();
                return speakers;
            }

            String response = new String(receivePacket.getData()).trim();
            if (response.startsWith(Constants.BROADCAST_RESPONSE_PREFIX)) {
                String ip = response.substring(Constants.BROADCAST_RESPONSE_PREFIX.length());

                if (!speakers.contains(ip)) {
                    speakers.add(ip);
                }
            }
        }

        clientSocket.close();
        return speakers;
    }

    private String getIpSubnetPrefix() {
        String ipAddress = Utils.getWifiIpAddress(getController());
        return ipAddress.substring(0, ipAddress.lastIndexOf(".") + 1);
    }

    private class SpeakerInfoLoader extends AsyncTask<String, Void, String> {

        private ProgressDialog mDialog;
        private String mAddress;

        @Override
        protected void onPreExecute() {
            mDialog = new ProgressDialog(getController());
            mDialog.setMessage("Getting Speaker Information...");
            mDialog.setIndeterminate(true);
            mDialog.show();
        }

        @Override
        protected String doInBackground(String... params) {
            mAddress = params[0];
            return getSpeakerName(mAddress);
        }

        @Override
        protected void onPostExecute(String name) {
            mDialog.dismiss();

            if (name != null) {
                autoSelectSpeaker(mAddress, name);
            } else {
                Toast.makeText(getController(), "Error connecting to speaker", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private class SpeakerDiscovery extends AsyncTask<Void, Void, LinkedHashMap<String, String>> {

        @Override
        protected void onPreExecute() {
            mLoadingView.setVisibility(View.VISIBLE);
            mLoadingView.animate().withLayer().alpha(1).setDuration(250).start();
        }

        @Override
        protected LinkedHashMap<String, String> doInBackground(Void... params) {
            LinkedHashMap<String, String> speakersMap = new LinkedHashMap<>();
            try {
                ArrayList<String> speakerAddresses = findSpeakers();
                for (String ip : speakerAddresses) {
                    String speakerName = getSpeakerName(ip);
                    if (speakerName == null) {
                        speakerName = "<Unknown Speaker Name>";
                    }

                    speakersMap.put(ip, speakerName);
                }
            } catch (UnknownHostException | SocketException e) {
                e.printStackTrace();
            }

            // TODO: DETERMINE IF NEEDED
            /**
            String currentSpeaker = getController().getSelectedSpeaker();
            if (!speakers.contains(currentSpeaker)) {
                getController().selectSpeaker(null);
            }
             */

            return speakersMap;
        }

        @Override
        protected void onPostExecute(LinkedHashMap<String, String> speakersMap) {
            mRunInitialSpeakerDiscovery = false;

            mAdapter.updateData(speakersMap);

            mLoadingView.animate().withLayer().alpha(0).setDuration(250).withEndAction(new Runnable() {
                @Override
                public void run() {
                    mLoadingView.setVisibility(View.GONE);
                }
            }).start();
        }
    }

    private String getSpeakerName(String ip) {
        Request request = new Request.Builder()
                .url("http://" + ip + ":" + Constants.SERVER_PORT + "/" + Constants.SPEAKER_STATE_URL)
                .build();

        try {
            Response response = mHttpClient.newCall(request).execute();
            try {
                JSONObject json = new JSONObject(response.body().string());

                if (json.has(Constants.SPEAKER_PROPERTY_NAME)) {
                    return json.getString(Constants.SPEAKER_PROPERTY_NAME);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }
}
