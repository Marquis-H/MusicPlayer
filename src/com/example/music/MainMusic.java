package com.example.music;

import java.util.*;

import android.view.KeyEvent;
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
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.SimpleAdapter;

public class MainMusic extends Activity {
    private ImageButton Btn_Previous;
    private ImageButton Btn_PlayOrPause;
    private ImageButton Btn_Stop;
    private ImageButton Btn_Next;
    private ListView musiclist;
    private TextView text_Current;
    private TextView text_Duration;
    private SeekBar seekBar;
    //���½�����
    private Handler seekBarHandler;
    //���ڸ����ĳ���ʱ��͵�ǰλ��
    private int duration;
    private int time;
    private String musicName;
    private String musicArtist;
    private RelativeLayout root_Layout;
    //���������Ƴ���
    private static final int PROGRESS_INCREASE=0;
    private static final int PROGRESS_PAUSE=1;
    private static final int PROGRESS_RESET=2;
    //����ģʽ����
    private static final int MODE_LIST_SEQUENCE=0;
    private static final int MODE_SINGLE_CYCLE=1;
    private static final int MODE_LIST_CYCLE=2;
    private int playmode;
    // ��ǰ��������ţ��±��1��ʼ
 	private int number;
    // Menu����
 	public static final int MENU_THEME = Menu.FIRST;
 	public static final int MENU_ABOUT = Menu.FIRST + 1;
 	 
