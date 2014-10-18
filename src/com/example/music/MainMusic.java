package com.example.music;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class MainMusic extends Activity {
    private ImageButton Btn_Previous;
    private ImageButton Btn_PlayOrPause;
    private ImageButton Btn_Stop;
    private ImageButton Btn_Next;
    private ListView musiclist;
    private TextView text_Current;
    private TextView text_Duration;
    private SeekBar seekBar;
    //更新进度条
    private Handler seekBarHandler;
    //当期歌曲的持续时间和当前位置
    private int duration;
    private int time;
    private RelativeLayout root_Layout;
    //进度条控制常量
    private static final int PROGRESS_INCREASE=0;
    private static final int PROGRESS_PAUSE=1;
    private static final int PROGRESS_RESET=2;
    // 当前歌曲的序号，下标从1开始
 	private int number;
    // Menu常量
 	public static final int MENU_THEME = Menu.FIRST;
 	public static final int MENU_ABOUT = Menu.FIRST + 1;
 	 
 	//广播接收器
 	private StatusChangedReceiver receiver;
 	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_music);
        findView();
        registerListeners();   
        number=1;
        Log.e( "HelloBroadReciever", "11");
        status=MusicService.STATUS_STOPPED;
        duration=0;
        time=0;
        startService(new Intent(this,MusicService.class));
        bindStatusChangedReceiver();
        sendBroadcastOnCommand(MusicService.COMMAND_CHECK_IS_PLAYING);
        initSeekBarHandler();
    }
    
   //绑定广播接收器
    private void bindStatusChangedReceiver(){
    	receiver = new StatusChangedReceiver();
    	IntentFilter filter = new IntentFilter(MusicService.BROADCAST_MUSICSERVICE_UPDATE_STATUS);
    	registerReceiver(receiver, filter);
    }
    
   //获取显示组件注册的id
    private void findView(){
    	Btn_Previous = (ImageButton) findViewById(R.id.previous);
    	Btn_PlayOrPause = (ImageButton) findViewById(R.id.play);
    	Btn_Stop = (ImageButton) findViewById(R.id.stop);
    	Btn_Next = (ImageButton) findViewById(R.id.next);
    	musiclist = (ListView) findViewById(R.id.Musiclist);
    	seekBar= (SeekBar) findViewById(R.id.seekbar);
    	text_Current=(TextView) findViewById(R.id.opentime);
    	text_Duration=(TextView) findViewById(R.id.endtime);
    	root_Layout = (RelativeLayout) findViewById(R.id.relativeLayout);
    }
    
    //为每个组件注册监听器
    private void registerListeners(){
    	//上一首
    	Btn_Previous.setOnClickListener(new OnClickListener(){
    		 public void onClick(View view){
    			sendBroadcastOnCommand(MusicService.COMMAND_PREVIOUS);	 
    		 }
    	});
    	//播放&暂停
    	Btn_PlayOrPause.setOnClickListener(new OnClickListener(){
   		 public void onClick(View view){
   			if (isPlaying()) {
   				sendBroadcastOnCommand(MusicService.COMMAND_PAUSE);	
			} else if(isPaused()) {
				sendBroadcastOnCommand(MusicService.COMMAND_RESUME);	
			} else if(isStop()){
				sendBroadcastOnCommand(MusicService.COMMAND_PLAY);
			}
   		 }
   	});
    	//停止
    	Btn_Stop.setOnClickListener(new OnClickListener(){
   		 public void onClick(View view){
   			sendBroadcastOnCommand(MusicService.COMMAND_STOP);
   		 }
   	});
    	//下一首
    	Btn_Next.setOnClickListener(new OnClickListener(){
   		 public void onClick(View view){
   			sendBroadcastOnCommand(MusicService.COMMAND_NEXT);
   		 }
   	});
    	musiclist.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				// position下标从0开始，number下标从1开始
				number = position + 1;
				sendBroadcastOnCommand(MusicService.COMMAND_PLAY);
			}
		});
    	seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener(){
    		@Override
    		public void onStopTrackingTouch(SeekBar seekBar){
    			//发广播给musicservice，执行音乐跳转
    			sendBroadcastOnCommand(MusicService.COMMAND_SEEK_TO);
    			if(isPlaying()){
    				//进度条恢复移动
    				seekBarHandler.sendEmptyMessageDelayed(PROGRESS_INCREASE, 1000);
    			}
    		}
    		@Override
    		public void onStartTrackingTouch(SeekBar seekBar){
    			seekBarHandler.sendEmptyMessage(PROGRESS_PAUSE);			
    		}
    		@Override
    		public void onProgressChanged(SeekBar seekBar,int progress,boolean fromUser){
    			time=progress;
    			text_Current.setText(formatTime(time));
    		}
    	});
    }
    
    private void moveNumberToNext() {
		// 判断是否到达了列表底端
		if ((number + 1) > musiclist.getCount()) {
			number = 1;
			Toast.makeText(MainMusic.this,
					MainMusic.this.getString(R.string.tip_reach_bottom),
					Toast.LENGTH_SHORT).show();
		} else {
			++number;
		}
	}
	private void moveNumberToPrevious() {
		// 判断是否到达了列表顶端
		if (number == 1) {
			number = musiclist.getCount();
			Toast.makeText(MainMusic.this,
					MainMusic.this.getString(R.string.tip_reach_top),
					Toast.LENGTH_SHORT).show();
		} else {
			--number;
		}
	}
	
	 //musicservice播放状态判断
    private int status;
    
    //是否正在播放音乐
    private boolean isPlaying(){
    	return status == MusicService.STATUS_PLAYING;
    }
    
    //是否暂停播放音乐
    private boolean isPaused(){
    	return status == MusicService.STATUS_PAUSED;
    }
    
    //是否停止播放音乐
    private boolean isStop(){
    	return status == MusicService.STATUS_STOPPED;
    }
    
	//musicservice播放器状态更新接收广播
	class StatusChangedReceiver extends BroadcastReceiver{
		public void onReceive(Context context,Intent intent){
			//获取musicservice播放器状态
			status = intent.getIntExtra("status", -1);
			if(status==MusicService.STATUS_PLAYING){
				time=intent.getIntExtra("time",0);
				
				duration =intent.getIntExtra("duration",0);
				 Log.e( "HelloBroadReciever", "+"+duration);
				
			}
			updateUI(status);
		}
	}
	
	//更新UI
	private void updateUI(int status){
		switch (status){
		case MusicService.STATUS_PLAYING:
			seekBarHandler.removeMessages(PROGRESS_INCREASE);
			seekBarHandler.sendEmptyMessageDelayed(PROGRESS_INCREASE, 1000);
			seekBar.setMax(duration);
			seekBar.setProgress(time);
			text_Duration.setText(formatTime(duration));
			Btn_PlayOrPause.setBackgroundResource(R.drawable.pause);
			Cursor cursor = MainMusic.this.getMusicCursor();
			cursor.moveToPosition(number - 1);
			String title = cursor.getString(cursor
					.getColumnIndex(MediaStore.Audio.AudioColumns.TITLE));
			MainMusic.this.setTitle("正在播放：" + title + " - MusicPlayer");
			break;
		case MusicService.STATUS_PAUSED:
			seekBarHandler.sendEmptyMessage(PROGRESS_PAUSE);
			Btn_PlayOrPause.setBackgroundResource(R.drawable.play);
			break;
		case MusicService.STATUS_STOPPED:
			seekBarHandler.sendEmptyMessage(PROGRESS_RESET);
			Btn_PlayOrPause.setBackgroundResource(R.drawable.play);
			break;
		case MusicService.STATUS_COMPLETED:
			sendBroadcastOnCommand(MusicService.COMMAND_NEXT);
			seekBarHandler.sendEmptyMessage(PROGRESS_RESET);
			Btn_PlayOrPause.setBackgroundResource(R.drawable.play);
			break;
		default:
			break;
		}
	}
	@Override
    protected void onResume(){
    	super.onResume();
    	MusicList();
    	
		if(musiclist.getCount()==0){
			Btn_Previous.setEnabled(false);
			Btn_PlayOrPause.setEnabled(false);
			Btn_Stop.setEnabled(false);
			Btn_Next.setEnabled(false);
			Toast.makeText(this, this.getString(R.string.tip_no_Music), Toast.LENGTH_SHORT).show();
    	}else{
    		Btn_Previous.setEnabled(true);
			Btn_PlayOrPause.setEnabled(true);
			Btn_Stop.setEnabled(true);
			Btn_Next.setEnabled(true);
    	}
		PropertyBean property = new PropertyBean(MainMusic.this);
		String theme = property.getTheme();
		// 设置Activity的主题
		setTheme(theme);
    		
    }
	@Override
    protected void onDestroy(){
    	if(isStop()){
    		stopService(new Intent(this,MusicService.class));
    	}
    	super.onDestroy();
    }
    
    //初始化播放列表
    private void MusicList(){
    	Cursor cursor = getMusicCursor();
    	setListContent(cursor);
    }
    
    //更新列表内容
    
	private void setListContent(Cursor musicCursor){
    	
		
		CursorAdapter adapter = new SimpleCursorAdapter(
    			this,android.R.layout.simple_list_item_2,musicCursor,new String[]{
    					MediaStore.Audio.AudioColumns.TITLE,
    					MediaStore.Audio.AudioColumns.ARTIST},new int[]{
    					android.R.id.text1,android.R.id.text2
    			},CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
		musiclist.setAdapter(adapter);
    }
    
    //获取扫描到的音乐媒体库
    private Cursor getMusicCursor(){
    	ContentResolver resolver = getContentResolver();
    	Cursor cursor = resolver.query(
				MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, null, null,
				null);
    	return cursor;
    }
    
    //格式化：毫秒 ->"mm:ss" 
	private String formatTime(int msec) {
		int minute = (msec / 1000) / 60;
		int second = (msec / 1000) % 60;
		String minuteString;
		String secondString;
		if (minute < 10) {
			minuteString = "0" + minute;
		} else {
			minuteString = "" + minute;
		}
		if (second < 10) {
			secondString = "0" + second;
		} else {
			secondString = "" + second;
		}
		return minuteString + ":" + secondString;
	}
	
	//控制进度条
	private void initSeekBarHandler(){
		seekBarHandler = new Handler(){
			public void handleMessage(Message msg){
				super.handleMessage(msg);
				switch(msg.what){
				case PROGRESS_INCREASE:
					if(seekBar.getProgress()<duration){
						//进度条前进1s
						seekBar.incrementProgressBy(1000);
						seekBarHandler.sendEmptyMessageDelayed(
								PROGRESS_INCREASE,1000);
						//修改显示进度的时间
						text_Current.setText(formatTime(time));
						time +=1000;
					}
					break;
				case PROGRESS_PAUSE:
					seekBarHandler.removeMessages(PROGRESS_INCREASE);
					break;
				case PROGRESS_RESET:
					seekBarHandler.removeMessages(PROGRESS_INCREASE);
					seekBar.setProgress(0);
					text_Current.setText("00:00");
					break;
				}
			}
		};
	}

    //发送命令给musicservice。控制音乐播放,参数在musicservice中
    private void sendBroadcastOnCommand(int command){
    	Intent intent = new Intent(MusicService.BROADCAST_MUSICSERVICE_CONTROL);
		intent.putExtra("command", command);
    	//根据指令的不同，封装不同的数据
    	switch(command){
    	case MusicService.COMMAND_PLAY:
    		intent.putExtra("number", number);
    		break;
    	case MusicService.COMMAND_PREVIOUS:
    		moveNumberToPrevious();
    		intent.putExtra("number", number);
    		break;
    	case MusicService.COMMAND_NEXT:
    		Log.e( "HelloBroadReciever", ""+number);
    		moveNumberToNext();
    		Log.e( "HelloBroadReciever", ""+number);
    		intent.putExtra("number", number);
    		break;
    	case MusicService.COMMAND_SEEK_TO:
    		intent.putExtra("time", time);
    		break;
    	case MusicService.COMMAND_PAUSE:
    	case MusicService.COMMAND_RESUME:
    	case MusicService.COMMAND_STOP:
    	default:
    	break;
    	}
    	sendBroadcast(intent);
    }
    
    private void setTheme(String theme) {
		if ("夜空".equals(theme)) {
			root_Layout.setBackgroundResource(R.drawable.bg_night);
		}  else if ("蓝色".equals(theme)) {
			root_Layout.setBackgroundResource(R.drawable.bg_blue);
		} else if ("线条".equals(theme)) {
			root_Layout.setBackgroundResource(R.drawable.bg_line);
		} else if ("天空".equals(theme)) {
			root_Layout.setBackgroundResource(R.drawable.bg_sky);
		}
	}
    // 创建菜单
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, MENU_THEME, 0, "主题");
		menu.add(0, MENU_ABOUT, 1, "关于");
		return super.onCreateOptionsMenu(menu);
	}

	// 处理菜单点击事件 
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_THEME:
			// 显示列表对话框
			new AlertDialog.Builder(this).setTitle("请选择主题").setItems(R.array.theme,
					new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {
									// 获取在array.xml中定义的主题名称
									String theme = PropertyBean.THEMES[which];
									// 设置Activity的主题
									setTheme(theme);
									// 保存选择的主题
									PropertyBean property = new PropertyBean(
											MainMusic.this);
									property.setAndSaveTheme(theme);
								}
							}).show();
			break;
		case MENU_ABOUT:
			// 显示文本对话框
			new AlertDialog.Builder(MainMusic.this).setTitle("Music")
					.setMessage(MainMusic.this.getString(R.string.about)).show();
			break;
		}
		return super.onOptionsItemSelected(item);
	}
}


