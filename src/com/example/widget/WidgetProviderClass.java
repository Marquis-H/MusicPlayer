package com.example.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import com.example.data.Music;
import com.example.music.MainMusic;
import com.example.music.MusicService;
import com.example.music.R;

/**
 * Created by Marquis on 2014/10/30.
 */
public class WidgetProviderClass extends AppWidgetProvider {

    //定义广播标示常量
    public static final String BROADCAST_MUSICSERVICE_CONTROL = "MusicService.ACTION_CONTROL";
    public static final String BROADCAST_MUSICSERVICE_UPDATE_STATUS = "MusicService.ACTION_UPDATE";

    //请求码，根据不同的请求码发送不同的广播
    public static final int RequstCode_StartActivity = 0;
    public static final int RequstCode_Play = 1;
    public static final int RequstCode_Pause = 2;
    public static final int RequstCode_Next = 3;
    public static final int RequstCode_Previous = 4;

    //播放状态
    private int status;
    private RemoteViews remoteViews = null;
    private String musicName = null;
    private String musicArtist = null;





    //widget删除时使用
    public void onDeleted(Context context, int[] appWidgetIds){
        super.onDeleted(context, appWidgetIds);
    }

    //最后一个widget被删除时调用
    public void onDisabled(Context context){
        super.onDisabled(context);
    }

    //第一个widget被创建时调用
    public void onEnabled(Context context){
        super.onEnabled(context);
    }

    //接受广播的回调函数
    public void onReceive(Context context, Intent intent){
        super.onReceive(context, intent);

        if (intent.getAction().equals(BROADCAST_MUSICSERVICE_UPDATE_STATUS))
        {
            status = intent.getIntExtra("status", -1);
            remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
            switch (status){
                case MusicService.STATUS_PLAYING:
                    //获取歌曲名，艺术家名
                    musicArtist = intent.getStringExtra("musicName");
                    musicName = intent.getStringExtra("musicArtist");

                    //修改标题及按钮图片
                    remoteViews.setTextViewText(R.id.widget_title, musicName+ " " +musicArtist);
                    remoteViews.setImageViewResource(R.id.widget_play, R.drawable.button_pause);

                    //播放状态时，点击播放、暂停按钮，发送带暂停指令的广播
                    Intent intent_pause = new Intent(BROADCAST_MUSICSERVICE_CONTROL);
                    intent_pause.putExtra("command", MusicService.COMMAND_PAUSE);
                    PendingIntent pendingIntent_pause = PendingIntent.getBroadcast(context,RequstCode_Pause,intent_pause,PendingIntent.FLAG_UPDATE_CURRENT);
                    remoteViews.setOnClickPendingIntent(R.id.widget_play,pendingIntent_pause);
                    break;
                case MusicService.STATUS_PAUSED:
                    remoteViews.setImageViewResource(R.id.widget_play,R.drawable.button_play);

                    //暂停状态时，点击播放、暂停按钮，发送带播放指令的广播
                    Intent intent_play = new Intent(BROADCAST_MUSICSERVICE_CONTROL);
                    intent_play.putExtra("command", MusicService.COMMAND_RESUME);
                    PendingIntent pendingIntent_play = PendingIntent.getBroadcast(context,RequstCode_Play,intent_play,PendingIntent.FLAG_UPDATE_CURRENT);
                    remoteViews.setOnClickPendingIntent(R.id.widget_play,pendingIntent_play);
                    break;
                case MusicService.STATUS_STOPPED:
                    remoteViews.setImageViewResource(R.id.widget_play,R.drawable.button_play);
                    remoteViews.setTextViewText(R.id.widget_title,"Music");
                    break;
                default:
                    break;
            }
            //将该界面显示在插件中，更新状态
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            ComponentName componentName = new ComponentName(context,WidgetProviderClass.class);
            appWidgetManager.updateAppWidget(componentName, remoteViews);
        }
    }

    //在更新widge时，该方法被调用
    public void onUpdate(Context context, AppWidgetManager appWidgetManager,int[] appWidgetIds){
        super.onUpdate(context,appWidgetManager,appWidgetIds);

        remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_layout);

        //发送广播，检测状态
        Intent intent = new Intent(BROADCAST_MUSICSERVICE_CONTROL);
        intent.putExtra("command", MusicService.COMMAND_CHECK_IS_PLAYING);
        context.sendBroadcast(intent);

        //标题
        Intent intent_title = new Intent();
        intent_title.setClass(context, MainMusic.class);
        PendingIntent pendingIntent_title = PendingIntent.getActivity(context, RequstCode_StartActivity,intent_title,PendingIntent.FLAG_UPDATE_CURRENT);

        //下一首，点击时发送带下一首指令的广播
        Intent intent_next = new Intent(BROADCAST_MUSICSERVICE_CONTROL);
        intent_next.putExtra("command", MusicService.COMMAND_NEXT);
        PendingIntent pendingIntent_next = PendingIntent.getBroadcast(context, RequstCode_Next,intent_next,PendingIntent.FLAG_UPDATE_CURRENT);

        //上一首，点击时发送带上一首指令的广播
        Intent intent_pre = new Intent(BROADCAST_MUSICSERVICE_CONTROL);
        intent_pre.putExtra("command", MusicService.COMMAND_PREVIOUS);
        PendingIntent pendingIntent_pre = PendingIntent.getBroadcast(context, RequstCode_Previous,intent_pre,PendingIntent.FLAG_UPDATE_CURRENT);

        remoteViews.setOnClickPendingIntent(R.id.widget_title,pendingIntent_title);
        remoteViews.setOnClickPendingIntent(R.id.widget_presong,pendingIntent_pre);
        remoteViews.setOnClickPendingIntent(R.id.widget_next,pendingIntent_next);

        appWidgetManager.updateAppWidget(appWidgetIds, remoteViews);

    }
}
