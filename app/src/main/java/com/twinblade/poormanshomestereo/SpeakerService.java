package com.twinblade.poormanshomestereo;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.view.ViewCompat;
import android.text.method.HideReturnsTransformationMethod;
import android.util.Log;

import java.io.IOException;
import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

public class SpeakerService extends Service {

    private SpeakerServer mSpeakerServer;
    private UdpServer mUdpServer;
    private String mControllerIP;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //TODO: MOVE TO ONCREATE()
        Log.d("*********", "In on start command");
        init(intent);
        Log.d("*********", "Finished init");
        return START_STICKY;
    }

    private void init(Intent intent) {

        if ("STOP_SERVICE".equals(intent.getAction())) {
            //TODO: MOVE TO ONDESTROY()
            stopForeground(true);
            stopSelf();
            mUdpServer.stop();
            mSpeakerServer.stop();
        } else {
            try {
                mSpeakerServer = new SpeakerService.SpeakerServer();
                mUdpServer = new SpeakerService.UdpServer();
                mUdpServer.start();
                mSpeakerServer.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
                    .setSmallIcon(R.mipmap.ic_queue)
                    .setContentTitle("PMHS notification") // TODO: Song name?
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentText("Click to end");

            Intent intentStopService = new Intent(this, SpeakerService.class);
            intentStopService.setAction("STOP_SERVICE");

            PendingIntent pStopSelf =
                    PendingIntent.getService(
                            this,
                            0,
                            intentStopService,
                            PendingIntent.FLAG_CANCEL_CURRENT
                    );
            mBuilder.setContentIntent(pStopSelf);
            mBuilder.addAction(R.mipmap.ic_search, "Stop", pStopSelf); // Not sure if needed
            startForeground(Constants.SPEAKER_NOTIFICATION_ID, mBuilder.build());
        }
    }

    @Override
    public void onCreate() {
    }

    public class SpeakerServer extends NanoHTTPD {
        public SpeakerServer() {
            super(Constants.SERVER_PORT);
        }

        @Override
        public Response serve(IHTTPSession session) {
            // TODO: Handle the different requests this could get
            Map<String, String> files = new HashMap<String, String>();

            try {
                session.parseBody(files);
            } catch (IOException ioe) {
                return newFixedLengthResponse("IO Exception...");
            } catch (ResponseException e) {
                return newFixedLengthResponse("Response exception...");
            }
            String postBody = session.getQueryParameterString();
            String command = files.get(Constants.SPEAKER_COMMAND);

            Log.d("*******IP*****", getWifiIpAddress());
            return newFixedLengthResponse(/*command*/ getWifiIpAddress());
        }

    }


    protected String getWifiIpAddress() {
        WifiManager wifiManager = (WifiManager) this.getSystemService(WIFI_SERVICE);
        int ipAddress = wifiManager.getConnectionInfo().getIpAddress();

        // Convert little-endian to big-endianif needed
        if (ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)) {
            ipAddress = Integer.reverseBytes(ipAddress);
        }

        byte[] ipByteArray = BigInteger.valueOf(ipAddress).toByteArray();

        String ipAddressString;
        try {
            ipAddressString = InetAddress.getByAddress(ipByteArray).getHostAddress();
        } catch (UnknownHostException ex) {
            Log.e("WIFIIP", "Unable to get host address.");
            ipAddressString = null;
        }

        return ipAddressString;
    }

    public class UdpServer {
        private AsyncTask<Void, Void, Void> async;
        private boolean udpServerRunning;

        public void start() {
            async = new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    byte[] lMsg = new byte[1024];
                    DatagramPacket dp = new DatagramPacket(lMsg, lMsg.length);
                    DatagramSocket ds = null;
                    try {
                        ds = new DatagramSocket(Constants.SERVER_PORT);

                        while (udpServerRunning) {
                            ds.receive(dp);
                            String receivedStr = new String(lMsg);

                            if (receivedStr.trim().equals(Constants.BROADCAST_KEY)) {
                                Log.e("********", "Got the broadcast!");
                                mControllerIP = dp.getAddress().getHostAddress(); //TODO: Move to when I get a play command or something
                                String myIP = getWifiIpAddress();
                                byte[] response = (Constants.BROADCAST_RESPONSE_PREFIX + myIP).getBytes();
                                DatagramPacket responsePacket = new DatagramPacket(response, response.length,
                                        dp.getAddress(), Constants.BROADCAST_PORT);
                                Log.e("*******", "Responding with" + myIP);
                                ds.send(responsePacket);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        if (ds != null) {
                            ds.close();
                        }
                    }

                    return null;
                }
            };
            async.execute();
        }

        public void stop() {
            udpServerRunning = false;
        }
    }
}