 	//�㲥������
 	private StatusChangedReceiver receiver;
    //�����б����
    private ArrayList<Music> musicArrayList;
    //�˳��жϱ��
    private static Boolean isExit = false;
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
        //Ĭ�ϲ���ģʽ��˳��ģʽ
        playmode = MainMusic.MODE_LIST_SEQUENCE;
    }
    
   //�󶨹㲥������
    private void bindStatusChangedReceiver(){
    	receiver = new StatusChangedReceiver();
    	IntentFilter filter = new IntentFilter(MusicService.BROADCAST_MUSICSERVICE_UPDATE_STATUS);
    	registerReceiver(receiver, filter);
    }
    
   //��ȡ��ʾ���ע���id
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
    
    //Ϊÿ�����ע�������
    private void registerListeners(){
    	//��һ��
    	Btn_Previous.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                sendBroadcastOnCommand(MusicService.COMMAND_PREVIOUS);
            }
        });
    	//����&��ͣ
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
    	//ֹͣ
    	Btn_Stop.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                sendBroadcastOnCommand(MusicService.COMMAND_STOP);
            }
        });
    	//��һ��
    	Btn_Next.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                sendBroadcastOnCommand(MusicService.COMMAND_NEXT);
            }
        });
    	musiclist.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                // position�±��0��ʼ��number�±��1��ʼ
                number = position;
                sendBroadcastOnCommand(MusicService.COMMAND_PLAY);
            }
        });
    	seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                //���㲥��musicservice��ִ��������ת
                sendBroadcastOnCommand(MusicService.COMMAND_SEEK_TO);
                if (isPlaying()) {
                    //�������ָ��ƶ�
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

	
	 //musicservice����״̬�ж�
    private int status;
    
    //�Ƿ����ڲ�������
    private boolean isPlaying(){
    	return status == MusicService.STATUS_PLAYING;
    }
    
    //�Ƿ���ͣ��������
    private boolean isPaused(){
    	return status == MusicService.STATUS_PAUSED;
    }
    
    //�Ƿ�ֹͣ��������
    private boolean isStop(){
    	return status == MusicService.STATUS_STOPPED;
    }
    
	//musicservice������״̬���½��չ㲥
	class StatusChangedReceiver extends BroadcastReceiver{
		public void onReceive(Context context,Intent intent){
			//��ȡmusicservice������״̬
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
	
	//����UI
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

			MainMusic.this.setTitle("���ڲ��ţ�" + musicName + " - "+musicArtist);
			break;
		case MusicService.STATUS_PAUSED:
			seekBarHandler.sendEmptyMessage(PROGRESS_PAUSE);
            String string = MainMusic.this.getTitle().toString().replace("���ڲ���:", "����ͣ:");
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
		// ����Activity������
		setTheme(theme);
    		
    }
	@Override
    protected void onDestroy(){
    	if(isStop()){
    		stopService(new Intent(this,MusicService.class));
    	}
    	super.onDestroy();
    }
    
    //��ʼ�������б�
    private void initMusicList(){
        musicArrayList = MusicList.getMusicList();
        //�����ظ��������
        if(musicArrayList.isEmpty())
        {
            Cursor mMusicCursor = this.getContentResolver().query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, null, null,
                    MediaStore.Audio.AudioColumns.TITLE);
            //����
            int indexTitle = mMusicCursor.getColumnIndex(MediaStore.Audio.AudioColumns.TITLE);
            //������
            int indexArtist = mMusicCursor.getColumnIndex(MediaStore.Audio.AudioColumns.ARTIST);
            //��ʱ��
            int indexTotalTime = mMusicCursor.getColumnIndex(MediaStore.Audio.AudioColumns.DURATION);
            //·��
            int indexPath = mMusicCursor.getColumnIndex(MediaStore.Audio.AudioColumns.DATA);

            /**ͨ��mMusicCursor�α�������ݿ⣬����Music�������ش�ArrayList��*/
            for (mMusicCursor.moveToFirst(); !mMusicCursor.isAfterLast(); mMusicCursor
                    .moveToNext()) {
                String strTitle = mMusicCursor.getString(indexTitle);
                String strArtist = mMusicCursor.getString(indexArtist);
                String strTotoalTime = mMusicCursor.getString(indexTotalTime);
                String strPath = mMusicCursor.getString(indexPath);

                if (strArtist.equals("<unknown>"))
                    strArtist = "��������";
                Music music = new Music(strTitle, strArtist, strPath, strTotoalTime);
                musicArrayList.add(music);
            }
        }
    }
    
    //�����б�����

    /**��������������ʼ��listView*/
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
    //��ʽ�������� ->"mm:ss" 
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
	
	//���ƽ�����
	private void initSeekBarHandler(){
		seekBarHandler = new Handler(){
			public void handleMessage(Message msg){
				super.handleMessage(msg);
				switch(msg.what){
				case PROGRESS_INCREASE:
					if(seekBar.getProgress()<duration){
						//������ǰ��1s
						seekBar.incrementProgressBy(1000);
						seekBarHandler.sendEmptyMessageDelayed(
								PROGRESS_INCREASE,1000);
						//�޸���ʾ���ȵ�ʱ��
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

    //���������musicservice���������ֲ���,������musicservice��
    private void sendBroadcastOnCommand(int command){
    	Intent intent = new Intent(MusicService.BROADCAST_MUSICSERVICE_CONTROL);
		intent.putExtra("command", command);
    	//����ָ��Ĳ�ͬ����װ��ͬ������
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
            Toast.makeText(this, "�ٰ�һ���˳���", Toast.LENGTH_SHORT).show();
            timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    isExit = false;
                }
            }, 2000);
        }
        else
        {
            System.exit(0);
        }


    }

    //��дonKeyDown����
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        switch (keyCode)
        {
            case KeyEvent.KEYCODE_BACK:
                exitByDoubleClick();
                break;
        }
        return false;
    }
    
    private void setTheme(String theme) {
		if ("ҹ��".equals(theme)) {
			root_Layout.setBackgroundResource(R.drawable.bg_night);
		}  else if ("��ɫ".equals(theme)) {
			root_Layout.setBackgroundResource(R.drawable.bg_blue);
		} else if ("����".equals(theme)) {
			root_Layout.setBackgroundResource(R.drawable.bg_line);
		} else if ("���".equals(theme)) {
			root_Layout.setBackgroundResource(R.drawable.bg_sky);
		}
	}
    // �����˵�
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_music, menu);
        return super.onCreateOptionsMenu(menu);
	}

	// ����˵�����¼� 
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_theme:
			// ��ʾ�б�Ի���
			new AlertDialog.Builder(this).setTitle("��ѡ������").setItems(R.array.theme,
					new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {
									// ��ȡ��array.xml�ж������������
									String theme = PropertyBean.THEMES[which];
									// ����Activity������
									setTheme(theme);
									// ����ѡ�������
									PropertyBean property = new PropertyBean(
											MainMusic.this);
									property.setAndSaveTheme(theme);
								}
							}).show();
			break;
		case R.id.menu_about:
			// ��ʾ�ı��Ի���
			new AlertDialog.Builder(MainMusic.this).setTitle("Music")
					.setMessage(MainMusic.this.getString(R.string.about)).show();
			break;
            case R.id.menu_quit:
                //�˳�����
                new AlertDialog.Builder(MainMusic.this).setTitle("��ʾ")
                        .setMessage(R.string.quit_message).setPositiveButton("ȷ��", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        // TODO Auto-generated method stub
                        System.exit(0);
                    }
                }).setNegativeButton("ȡ��", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        // TODO Auto-generated method stub

                    }
                }).show();
                break;
            case R.id.menu_playmode:
                String[] mode = new String[] { "˳�򲥷�", "����ѭ��", "�б�ѭ��" };
                AlertDialog.Builder builder = new AlertDialog.Builder(
                        MainMusic.this);
                builder.setTitle("����ģʽ");
                builder.setSingleChoiceItems(mode, playmode,						//���õ�ѡ�����ڶ���������Ĭ��ѡ�����ţ��������playmode��ֵ��ȷ��
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface arg0, int arg1) {
                                // TODO Auto-generated method stub
                                playmode = arg1;
                            }
                        });
                builder.setPositiveButton("ȷ��",
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
        }
		return super.onOptionsItemSelected(item);
	}
}


