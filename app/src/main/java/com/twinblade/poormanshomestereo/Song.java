package com.twinblade.poormanshomestereo;

import android.graphics.Bitmap;

import java.io.File;
import java.io.Serializable;

public class Song implements Serializable {

    private String mId;
    private String mTitle;
    private String mArtist;
    private String mAlbum;
    private String mAlbumId;
    private File mFile;

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

    public void setAlbumCover(Bitmap albumCover) {
        mAlbumCover = albumCover;
    }

    public Bitmap getAlbumCover() {
        return mAlbumCover;
    }
}
