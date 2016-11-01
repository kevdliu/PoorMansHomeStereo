package com.twinblade.poormanshomestereo.fragments;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.twinblade.poormanshomestereo.Constants;
import com.twinblade.poormanshomestereo.ControllerActivity;
import com.twinblade.poormanshomestereo.R;
import com.twinblade.poormanshomestereo.adapters.SongsAdapter;

public class SearchFragment extends BaseFragment {

    private EditText mSearchBox;
    private InputMethodManager mInputMethodManager;

    @Override
    public void onCreate(Bundle saved) {
        super.onCreate(saved);

        mInputMethodManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        mAdapter = new SongsAdapter((ControllerActivity) getActivity(), null);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_songs, container, false);

        mSearchBox = (EditText) root.findViewById(R.id.search_box);
        mSongList = (ListView) root.findViewById(R.id.songs_list);
        mSongList.setAdapter(mAdapter);
        registerForInteraction();

        mSearchBox.setVisibility(View.VISIBLE);
        mSearchBox.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_NULL || actionId == EditorInfo.IME_ACTION_DONE
                        || actionId == EditorInfo.IME_ACTION_NEXT) {
                    String term = textView.getText().toString().trim();

                    new SongSearchTask().execute(term);
                    return true;
                }

                return false;
            }
        });

        mSearchBox.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (!hasFocus) {
                    hideKeyboard();
                }
            }
        });

        mSongList.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    if (mSearchBox.hasFocus()) {
                        view.requestFocus();
                    }
                }

                return false;
            }
        });

        return root;
    }

    private void hideKeyboard() {
        mInputMethodManager.hideSoftInputFromWindow(mSearchBox.getApplicationWindowToken(), 0);
    }

    private class SongSearchTask extends AsyncTask<String, Integer, Cursor> {

        @Override
        protected void onPreExecute() {
            //
        }

        @Override
        protected Cursor doInBackground(String... params) {
            String searchTerm = "%" + params[0] + "%";
            ContentResolver cr = getActivity().getContentResolver();

            Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            String where = MediaStore.Audio.Media.IS_MUSIC + " != ? AND ("
                    + MediaStore.Audio.Media.TITLE + " LIKE ? OR "
                    + MediaStore.Audio.Media.ARTIST + " LIKE ? OR "
                    + MediaStore.Audio.Media.ALBUM + " LIKE ? )";
            String[] whereArgs = new String[] {"0", searchTerm, searchTerm, searchTerm};
            String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";
            return cr.query(uri,
                    Constants.SONG_COLUMNS,
                    where,
                    whereArgs,
                    sortOrder);
        }

        @Override
        protected void onPostExecute(Cursor result) {
            mSongCursor = result;
            mAdapter.changeCursor(result);
        }
    }
}
