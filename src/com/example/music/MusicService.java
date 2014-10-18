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
	// ���ſ��������ʶ����
	public static final int COMMAND_UNKNOWN = -1;
	public static final int COMMAND_PLAY = 0;
	public static final int COMMAND_PAUSE = 1;
	public static final int COMMAND_STOP = 2;
	public static final int COMMAND_RESUME = 3;
	public static final int COMMAND_PREVIOUS = 4;
	public static final int COMMAND_NEXT = 5;
	public static final int COMMAND_CHECK_IS_PLAYING = 6;
	public static final int COMMAND_SEEK_TO = 7;
	// ������״̬
	public static final int STATUS_PLAYING = 0;
	public static final int STATUS_PAUSED = 1;
	public static final int STATUS_STOPPED = 2;
	public static final int STATUS_COMPLETED = 3;
	// �㲥��ʶ
	public static final String BROADCAST_MUSICSERVICE_CONTROL = "MusicService.ACTION_CONTROL";
	public static final String BROADCAST_MUSICSERVICE_UPDATE_STATUS = "MusicService.ACTION_UPDATE";
	
	//�㲥������
	private CommandReceiver receiver;
	
	// ý�岥����
	private MediaPlayer player;
		
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		// �󶨹㲥�����������Խ��չ㲥
		bindCommandReceiver();
		Toast.makeText(this, "MusicService.onCreate()", Toast.LENGTH_SHORT).show();		
	}


	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return super.onStartCommand(intent, flags, startId);  
	}

	@Override
	public void onDestroy() {
		// �ͷŲ�������Դ
		if (player != null) {
			player.release();
		}
		super.onDestroy();
	}
	
	
	
	//����mainmusic�����Ĺ㲥�����ִ�в���
	class CommandReceiver extends BroadcastReceiver{
		public void onReceive(Context context,Intent intent){
			//��ȡ����
			int command = intent.getIntExtra("command", COMMAND_UNKNOWN);
			//ִ������
			switch (command){
			case COMMAND_SEEK_TO:
				seekTo(intent.getIntExtra("time", 0));
				break;
			case COMMAND_PLAY:
			case COMMAND_PREVIOUS:
			case COMMAND_NEXT:
				int number = intent.getIntExtra("number", 1);
				Toast.makeText(MusicService.this, "���ڲ��ŵ�"+number+"�׸���", Toast.LENGTH_SHORT).show();
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
	
	//��ȡ�����ļ�
	private void load(int number) {
		// ֮ǰ����Դ�ͷŵ�
		if (player != null) {
			player.release();
		}
		//Uri musicUri = Uri.withAppendedPath(
		//		MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, "" + number);
		// ��ȡ�����ļ�������MediaPlayer����
		//player = MediaPlayer.create(this, musicUri);
		
		//��ȡɨ�赽������ý���
		ContentResolver reslover=getContentResolver();
		//��ȡɨ�赽������ý���
    	Cursor cursor=reslover.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null,null, null,null);
    	cursor.move(number);	
    			 String  musicUri = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));  
    			 Log.e( "MusicUrl", ""+musicUri);
    			 cursor.moveToNext(); //��ѯ�õ�cursor���һ�ε���moveToFirst��moveToNext�����Խ�cursor�ƶ�����һ����¼��	
    			 try{    //�ڵ���setDataSource(), ������ʱ��һ��Ҫcatch ��� �׳�  IllegalArgumentException �� IOException �����쳣,��Ϊ�㶨����ļ����ܲ�����.
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
		// ע�������
		player.setOnCompletionListener(completionListener);
	}

	// ���Ž���������
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
	
	//���ֲ���
	private void play(int number){
		if(player != null&& player.isPlaying()){
			player.stop();
		}
		load(number);
		player.start();
		sendBroadcastOnStatusChanged(MusicService.STATUS_PLAYING);
	}
	
	//��ͣ����
	private void pause() {
		if (player.isPlaying()) {
			player.pause();
			sendBroadcastOnStatusChanged(MusicService.STATUS_PAUSED);
		}
	}

	//ֹͣ����
	private void stop() {
		if (player != null) {
			player.stop();
			sendBroadcastOnStatusChanged(MusicService.STATUS_STOPPED);
		}
	}

	//�ָ�����
	private void resume() {
		player.start();
		sendBroadcastOnStatusChanged(MusicService.STATUS_PLAYING);
	}

	//���²���
	private void replay() {
		player.start();
		sendBroadcastOnStatusChanged(MusicService.STATUS_PLAYING);
	}
		
	//��ת������λ��
	private void seekTo(int time){
		if(player != null){
			player.seekTo(time);
		}
	}
	//��musicservice�Ĺ㲥��������mainmusic���а󶨡����������ܹ���
	private void bindCommandReceiver() {
		receiver = new CommandReceiver();
		IntentFilter filter = new IntentFilter(BROADCAST_MUSICSERVICE_CONTROL);
		registerReceiver(receiver, filter);
	}
		
	//musicservice������״̬�Թ㲥����ʽ���͸�mainmusic
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
