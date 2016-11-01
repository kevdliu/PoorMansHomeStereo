package com.twinblade.poormanshomestereo.fragments;

import android.Manifest;
import android.app.AlertDialog;
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

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class SpeakersFragment extends Fragment implements Button.OnClickListener {

    private SpeakersAdapter mAdapter;
    private ListView mSpeakerList;
    private LinearLayout mLoadingView;

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
        input.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setKeyListener(DigitsKeyListener.getInstance("0123456789."));
        builder.setView(input);

        builder.setNegativeButton("Cancel", null);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String entry = input.getText().toString();
                if (Patterns.IP_ADDRESS.matcher(entry).matches()) {
                    autoSelectSpeaker(entry);
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
                    autoSelectSpeaker(ip);
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

    private void autoSelectSpeaker(String speaker) {
        String selectedSpeaker = getController().getSelectedSpeaker();
        ArrayList<String> discoveredSpeakers = mAdapter.getDiscoveredSpeakers();

        if (!discoveredSpeakers.contains(speaker)) {
            mAdapter.addSpeaker(speaker);
        }

        if (selectedSpeaker == null || selectedSpeaker.equals("")) {
            getController().selectSpeaker(speaker);
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
                speakers = findSpeakers();
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
