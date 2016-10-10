package com.twinblade.poormanshomestereo;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.view.ViewCompat;
import android.text.method.HideReturnsTransformationMethod;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SpeakerService extends Service {

    private SpeakerServer mSpeakerServer;
    private UdpServer mUdpServer;
    private String mControllerIP;
    private CommandReceiver mCommandReceiver;
    private MediaPlayer mMediaPlayer;
    private OkHttpClient mHttpClient;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        mHttpClient = new OkHttpClient();
        Log.e("PMHS", "SpeakerService onCreate()");
        mCommandReceiver = new CommandReceiver();
        registerReceiver(mCommandReceiver, new IntentFilter(Constants.INTENT_STOP_SPEAKER_SERVICE));
        mMediaPlayer = new MediaPlayer();
        init();
    }

    private class CommandReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Constants.INTENT_STOP_SPEAKER_SERVICE)) {
                stopSelf();
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.e("PMHS", "SpeakerService onDestroy()");
        try {
            unregisterReceiver(mCommandReceiver);
        } catch (Exception e) {
        }
        //TODO: ENSURE NOT NULL
        mUdpServer.stop();
        mSpeakerServer.stop();

        if (mMediaPlayer != null) {
            mMediaPlayer.release();
        }
    }

    private void init() {
        try {
            mSpeakerServer = new SpeakerServer();
            Log.e("PMHS", "Created speakerServer");
            mSpeakerServer.start();
            Log.e("PMHS", "Started SpeakerServer in init");
            mUdpServer = new UdpServer();
            Log.e("PMHS", "Created udpServer");
            mUdpServer.start();
            Log.e("PMHS", "Started UDPServer in init");
        } catch (IOException e) {
            e.printStackTrace();
        }
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_queue)
                .setContentTitle("PMHS notification") // TODO: Song name?
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentText("Click to end");

        Intent intentStopService = new Intent(Constants.INTENT_STOP_SPEAKER_SERVICE);
        PendingIntent pStopSelf =
                PendingIntent.getBroadcast(
                        this,
                        0,
                        intentStopService,
                        0
                );
        mBuilder.setContentIntent(pStopSelf);
        mBuilder.addAction(R.mipmap.ic_search, "Stop", pStopSelf); // Not sure if needed
        startForeground(Constants.SPEAKER_NOTIFICATION_ID, mBuilder.build());
    }

    public class SpeakerServer extends NanoHTTPD {
        public SpeakerServer() {
            super(Constants.SERVER_PORT);
        }

        @Override
        public Response serve(IHTTPSession session) {
            // TODO: Handle the different requests this could get

            Log.e("PMHS", "Serving....");

            if(session.getMethod() == Method.POST && session.getUri().startsWith("/" + Constants.SPEAKER_COMMAND_URL)) {
                Map<String, List<String>> params;
                try {
                    session.parseBody(new HashMap<String, String>());
                    params = session.getParameters();
                    //Log.e("PMHS", "Parsed body");
                } catch (IOException ioe) {
                    //Log.e("PMHS", "IO Exception....");
                    return newFixedLengthResponse("IO Exception...");
                } catch (ResponseException e) {
                    //Log.e("PMHS", "Response Exception....");
                    return newFixedLengthResponse("Response exception...");
                }

                mControllerIP = session.getRemoteIpAddress();
                //Log.e("PMHS", "Controller ip: " + mControllerIP);
                if (!params.containsKey(Constants.SPEAKER_COMMAND)) {
                    Log.e("PMHS", "No speaker command provided");
                    return newFixedLengthResponse("No speaker command provided");
                }
                String command = params.get(Constants.SPEAKER_COMMAND).get(0);
                switch (command) {
                    case Constants.SPEAKER_COMMAND_PLAY:
                        Log.e("PMHS", "Play command received");

                        String url = "http://" + mControllerIP + ":" + Constants.SERVER_PORT + "/" + Constants.CONTROLLER_FILE_URL;

                        mMediaPlayer.reset();
                        try {
                            mMediaPlayer.setDataSource(url);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                            @Override
                            public void onPrepared(MediaPlayer mp) {
                                mMediaPlayer.start();
                            }
                        });

                        mMediaPlayer.prepareAsync();
                        mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                            @Override
                            public void onCompletion(MediaPlayer mp) {
                                Log.e("PMHS", "Song completed!");


                                FormBody.Builder builder = new FormBody.Builder();
                                builder.add(Constants.SPEAKER_STATUS, Constants.SPEAKER_STATUS_END_OF_SONG);
                                RequestBody body = builder.build();

                                Request request = new Request.Builder()
                                        .url("http://" + mControllerIP + ":" + Constants.SERVER_PORT + "/" + Constants.CONTROLLER_MSG_URL)
                                        .post(body)
                                        .build();

                                Call call = mHttpClient.newCall(request);
                                call.enqueue(new Callback() {
                                    @Override
                                    public void onFailure(Call call, IOException e) {
                                        e.printStackTrace();
                                    }

                                    @Override
                                    public void onResponse(Call call, okhttp3.Response response) throws IOException {

                                    }
                                });
                            }
                        });
                        break;
                    case Constants.SPEAKER_COMMAND_PAUSE:
                        Log.e("PMHS", "Pause command received");
                        break;
                    case Constants.SPEAKER_COMMAND_RESUME:
                        Log.e("PMHS", "Resume command received");
                        break;
                    case Constants.SPEAKER_COMMAND_SEEK:
                        Log.e("PMHS", "Seek command received");
                        break;
                    default:
                        Log.e("PMHS", "Invalid speaker command");
                        return newFixedLengthResponse("Invalid speaker command");
                }
            } else if (session.getMethod() == Method.GET && session.getUri().startsWith("/" + Constants.SPEAKER_STATUS_URL)) {
                // TODO: seek position
                Log.e("PMHS", "GET command");
                return newFixedLengthResponse("GET command");
            } else {
                Log.e("PMHS", "Unrecognized command");
                return newFixedLengthResponse("Unrecognized command");
            }
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

        private DatagramSocket mUdpSocket;
        private AsyncTask<Void, Void, Void> async;

        public void start() {
            Log.e("PMHS", "UdpServer started...");
            async = new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    Log.e("PMHS", "UDP RUNNING");

                    byte[] lMsg = new byte[1024];
                    DatagramPacket dp = new DatagramPacket(lMsg, lMsg.length);
                    mUdpSocket = null;
                    try {
                        mUdpSocket = new DatagramSocket(Constants.BROADCAST_PORT);
                        mUdpSocket.setBroadcast(true);

                        while (!mUdpSocket.isClosed()) {
                            Log.e("PMHS", "RECEIVING");
                            mUdpSocket.receive(dp);
                            String receivedStr = new String(lMsg);
                            Log.e("PMHS", "RECEIVED: " + receivedStr);
                            if (receivedStr.trim().equals(Constants.BROADCAST_KEY)) {
                                Log.e("PMHS", "Got the broadcast!");
                                mControllerIP = dp.getAddress().getHostAddress(); //TODO: Move to when I get a play command or something
                                String myIP = getWifiIpAddress();
                                byte[] response = (Constants.BROADCAST_RESPONSE_PREFIX + myIP).getBytes();
                                DatagramPacket responsePacket = new DatagramPacket(response, response.length,
                                        dp.getAddress(), Constants.BROADCAST_PORT);
                                Log.e("PMHS", "Responding with " + myIP);
                                mUdpSocket.send(responsePacket);
                            }
                        }
                    } catch (Exception e) {
                        Log.e("PMHS", "ERR ", e);
                    }

                    return null;
                }
            };
            async.execute();
        }

        public void stop() {
            Log.e("PMHS", "Udp server stopped....");
            if (mUdpSocket != null) {
                Log.e("PMHS", "About to close socket");
                mUdpSocket.close();
            }
        }
    }
}