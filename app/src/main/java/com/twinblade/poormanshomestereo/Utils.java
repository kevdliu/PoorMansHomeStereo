package com.twinblade.poormanshomestereo;

import android.content.Context;
import android.database.Cursor;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.format.Formatter;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class Utils {

    public static Song getSongFromCursor(Cursor cursor) {
        String id = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media._ID));
        String title = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
        String artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
        String album = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM));
        String albumId = Long.toString(cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)));
        String fileLocation = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));

        return new Song(id, title, artist, album, albumId, new File(fileLocation));
    }

    public static ArrayList<String> findSpeakers(Context context, Handler handler)
            throws SocketException, UnknownHostException {
        ArrayList<String> speakers = new ArrayList<>();

        final DatagramSocket clientSocket = new DatagramSocket(Constants.BROADCAST_PORT);
        clientSocket.setBroadcast(true);
        InetAddress broadcastAddress = InetAddress.getByName(getIpSubnetPrefix(context) + "255");

        byte[] sendData = Constants.BROADCAST_KEY.getBytes();
        byte[] receiveData = new byte[1024];

        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, broadcastAddress, Constants.BROADCAST_PORT);
        try {
            clientSocket.send(sendPacket);
        } catch (IOException e) {
            e.printStackTrace();
            return speakers;
        }

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                clientSocket.close();
            }
        }, Constants.BROADCAST_RESPONSE_TIMEOUT);

        while (!clientSocket.isClosed()) {
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            try {
                clientSocket.receive(receivePacket);
            } catch (IOException e) {
                e.printStackTrace();
                clientSocket.close();
                return speakers;
            }

            String response = new String(receivePacket.getData()).trim();

            Log.e("PMHS", "RECEIVED: " + response);

            if (response.startsWith(Constants.BROADCAST_RESPONSE_PREFIX)) {
                String ip = response.substring(Constants.BROADCAST_RESPONSE_PREFIX.length());
                speakers.add(ip);
            }
        }

        clientSocket.close();
        return speakers;
    }

    @SuppressWarnings("deprecation")
    private static String getIpSubnetPrefix(Context context) {
        WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        String ipAddress = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());

        return ipAddress.substring(0, ipAddress.lastIndexOf(".") + 1);
    }
}
