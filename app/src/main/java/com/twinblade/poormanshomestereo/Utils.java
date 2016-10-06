package com.twinblade.poormanshomestereo;

import android.database.Cursor;
import android.provider.MediaStore;

import java.io.File;

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
}
