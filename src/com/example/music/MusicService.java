package com.example.music;

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
	private MediaPlayer player;
		
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		// 绑定广播接收器，可以接收广播
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
			//执行命令
			switch (command){
			case COMMAND_SEEK_TO:
				seekTo(intent.getIntExtra("time", 0));
				break;
			case COMMAND_PLAY:
			case COMMAND_PREVIOUS:
			case COMMAND_NEXT:
				int number = intent.getIntExtra("number", 1);
				Toast.makeText(MusicService.this, "正在播放第"+number+"首歌曲", Toast.LENGTH_SHORT).show();
				play(number);
				Log.e( "HelloBroadReciever", ""+number);
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
	
	//读取音乐文件
	private void load(int number) {
		// 之前的资源释放掉
		if (player != null) {
			player.release();
		}
		//Uri musicUri = Uri.withAppendedPath(
		//		MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, "" + number);
		// 读取音乐文件，创建MediaPlayer对象
		//player = MediaPlayer.create(this, musicUri);
		
		//获取扫描到的音乐媒体库
		ContentResolver reslover=getContentResolver();
		//获取扫描到的音乐媒体库
    	Cursor cursor=reslover.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null,null, null,null);
    	cursor.move(number);	
    			 String  musicUri = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));  
    			 Log.e( "MusicUrl", ""+musicUri);
    			 cursor.moveToNext(); //查询得到cursor后第一次调用moveToFirst或moveToNext都可以将cursor移动到第一条记录上	
    			 try{    //在调用setDataSource(), 方法的时候一定要catch 获得 抛出  IllegalArgumentException 和 IOException 两个异常,因为你定义的文件可能不存在.
    					player=new MediaPlayer();
    					player.setDataSource(musicUri);
    			        player.prepare();
    			        player.start();
    					}catch (IllegalArgumentException e) {
    			            e.printStackTrace();
    			         } catch (IllegalStateException e) {
    			            e.printStackTrace();
    			         } catch (IOException e) {
    			            e.printStackTrace();
    		         }
		// 注册监听器
		player.setOnCompletionListener(completionListener);
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
		sendBroadcastOnStatusChanged(MusicService.STATUS_PLAYING);
	}
	
	//暂停音乐
	private void pause() {
		if (player.isPlaying()) {
			player.pause();
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
		sendBroadcastOnStatusChanged(MusicService.STATUS_PLAYING);
	}

	//重新播放
	private void replay() {
		player.start();
		sendBroadcastOnStatusChanged(MusicService.STATUS_PLAYING);
	}
		
	//跳转至播放位置
	private void seekTo(int time){
		if(player != null){
			player.seekTo(time);
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
		if(status == STATUS_PLAYING){
			intent.putExtra("time", player.getCurrentPosition());
			intent.putExtra("duration", player.getDuration());
			Log.e( "HelloBroadReciever", "123"+player.getCurrentPosition());
			Log.e( "HelloBroadReciever", "-"+player.getDuration());
		
			
		}
		sendBroadcast(intent);
	}
		

}
