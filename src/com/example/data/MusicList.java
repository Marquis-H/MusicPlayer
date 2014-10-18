package com.example.data;
import java.util.ArrayList;
/**
 * Created by Marquis on 2014/10/18.
 */
public class MusicList {
    private static ArrayList<Music> musicarray = new ArrayList<Music>();
    private MusicList(){}

    public static ArrayList<Music> getMusicList()
    {
        return musicarray;
    }
}
