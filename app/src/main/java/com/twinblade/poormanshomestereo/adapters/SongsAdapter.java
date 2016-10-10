package com.twinblade.poormanshomestereo.adapters;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.v4.widget.CursorAdapter;
import android.text.TextUtils;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.twinblade.poormanshomestereo.Constants;
import com.twinblade.poormanshomestereo.ControllerActivity;
import com.twinblade.poormanshomestereo.R;
import com.twinblade.poormanshomestereo.Song;
import com.twinblade.poormanshomestereo.Utils;

import java.lang.ref.WeakReference;

public class SongsAdapter extends CursorAdapter {

    private ControllerActivity mActivity;
    private ContentResolver mContentResolver;

    private Bitmap mPlaceHolderBitmap;
    private LruCache<String, Bitmap> mMemoryCache;

    public SongsAdapter(ControllerActivity activity, Cursor songCursor) {
        super(activity, songCursor, 0);

        mActivity = activity;
        mContentResolver = activity.getContentResolver();

        mPlaceHolderBitmap = BitmapFactory.decodeResource(mActivity.getResources(), R.mipmap.ic_songs);
        mMemoryCache = new LruCache<>(Constants.LIST_BITMAP_CACHE_SIZE);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.row_songs, parent, false);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        TextView titleView = (TextView) view.findViewById(R.id.title);
        TextView artistAlbum = (TextView) view.findViewById(R.id.artist_album);
        ImageView albumCover = (ImageView) view.findViewById(R.id.album_cover);

        Song song = Utils.getSongFromCursor(cursor);
        loadBitmap(song.getAlbumId(), albumCover);
        titleView.setText(song.getTitle());

        String artistAlbumInfo = song.getArtist() + " ▼ " + song.getAlbum();
        artistAlbum.setText(artistAlbumInfo);

        Song currentSong = mActivity.getCurrentSong();
        if (currentSong != null && TextUtils.equals(song.getId(), currentSong.getId())) {
            titleView.setTextColor(Color.parseColor("#2196F3"));
            artistAlbum.setTextColor(Color.parseColor("#2196F3"));
        } else {
            titleView.setTextColor(Color.BLACK);
            artistAlbum.setTextColor(Color.BLACK);
        }
    }

    public void loadBitmap(String albumId, ImageView imageView) {
        if (cancelPotentialWork(albumId, imageView)) {
            final AlbumCoverLoader task = new AlbumCoverLoader(imageView);
            final AsyncDrawable asyncDrawable =
                    new AsyncDrawable(mActivity.getResources(), mPlaceHolderBitmap, task);
            imageView.setImageDrawable(asyncDrawable);
            task.execute(albumId);
        }
    }

    private boolean cancelPotentialWork(String albumId, ImageView imageView) {
        final AlbumCoverLoader task = getTask(imageView);

        if (task != null) {
            final String bitmapData = task.mAlbumId;

            if (bitmapData == null || !TextUtils.equals(bitmapData, albumId)) {
                task.cancel(true);
            } else {
                return false;
            }
        }

        return true;
    }

    private AlbumCoverLoader getTask(ImageView imageView) {
        if (imageView != null) {
            final Drawable drawable = imageView.getDrawable();
            if (drawable instanceof AsyncDrawable) {
                final AsyncDrawable asyncDrawable = (AsyncDrawable) drawable;
                return asyncDrawable.getTask();
            }
        }

        return null;
    }

    private class AlbumCoverLoader extends AsyncTask<String, Void, Bitmap> {

        private final WeakReference<ImageView> mImageViewReference;
        private String mAlbumId = null;

        public AlbumCoverLoader(ImageView imageView) {
            mImageViewReference = new WeakReference<>(imageView);
        }

        @Override
        protected Bitmap doInBackground(String... params) {
            mAlbumId = params[0];

            Bitmap bm = getBitmapFromMemoryCache(mAlbumId);
            if (bm != null) {
                return bm;
            }

            Cursor cursor = mContentResolver.query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                    new String[] {MediaStore.Audio.Albums.ALBUM_ART},
                    MediaStore.Audio.Albums._ID + " = ?",
                    new String[] {mAlbumId},
                    null);

            if (cursor != null && cursor.moveToFirst()) {
                String path = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART));
                cursor.close();

                return BitmapFactory.decodeFile(path);
            }

            return null;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (isCancelled()) {
                bitmap = null;
            }

            if (bitmap != null) {
                final ImageView imageView = mImageViewReference.get();
                final AlbumCoverLoader task = getTask(imageView);
                if (this == task) {
                    imageView.setImageBitmap(bitmap);
                    addBitmapToMemoryCache(mAlbumId, bitmap);
                }
            }
        }
    }

    private void addBitmapToMemoryCache(String key, Bitmap bitmap) {
        if (getBitmapFromMemoryCache(key) == null) {
            mMemoryCache.put(key, bitmap);
        }
    }

    private Bitmap getBitmapFromMemoryCache(String key) {
        return mMemoryCache.get(key);
    }

    private static class AsyncDrawable extends BitmapDrawable {

        private final WeakReference<AlbumCoverLoader> mTaskReference;

        public AsyncDrawable(Resources res, Bitmap bitmap, AlbumCoverLoader task) {
            super(res, bitmap);
            mTaskReference = new WeakReference<>(task);
        }

        public AlbumCoverLoader getTask() {
            return mTaskReference.get();
        }
    }
}
