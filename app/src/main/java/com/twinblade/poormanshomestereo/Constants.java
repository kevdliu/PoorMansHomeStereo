package com.twinblade.poormanshomestereo;

import android.provider.MediaStore;

public class Constants {

    static final int SERVER_PORT = 6969;
    public static final int BROADCAST_PORT = 9696;

    private static final String PKG = Constants.class.getClass().getPackage().getName();
    public static final String BROADCAST_KEY = PKG + ".BROADCAST";
    public static final int BROADCAST_RESPONSE_TIMEOUT = 1500;
    public static final String BROADCAST_RESPONSE_PREFIX = PKG + ".BROADCAST_RESPONSE.";

    static final String SPEAKER_STATUS_URL = "state.json";
    static final String SPEAKER_COMMAND_URL = "cmd";
    static final String CONTROLLER_FILE_URL = "res.mp3";
    static final String CONTROLLER_MSG_URL = "msg";

    public static final int LIST_BITMAP_CACHE_SIZE = 20;

    static final int SPEAKER_NOTIFICATION_ID = 1;
    static final int CONTROLLER_NOTIFICATION_ID = 2;

    static final int STORAGE_PERMISSION_REQUEST_ID = 1;
    public static final int CAMERA_PERMISSION_REQUEST_ID = 2;

    static final String SPEAKER_STATUS = "speaker_status";
    static final String SPEAKER_STATUS_PLAYING = "playing";
    static final String SPEAKER_STATUS_STOPPED = "stopped";
    static final String SPEAKER_STATUS_END_OF_SONG = "end_of_song";

    static final String SPEAKER_REQUEST = "speaker_request";
    static final String SPEAKER_REQUEST_PAUSE = "pause";
    static final String SPEAKER_REQUEST_RESUME = "resume";
    static final String SPEAKER_REQUEST_SKIP_NEXT = "skip_next";
    static final String SPEAKER_REQUEST_SKIP_PREVIOUS = "skip_previous";
    static final String SPEAKER_REQUEST_SEEK_FORWARD = "seek_forward";
    static final String SPEAKER_REQUEST_SEEK_BACK = "seek_back";

    static final String SPEAKER_COMMAND = "speaker_command";
    static final String SPEAKER_COMMAND_PLAY = "play";
    static final String SPEAKER_COMMAND_RESUME = "resume";
    static final String SPEAKER_COMMAND_PAUSE = "pause";
    static final String SPEAKER_COMMAND_SEEK = "seek";
    static final String SPEAKER_COMMAND_SEEK_TIME = "seek_time";

    static final int SEEK_DISTANCE_SECONDS = 15;

    static final String FRAGMENT_SPEAKERS = "speakers";
    public static final String FRAGMENT_SONGS = "songs";
    static final String FRAGMENT_SEARCH = "search";
    public static final String FRAGMENT_QUEUE = "queue";

    public static final int MENU_PLAY_NEXT = 0;
    public static final int MENU_ADD_TO_QUEUE = 1;
    public static final int MENU_REMOVE_FROM_QUEUE = 2;

    public static final String[] SONG_COLUMNS = new String[] {MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.DATA};

    static final String INTENT_STOP_CONTROLLER_SERVICE = PKG + ".STOP_CONTROLLER_SERVICE";
    static final String INTENT_STOP_SPEAKER_SERVICE = PKG + ".STOP_SPEAKER_SERVICE";
    static final String INTENT_SPEAKER_TOGGLE_PLAYBACK = PKG + ".SPEAKER_TOGGLE_PLAYBACK";
    static final String INTENT_SPEAKER_NEXT_SONG = PKG + ".SPEAKER_NEXT_SONG";
}
