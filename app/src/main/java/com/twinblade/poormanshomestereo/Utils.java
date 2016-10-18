package com.twinblade.poormanshomestereo;

import android.content.ContentResolver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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

    public static Bitmap getAlbumCover(ContentResolver cr, String albumId) {
        Cursor cursor = cr.query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                new String[] {MediaStore.Audio.Albums.ALBUM_ART},
                MediaStore.Audio.Albums._ID + " = ?",
                new String[] {albumId},
                null);

        if (cursor != null && cursor.moveToFirst()) {
            String path = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART));
            cursor.close();

            return BitmapFactory.decodeFile(path);
        }

        return null;
    }
}
