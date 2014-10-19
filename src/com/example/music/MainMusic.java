package com.example.music;

import java.sql.Time;
import java.util.*;

import android.media.AudioManager;
import android.view.KeyEvent;
import android.widget.*;
import com.example.data.Music;
import com.example.data.MusicList;

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
import android.widget.SeekBar.OnSeekBarChangeListener;
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
    private String musicName;
    private String musicArtist;
    private RelativeLayout root_Layout;
    //进度条控制常量
    private static final int PROGRESS_INCREASE=0;
    private static final int PROGRESS_PAUSE=1;
    private static final int PROGRESS_RESET=2;
    //播放模式常量
    private static final int MODE_LIST_SEQUENCE=0;
    private static final int MODE_SINGLE_CYCLE=1;
    private static final int MODE_LIST_CYCLE=2;
    private int playmode;
    // 当前歌曲的序号，下标从1开始
 	private int number;
    // Menu常量
 	public static final int MENU_THEME = Menu.FIRST;
 	public static final int MENU_ABOUT = Menu.FIRST + 1;
 	 
 	//广播接收器
 	private StatusChangedReceiver receiver;
    //歌曲列表对象
    private ArrayList<Music> musicArrayList;
    //退出判断标记
    private static Boolean isExit = false;
    //音量控制
    private TextView vol;
    private SeekBar seekBar_vol;
    //睡眠模式相关组件，标示常量
    private ImageView iv_sleep;
    private Timer timer_sleep;
    private static final boolean NOTSLEEP = false;
    private static final boolean ISSLEEP = true;
    //默认的睡眠时间为20
    private int sleeptime = 20;
    //标示是否打开睡眠模式
    private static boolean sleepmode;
 	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_music);
        findView();
        registerListeners();
        initMusicList();
        initListView();
        checkMusicfile();
        startService(new Intent(this,MusicService.class));
        bindStatusChangedReceiver();
        sendBroadcastOnCommand(MusicService.COMMAND_CHECK_IS_PLAYING);
        initSeekBarHandler();
        status = MusicService.COMMAND_STOP;
        //默认播放模式是顺序模式
        playmode = MainMusic.MODE_LIST_SEQUENCE;
        //默认睡眠模式为关闭状态
        sleepmode = MainMusic.NOTSLEEP;
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
        vol = (TextView) findViewById(R.id.main_volumeText);
        seekBar_vol = (SeekBar) findViewById(R.id.main_volumebar);
        iv_sleep = (ImageView)findViewById(R.id.main_sleep);
    }
    
    //为每个组件注册监听器
    private void registerListeners(){
    	//上一首
    	Btn_Previous.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                sendBroadcastOnCommand(MusicService.COMMAND_PREVIOUS);
            }
        });
    	//播放&暂停
    	Btn_PlayOrPause.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                if (isPlaying()) {
                    sendBroadcastOnCommand(MusicService.COMMAND_PAUSE);
                } else if (isPaused()) {
                    sendBroadcastOnCommand(MusicService.COMMAND_RESUME);
                } else if (isStop()) {
                    sendBroadcastOnCommand(MusicService.COMMAND_PLAY);
                }
            }
        });
    	//停止
    	Btn_Stop.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                sendBroadcastOnCommand(MusicService.COMMAND_STOP);
            }
        });
    	//下一首
    	Btn_Next.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                sendBroadcastOnCommand(MusicService.COMMAND_NEXT);
            }
        });
    	musiclist.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                // position下标从0开始，number下标从1开始
                number = position;
                sendBroadcastOnCommand(MusicService.COMMAND_PLAY);
            }
        });
    	seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                //发广播给musicservice，执行音乐跳转
                sendBroadcastOnCommand(MusicService.COMMAND_SEEK_TO);
                if (isPlaying()) {
                    //进度条恢复移动
                    sendBroadcastOnCommand(MusicService.COMMAND_SEEK_TO);
                    seekBarHandler.sendEmptyMessageDelayed(PROGRESS_INCREASE, 1000);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                seekBarHandler.sendEmptyMessage(PROGRESS_PAUSE);
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                time = progress;
                text_Current.setText(formatTime(time));
            }
        });
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
                musicName = intent.getStringExtra("musicName");
                musicArtist = intent.getStringExtra("musicArtist");
				duration =intent.getIntExtra("duration",0);
                number = intent.getIntExtra("number", number);
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
            musiclist.setSelection(number);

			MainMusic.this.setTitle("正在播放：" + musicName + " - "+musicArtist);
			break;
		case MusicService.STATUS_PAUSED:
			seekBarHandler.sendEmptyMessage(PROGRESS_PAUSE);
            String string = MainMusic.this.getTitle().toString().replace("正在播放:", "已暂停:");
            MainMusic.this.setTitle(string);
			Btn_PlayOrPause.setBackgroundResource(R.drawable.play);
			break;
		case MusicService.STATUS_STOPPED:
            time = 0;
            duration = 0;
            text_Current.setText(formatTime(time));
            text_Duration.setText(formatTime(duration));
			seekBarHandler.sendEmptyMessage(PROGRESS_RESET);
            MainMusic.this.setTitle("Music");
			Btn_PlayOrPause.setBackgroundResource(R.drawable.play);
			break;
		case MusicService.STATUS_COMPLETED:
            if(playmode == MainMusic.MODE_LIST_SEQUENCE)
            {
                if(number == MusicList.getMusicList().size()-1)
                {
                    sendBroadcastOnCommand(MusicService.STATUS_STOPPED);
                }
                else
                {
                    sendBroadcastOnCommand(MusicService.COMMAND_NEXT);
                }
            }
            else if(playmode == MainMusic.MODE_SINGLE_CYCLE)
                sendBroadcastOnCommand(MusicService.COMMAND_PLAY);

            else if(playmode == MainMusic.MODE_LIST_CYCLE)
            {
                if(number ==musicArrayList.size()-1)
                {
                    number = 0;
                    sendBroadcastOnCommand(MusicService.COMMAND_PLAY);
                }
                else
                {
                    sendBroadcastOnCommand(MusicService.COMMAND_NEXT);
                }
            }

			seekBarHandler.sendEmptyMessage(PROGRESS_RESET);
            MainMusic.this.setTitle("Music");
			Btn_PlayOrPause.setBackgroundResource(R.drawable.play);
			break;
		default:
			break;
		}
	}
	@Override
    protected void onResume(){
    	super.onResume();
        sendBroadcastOnCommand(MusicService.COMMAND_CHECK_IS_PLAYING);
		PropertyBean property = new PropertyBean(MainMusic.this);
		String theme = property.getTheme();
		// 设置Activity的主题
		setTheme(theme);
    	audio_control();
        //睡眠模式打开时显示图标，关闭时隐藏图标
        if(sleepmode == MainMusic.ISSLEEP) iv_sleep.setVisibility(View.VISIBLE);
        else iv_sleep.setVisibility(View.INVISIBLE);
    }
	@Override
    protected void onDestroy(){
    	if(isStop()){
    		stopService(new Intent(this,MusicService.class));
    	}
    	super.onDestroy();
    }
    
    //初始化播放列表
    private void initMusicList(){
        musicArrayList = MusicList.getMusicList();
        //避免重复添加音乐
        if(musicArrayList.isEmpty())
        {
            Cursor mMusicCursor = this.getContentResolver().query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, null, null,
                    MediaStore.Audio.AudioColumns.TITLE);
            //标题
            int indexTitle = mMusicCursor.getColumnIndex(MediaStore.Audio.AudioColumns.TITLE);
            //艺术家
            int indexArtist = mMusicCursor.getColumnIndex(MediaStore.Audio.AudioColumns.ARTIST);
            //总时长
            int indexTotalTime = mMusicCursor.getColumnIndex(MediaStore.Audio.AudioColumns.DURATION);
            //路径
            int indexPath = mMusicCursor.getColumnIndex(MediaStore.Audio.AudioColumns.DATA);

            /**通过mMusicCursor游标遍历数据库，并将Music类对象加载带ArrayList中*/
            for (mMusicCursor.moveToFirst(); !mMusicCursor.isAfterLast(); mMusicCursor
                    .moveToNext()) {
                String strTitle = mMusicCursor.getString(indexTitle);
                String strArtist = mMusicCursor.getString(indexArtist);
                String strTotoalTime = mMusicCursor.getString(indexTotalTime);
                String strPath = mMusicCursor.getString(indexPath);

                if (strArtist.equals("<unknown>"))
                    strArtist = "无艺术家";
                Music music = new Music(strTitle, strArtist, strPath, strTotoalTime);
                musicArrayList.add(music);
            }
        }
    }
    
    //更新列表内容

    /**设置适配器并初始化listView*/
    private void initListView() {
        List<Map<String, String>> list_map = new ArrayList<Map<String, String>>();
        HashMap<String, String> map;
        SimpleAdapter simpleAdapter;
        for (Music music : musicArrayList) {
            map = new HashMap<String, String>();
            map.put("musicName", music.getmusicName());
            map.put("musicArtist", music.getmusicArtist());
            list_map.add(map);
        }

        String[] from = new String[] { "musicName", "musicArtist" };
        int[] to = { R.id.listview_tv_title_item, R.id.listview_tv_artist_item };

        simpleAdapter = new SimpleAdapter(this, list_map, R.layout.listview,from, to);
        musiclist.setAdapter(simpleAdapter);
    }


    private void checkMusicfile() {
        if (musicArrayList.isEmpty()) {
            Btn_Previous.setEnabled(false);
            Btn_PlayOrPause.setEnabled(false);
            Btn_Stop.setEnabled(false);
            Btn_Next.setEnabled(false);
            Toast.makeText(this, this.getString(R.string.tip_no_Music), Toast.LENGTH_SHORT).show();
        } else {
            Btn_Previous.setEnabled(true);
            Btn_PlayOrPause.setEnabled(true);
            Btn_Stop.setEnabled(true);
            Btn_Next.setEnabled(true);
        }
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
    	case MusicService.COMMAND_NEXT:
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

    private void exitByDoubleClick()
    {
        Timer timer = null;
        if(isExit == false)
        {
            isExit = true;
            Toast.makeText(this, "再按一次退出！", Toast.LENGTH_SHORT).show();
            timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    isExit = false;
                    finish();
                }
            }, 2000);
        }
        else
        {
            System.exit(0);
        }


    }

    //重写onKeyDown方法
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        int progress;
        switch (keyCode)
        {
            case KeyEvent.KEYCODE_BACK:
                exitByDoubleClick();
                break;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                progress = seekBar_vol.getProgress();
                if(progress !=0)
                    seekBar_vol.setProgress(progress-1);
                return true;
            case KeyEvent.KEYCODE_VOLUME_UP:
                progress = seekBar_vol.getProgress();
                if(progress != seekBar_vol.getMax());
                seekBar_vol.setProgress(progress+1);
                return true;
            default:
                break;
        }
        return false;
    }

    //控制音量
    private void audio_control()
    {
        //获取音量管理器
        final AudioManager audioManager = (AudioManager)this.getSystemService(Context.AUDIO_SERVICE);
        //设置当前调节音量大小只是针对媒体音乐
        this.setVolumeControlStream(AudioManager.STREAM_MUSIC);
        //设置滑动条最大值
        final int max_progress = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        seekBar_vol.setMax(max_progress);
        //获取当前音量
        int progress = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        seekBar_vol.setProgress(progress);
        vol.setText("音量："+(progress*100/max_progress)+"%");
        seekBar_vol.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                vol.setText("音量："+(i*100/max_progress)+"%");
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,i,AudioManager.FLAG_PLAY_SOUND);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    //获取到该布局上的组件对象
    private void showSleepDialog()
    {
        //先用getLayoutInflater().inflate方法获取sleep.xml布局组件，并初始化View
        final View userview = this.getLayoutInflater().inflate(R.layout.sleep,null);
        //通过View类的findViewById方法获取到组件对象
        final TextView sleep_text = (TextView)userview.findViewById(R.id.sleep_text);
        final Switch sleep_switch = (Switch)userview.findViewById(R.id.sleep_switch);
        final SeekBar sleep_seekbar = (SeekBar)userview.findViewById(R.id.sleep_seekbar);

        sleep_text.setText("睡眠于："+sleeptime+"分钟后");
        //根据当前的睡眠状态来确定Switch的状态
        if(sleepmode == MainMusic.ISSLEEP) sleep_switch.setChecked(true);
        sleep_seekbar.setMax(60);
        sleep_seekbar.setProgress(sleeptime);
        sleep_seekbar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                sleeptime = i;
                sleep_text.setText("睡眠于："+sleeptime+"分钟后");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        sleep_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                sleepmode = b;
            }
        });
        //定义定时器任务
        final TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                System.exit(0);
            }
        };
        //定义对话框以及初始化
        final AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle("选择睡眠时间（0~60分钟）");
        //设置布局
        dialog.setView(userview);
        //设置取消按钮响应事件
        dialog.setNegativeButton("取消",new DialogInterface.OnClickListener(){
            public void onClick(DialogInterface arg0,int arg1){
                arg0.dismiss();
            }
        });
        //设置重置按钮响应时间
        dialog.setNegativeButton("重置",new DialogInterface.OnClickListener(){
            public void onClick(DialogInterface arg0,int arg1){
                sleepmode = MainMusic.NOTSLEEP;
                sleeptime = 20;
                timerTask.cancel();
                iv_sleep.setVisibility(View.INVISIBLE);
            }
        });
        //设置确定按钮响应事件
        dialog.setPositiveButton("确定",new DialogInterface.OnClickListener(){
            public void onClick(DialogInterface arg0,int arg1){
                if(sleepmode == MainMusic.ISSLEEP)
                {
                    timer_sleep = new Timer();
                    int time = sleep_seekbar.getProgress();
                    //启动任务
                    timer_sleep.schedule(timerTask,time*60*1000);
                    iv_sleep.setVisibility(View.VISIBLE);
                }
                else
                {
                    //取消任务
                    timerTask.cancel();
                    timer_sleep.cancel();
                    arg0.dismiss();
                    iv_sleep.setVisibility(View.INVISIBLE);
                }
            }
        });
        dialog.show();

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
        getMenuInflater().inflate(R.menu.main_music, menu);
        return super.onCreateOptionsMenu(menu);
	}

	// 处理菜单点击事件 
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_theme:
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
		case R.id.menu_about:
			// 显示文本对话框
			new AlertDialog.Builder(MainMusic.this).setTitle("Music")
					.setMessage(MainMusic.this.getString(R.string.about)).show();
			break;
            case R.id.menu_quit:
                //退出程序
                new AlertDialog.Builder(MainMusic.this).setTitle("提示")
                        .setMessage(R.string.quit_message).setPositiveButton("确定", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        // TODO Auto-generated method stub
                        System.exit(0);
                    }
                }).setNegativeButton("取消", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        // TODO Auto-generated method stub

                    }
                }).show();
                break;
            case R.id.menu_playmode:
                String[] mode = new String[] { "顺序播放", "单曲循环", "列表循环" };
                AlertDialog.Builder builder = new AlertDialog.Builder(
                        MainMusic.this);
                builder.setTitle("播放模式");
                builder.setSingleChoiceItems(mode, playmode,						//设置单选项，这里第二个参数是默认选择的序号，这里根据playmode的值来确定
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface arg0, int arg1) {
                                // TODO Auto-generated method stub
                                playmode = arg1;
                            }
                        });
                builder.setPositiveButton("确定",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface arg0, int arg1) {
                                // TODO Auto-generated method stub
                                switch (playmode) {
                                    case 0:
                                        playmode = MainMusic.MODE_LIST_SEQUENCE;
                                        Toast.makeText(getApplicationContext(), R.string.sequence, Toast.LENGTH_SHORT).show();
                                        break;
                                    case 1:
                                        playmode = MainMusic.MODE_SINGLE_CYCLE;
                                        Toast.makeText(getApplicationContext(), R.string.singlecycle, Toast.LENGTH_SHORT).show();
                                        break;
                                    case 2:
                                        playmode = MainMusic.MODE_LIST_CYCLE;
                                        Toast.makeText(getApplicationContext(), R.string.listcycle, Toast.LENGTH_SHORT).show();
                                        break;
                                    default:
                                        break;
                                }
                            }
                        });
                builder.create().show();
                break;
            case R.id.menu_sleep:
                showSleepDialog();
                break;
        }
		return super.onOptionsItemSelected(item);
	}
}


