package com.twinblade.poormanshomestereo;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.text.format.Formatter;
import android.util.Log;

import java.io.File;
import java.io.InterruptedIOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

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

    public static Song getSongFromUrl(String url, Context context) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(url, new HashMap<String, String>());

        String title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
        if (title == null) {
            title = "<Title not found>";
        }
        String artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
        String album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);
        byte[] albumCoverArr = retriever.getEmbeddedPicture();
        retriever.release();

        Song song = new Song(null, title, artist, album, null, null);
        if (albumCoverArr != null && albumCoverArr.length > 0) {
            Bitmap albumCover = BitmapFactory.decodeByteArray(albumCoverArr, 0, albumCoverArr.length);
            song.setAlbumCover(albumCover);
        } else {
            song.setAlbumCover(BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_songs));
        }

        return song;
    }

    public static Bitmap getAlbumCover(ContentResolver cr, String albumId) {
        Cursor cursor = cr.query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                new String[] {MediaStore.Audio.Albums.ALBUM_ART},
                MediaStore.Audio.Albums._ID + " = ?",
                new String[] {albumId},
                null);
        Log.e("PMHS_album", "Album id: " + albumId);
        Log.e("PMHS_album", "Cursor: " + cursor);

        if (cursor != null && cursor.moveToFirst()) {
            String path = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART));
            cursor.close();

            Log.e("PMHS_album", "Returning decoded file at: " + path);
            return BitmapFactory.decodeFile(path);
        }

        Log.e("PMHS_album", "Returning null");
        return null;
    }

    @SuppressWarnings("deprecation")
    public static String getWifiIpAddress(Context context) {
        WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        return Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
    }

    public static long getSystemTime() {
        return Calendar.getInstance().getTimeInMillis();
    }

    public static Long getNetworkTimeOffset() {
        long systemTime = System.currentTimeMillis();
        final long[] networkTime = new long[1];
        Thread ntpFetcher = new Thread() {
            @Override
            public void run() {
                SntpClient client = new SntpClient();
                if (client.requestTime("pool.ntp.org", 10000)) {
                    // Not needed:
                    Date date = new Date(client.getNtpTime());
                    SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss:SSS");
                    String dateFormatted = formatter.format(date);
                    //

                    Log.e("PMHSTime", "Time found?: " + dateFormatted);
                    networkTime[0] = client.getNtpTime();
                }
            }
        };
        ntpFetcher.start();
        try {
            ntpFetcher.join();
            return new Long(networkTime[0] - systemTime);
        } catch (InterruptedException e) {
            return null;
        }
    }

}



