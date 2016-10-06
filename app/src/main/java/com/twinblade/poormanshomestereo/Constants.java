package com.twinblade.poormanshomestereo;

import android.provider.MediaStore;

public class Constants {

    public static final int CONTROLLER_SERVER_PORT = 6969;
    public static final int SPEAKER_SERVER_PORT = 6969;

    public static final int LIST_BITMAP_CACHE_SIZE = 20;

    public static final String SPEAKER_STATUS = "speaker_status";
    public static final String SPEAKER_STATUS_PLAYING = "playing";
    public static final String SPEAKER_STATUS_PAUSED = "paused";
    public static final String SPEAKER_STATUS_STOPPED = "stopped";
    public static final String SPEAKER_STATUS_ERROR = "error";
    public static final String SPEAKER_STATUS_END_OF_SONG = "end_of_song";
    public static final String SPEAKER_STATUS_SEEK = "seek";

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
}
