package com.twinblade.poormanshomestereo;

import android.provider.MediaStore;

public class Constants {

    public static final int SERVER_PORT = 6969;
    public static final int BROADCAST_PORT = 9696;

    private static final String PKG = Constants.class.getClass().getPackage().getName();
    public static final String BROADCAST_KEY = PKG + ".BROADCAST";
    public static final int BROADCAST_RESPONSE_TIMEOUT = 3000;
    public static final String BROADCAST_RESPONSE_PREFIX = PKG + ".BROADCAST_RESPONSE.";

    public static final String SPEAKER_STATUS_URL = "state.json";
    public static final String SPEAKER_COMMAND_URL = "cmd";
    public static final String CONTROLLER_FILE_URL = "res.mp3";
    public static final String CONTROLLER_MSG_URL = "msg";

    public static final int LIST_BITMAP_CACHE_SIZE = 20;

    public static final int SPEAKER_NOTIFICATION_ID = 1;

    public static final String SPEAKER_STATUS = "speaker_status";
    public static final String SPEAKER_STATUS_PLAYING = "playing";
    public static final String SPEAKER_STATUS_STOPPED = "stopped";
    public static final String SPEAKER_STATUS_ERROR = "error";
    public static final String SPEAKER_STATUS_END_OF_SONG = "end_of_song";
    public static final String SPEAKER_STATUS_POSITION = "position";

    public static final String SPEAKER_COMMAND = "speaker_command";
    public static final String SPEAKER_COMMAND_PLAY = "play";
    public static final String SPEAKER_COMMAND_RESUME = "resume";
    public static final String SPEAKER_COMMAND_PAUSE = "pause";
    public static final String SPEAKER_COMMAND_SEEK = "seek";

    public static final String FRAGMENT_SPEAKERS = "speakers";
    public static final String FRAGMENT_SONGS = "songs";
    public static final String FRAGMENT_SEARCH = "search";
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

    public static final String INTENT_STOP_CONTROLLER_SERVICE = PKG + ".STOP_CONTROLLER_SERVICE";
    public static final String INTENT_STOP_SPEAKER_SERVICE = PKG + ".STOP_SPEAKER_SERVICE";
}
