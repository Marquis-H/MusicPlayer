package com.example.music;
import com.example.data.MusicList;

import java.io.IOException;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.Uri;
import android.os.IBinder;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

public class MusicService extends Service {
	// 播放控制命令，标识操作
	public static final int COMMAND_UNKNOWN = -1;
	public static final int COMMAND_PLAY = 0;
	public static final int COMMAND_PAUSE = 1;
	public static final int COMMAND_STOP = 2;
	public static final int COMMAND_RESUME = 3;
	public static final int COMMAND_PREVIOUS = 4;
	public static final int COMMAND_NEXT = 5;
	public static final int COMMAND_CHECK_IS_PLAYING = 6;
	public static final int COMMAND_SEEK_TO = 7;
	// 播放器状态
	public static final int STATUS_PLAYING = 0;
	public static final int STATUS_PAUSED = 1;
	public static final int STATUS_STOPPED = 2;
	public static final int STATUS_COMPLETED = 3;
	// 广播标识
	public static final String BROADCAST_MUSICSERVICE_CONTROL = "MusicService.ACTION_CONTROL";
	public static final String BROADCAST_MUSICSERVICE_UPDATE_STATUS = "MusicService.ACTION_UPDATE";
	
	//广播接收器
	private CommandReceiver receiver;
    // 媒体播放类
    private MediaPlayer player = new MediaPlayer();
    //歌曲序号，从0开始
    private int number = 0;
    private int status;
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		//绑定广播接收器，可以接收广播
		bindCommandReceiver();
		Toast.makeText(this, "MusicService.onCreate()", Toast.LENGTH_SHORT).show();		
	}


	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return super.onStartCommand(intent, flags, startId);  
	}

	@Override
	public void onDestroy() {
		// 释放播放器资源
		if (player != null) {
			player.release();
		}
		super.onDestroy();
	}
	
	
	
	//接收mainmusic传来的广播命令，并执行操作
	class CommandReceiver extends BroadcastReceiver{
		public void onReceive(Context context,Intent intent){
			//获取命令
			int command = intent.getIntExtra("command", COMMAND_UNKNOWN);
			//ִ执行命令
			switch (command){
			case COMMAND_SEEK_TO:
				seekTo(intent.getIntExtra("time", 0));
				break;
			case COMMAND_PLAY:
                number = intent.getIntExtra("number", 0);
                play(number);
                break;
			case COMMAND_PREVIOUS:
                moveNumberToPrevious();
                break;
			case COMMAND_NEXT:
                moveNumberToNext();
                break;
			case COMMAND_PAUSE:
				pause();
				break;
			case COMMAND_STOP:
				stop();
				break;
			case COMMAND_RESUME:
				resume();
				break;
			case COMMAND_CHECK_IS_PLAYING:
				if(player.isPlaying()&& player != null){
					sendBroadcastOnStatusChanged(MusicService.STATUS_PLAYING);
				}
				break;
			case COMMAND_UNKNOWN:
			default:
				break;
			}
		}
	}
	
	//��ȡ�����ļ�
	private void load(int number) {
        try {
            player.reset();
            player.setDataSource(MusicList.getMusicList().get(number).getmusicPath());
            player.prepare();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
		player.setOnCompletionListener(completionListener);
	}
    /** 选择下一曲 */
    private void moveNumberToNext() {
        // 判断是否到达了列表底端
        if ((number ) == MusicList.getMusicList().size()-1) {
            Toast.makeText(MusicService.this,"已经到达列表底部",Toast.LENGTH_SHORT).show();
        } else {
            ++number;
            play(number);
        }
    }

    /** 选择下一曲 */
    private void moveNumberToPrevious() {
        // 判断是否到达了列表顶端
        if (number == 0) {
            Toast.makeText(MusicService.this,"已经到达列表顶部",Toast.LENGTH_SHORT).show();
        } else {
            --number;
            play(number);
        }
    }
	// 播放结束监听器
	OnCompletionListener completionListener = new OnCompletionListener() {
		@Override
		public void onCompletion(MediaPlayer player) {
			if (player.isLooping()) {
				replay();
			} else {
				sendBroadcastOnStatusChanged(MusicService.STATUS_COMPLETED);
			}
		}
	};
	
	//音乐播放
	private void play(int number){
		if(player != null&& player.isPlaying()){
			player.stop();
		}
		load(number);
		player.start();
        status = MusicService.STATUS_PLAYING;
		sendBroadcastOnStatusChanged(MusicService.STATUS_PLAYING);
	}
	
	//暂停音乐
	private void pause() {
		if (player.isPlaying()) {
			player.pause();
            status = MusicService.STATUS_PAUSED;
			sendBroadcastOnStatusChanged(MusicService.STATUS_PAUSED);
		}
	}

	//停止播放
	private void stop() {
		if (player != null) {
			player.stop();
			sendBroadcastOnStatusChanged(MusicService.STATUS_STOPPED);
		}
	}

	//恢复播放
	private void resume() {
		player.start();
        status = MusicService.STATUS_PLAYING;
		sendBroadcastOnStatusChanged(MusicService.STATUS_PLAYING);
	}

	//重新播放
	private void replay() {
		player.start();
        status = MusicService.STATUS_PLAYING;
		sendBroadcastOnStatusChanged(MusicService.STATUS_PLAYING);
	}
		
	//跳转至播放位置
	private void seekTo(int time){
		if(player != null){
			player.seekTo(time);
            status = MusicService.STATUS_PLAYING;
            sendBroadcastOnStatusChanged(MusicService.STATUS_PLAYING);
		}
	}
	//将musicservice的广播接收器跟mainmusic进行绑定、接收器才能工作
	private void bindCommandReceiver() {
		receiver = new CommandReceiver();
		IntentFilter filter = new IntentFilter(BROADCAST_MUSICSERVICE_CONTROL);
		registerReceiver(receiver, filter);
	}
		
	//musicservice将最新状态以广播的形式发送给mainmusic
	private void sendBroadcastOnStatusChanged(int status){
		Intent intent = new Intent(BROADCAST_MUSICSERVICE_UPDATE_STATUS);
		intent.putExtra("status", status);
		if(status !=STATUS_STOPPED){
			intent.putExtra("time", player.getCurrentPosition());
			intent.putExtra("duration", player.getDuration());
            intent.putExtra("number", number);
            intent.putExtra("musicName", MusicList.getMusicList().get(number).getmusicName());
            intent.putExtra("musicArtist", MusicList.getMusicList().get(number).getmusicArtist());
		
			
		}
		sendBroadcast(intent);
	}
		

}
