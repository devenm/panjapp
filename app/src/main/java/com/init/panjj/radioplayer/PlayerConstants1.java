package com.init.panjj.radioplayer;

import android.os.Handler;


import java.util.ArrayList;

public class PlayerConstants1 {
	//List of Songs
	//public static ArrayList<MediaItem> SONGS_LIST = new ArrayList<MediaItem>();
	public static ArrayList<RadioBean> SONGS_LIST1 = new ArrayList<RadioBean>();
	//song number which is playing right now from SONGS_LIST
	public static int SONG_NUMBER = 0;
	//song is playing or paused
	public static boolean SONG_PAUSED = true;
	//song changed (next, previous)
	public static boolean SONG_CHANGED = false;
	//handler for song changed(next, previous) defined in service(SongService)
	public static Handler SONG_CHANGE_HANDLER;
	//handler for song play/pause defined in service(SongService)
	public static Handler PLAY_PAUSE_HANDLER;
	//handler for showing song progress defined in Activities(MainActivity, AudioPlayerActivity)
	public static Handler PROGRESSBAR_HANDLER;
	public static int SEEK_TO;
	public static Handler SEEKBAR_HANDLER;
}
