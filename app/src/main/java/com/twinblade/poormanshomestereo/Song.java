package com.twinblade.poormanshomestereo;

import android.graphics.Bitmap;

import java.io.File;
import java.io.Serializable;

public class Song implements Serializable {

    private final String mId;
    private final String mTitle;
    private final String mArtist;
    private final String mAlbum;
    private final String mAlbumId;
    private final File mFile;

    private Bitmap mAlbumCover;

    public Song(String id, String title, String artist, String album, String albumId, File file) {
        mId = id;
        mTitle = title;
        mArtist = artist;
        mAlbum = album;
        mAlbumId = albumId;
        mFile = file;
    }

    public String getId() {
        return mId;
    }

    public String getTitle() {
        return mTitle;
    }

    public String getArtist() {
        return mArtist;
    }

    public String getAlbum() {
        return mAlbum;
    }

    public String getAlbumId() {
        return mAlbumId;
    }

    public File getFile() {
        return mFile;
    }

    void setAlbumCover(Bitmap albumCover) {
        mAlbumCover = albumCover;
    }

    Bitmap getAlbumCover() {
        return mAlbumCover;
    }
}
