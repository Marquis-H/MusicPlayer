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

    //����㲥��ʾ����
    public static final String BROADCAST_MUSICSERVICE_CONTROL = "MusicService.ACTION_CONTROL";
    public static final String BROADCAST_MUSICSERVICE_UPDATE_STATUS = "MusicService.ACTION_UPDATE";

    //�����룬���ݲ�ͬ�������뷢�Ͳ�ͬ�Ĺ㲥
    public static final int RequstCode_StartActivity = 0;
    public static final int RequstCode_Play = 1;
    public static final int RequstCode_Pause = 2;
    public static final int RequstCode_Next = 3;
    public static final int RequstCode_Previous = 4;

    //����״̬
    private int status;
    private RemoteViews remoteViews = null;
    private String musicName = null;
    private String musicArtist = null;





    //widgetɾ��ʱʹ��
    public void onDeleted(Context context, int[] appWidgetIds){
        super.onDeleted(context, appWidgetIds);
    }

    //���һ��widget��ɾ��ʱ����
    public void onDisabled(Context context){
        super.onDisabled(context);
    }

    //��һ��widget������ʱ����
    public void onEnabled(Context context){
        super.onEnabled(context);
    }

    //���ܹ㲥�Ļص�����
    public void onReceive(Context context, Intent intent){
        super.onReceive(context, intent);

        if (intent.getAction().equals(BROADCAST_MUSICSERVICE_UPDATE_STATUS))
        {
            status = intent.getIntExtra("status", -1);
            remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
            switch (status){
                case MusicService.STATUS_PLAYING:
                    //��ȡ����������������
                    musicArtist = intent.getStringExtra("musicName");
                    musicName = intent.getStringExtra("musicArtist");

                    //�޸ı��⼰��ťͼƬ
                    remoteViews.setTextViewText(R.id.widget_title, musicName+ " " +musicArtist);
                    remoteViews.setImageViewResource(R.id.widget_play, R.drawable.button_pause);

                    //����״̬ʱ��������š���ͣ��ť�����ʹ���ָͣ��Ĺ㲥
                    Intent intent_pause = new Intent(BROADCAST_MUSICSERVICE_CONTROL);
                    intent_pause.putExtra("command", MusicService.COMMAND_PAUSE);
                    PendingIntent pendingIntent_pause = PendingIntent.getBroadcast(context,RequstCode_Pause,intent_pause,PendingIntent.FLAG_UPDATE_CURRENT);
                    remoteViews.setOnClickPendingIntent(R.id.widget_play,pendingIntent_pause);
                    break;
                case MusicService.STATUS_PAUSED:
                    remoteViews.setImageViewResource(R.id.widget_play,R.drawable.button_play);

                    //��ͣ״̬ʱ��������š���ͣ��ť�����ʹ�����ָ��Ĺ㲥
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
            //���ý�����ʾ�ڲ���У�����״̬
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            ComponentName componentName = new ComponentName(context,WidgetProviderClass.class);
            appWidgetManager.updateAppWidget(componentName, remoteViews);
        }
    }

    //�ڸ���widgeʱ���÷���������
    public void onUpdate(Context context, AppWidgetManager appWidgetManager,int[] appWidgetIds){
        super.onUpdate(context,appWidgetManager,appWidgetIds);

        remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_layout);

        //���͹㲥�����״̬
        Intent intent = new Intent(BROADCAST_MUSICSERVICE_CONTROL);
        intent.putExtra("command", MusicService.COMMAND_CHECK_IS_PLAYING);
        context.sendBroadcast(intent);

        //����
        Intent intent_title = new Intent();
        intent_title.setClass(context, MainMusic.class);
        PendingIntent pendingIntent_title = PendingIntent.getActivity(context, RequstCode_StartActivity,intent_title,PendingIntent.FLAG_UPDATE_CURRENT);

        //��һ�ף����ʱ���ʹ���һ��ָ��Ĺ㲥
        Intent intent_next = new Intent(BROADCAST_MUSICSERVICE_CONTROL);
        intent_next.putExtra("command", MusicService.COMMAND_NEXT);
        PendingIntent pendingIntent_next = PendingIntent.getBroadcast(context, RequstCode_Next,intent_next,PendingIntent.FLAG_UPDATE_CURRENT);

        //��һ�ף����ʱ���ʹ���һ��ָ��Ĺ㲥
        Intent intent_pre = new Intent(BROADCAST_MUSICSERVICE_CONTROL);
        intent_pre.putExtra("command", MusicService.COMMAND_PREVIOUS);
        PendingIntent pendingIntent_pre = PendingIntent.getBroadcast(context, RequstCode_Previous,intent_pre,PendingIntent.FLAG_UPDATE_CURRENT);

        remoteViews.setOnClickPendingIntent(R.id.widget_title,pendingIntent_title);
        remoteViews.setOnClickPendingIntent(R.id.widget_presong,pendingIntent_pre);
        remoteViews.setOnClickPendingIntent(R.id.widget_next,pendingIntent_next);

        appWidgetManager.updateAppWidget(appWidgetIds, remoteViews);

    }
}
