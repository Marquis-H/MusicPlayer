package com.example.music;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.example.data.Music;
import com.example.http.LrcFileDownLoad;

import java.io.*;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Marquis on 2014/10/28.
 */
public class LrcActivity extends Activity{
    private TextView lrc_nr;
    private int status;

    //广播接收器
    private StatusChangedReceiver receiver;
    private LinearLayout linearLayout;

    private String lrc_data = null;
    private String filename = null;
    private String musicName = null;
    private String musicArtist = null;

    private AsyncDownLoad asyncDownLoad;
    @Override
    protected void onCreate(Bundle savedInstanceState){

        super.onCreate(savedInstanceState);
        setContentView(R.layout.lrc);

        lrc_nr = (TextView)findViewById(R.id.lrc_tv_nr);
        linearLayout = (LinearLayout)findViewById(R.id.lrc);

        bindStatusChangedReceiver();
        lrc_nr.setText("当前没有播放的歌曲！");
        sendBroadcastOnCommand(MusicService.COMMAND_CHECK_IS_PLAYING);
    }

    //绑定广播接收器
    private void bindStatusChangedReceiver(){
        receiver = new StatusChangedReceiver();
        IntentFilter filter = new IntentFilter(MusicService.BROADCAST_MUSICSERVICE_UPDATE_STATUS);
        registerReceiver(receiver,filter);
    }
    private void sendBroadcastOnCommand(int command){
        Intent intent = new Intent(MusicService.BROADCAST_MUSICSERVICE_CONTROL);
        intent.putExtra("command",command);
        sendBroadcast(intent);
    }

    //内部类，指处理播放状态的广播
    class StatusChangedReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            //获取播放状态
            status = intent.getIntExtra("status", -1);
            switch (status) {
                case MusicService.STATUS_PLAYING:
                    musicArtist = intent.getStringExtra("musicArtist");
                    musicName = intent.getStringExtra("musicName");
                    LrcActivity.this.setTitle("歌词：" + musicName + "-" + musicArtist);
                    //asyncDownLoad = new AsyncDownLoad();
                    // asyncDownLoad.execute(LrcFileDownLoad.LRC_SEARCH_URL,musicName);
                    try {
                        get_lrc(LrcActivity.this);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;

            }
        }
    }

    class AsyncDownLoad extends AsyncTask<String,Integer,String>
    {
        //执行时调用该方法
        protected String doInBackground(String... arg0){
            String url = null;
            try {
                url = LrcFileDownLoad.getLrcSearchUrl(arg0[0], arg0[1]);
                lrc_data = LrcFileDownLoad.getHtmlCode(url);
            }catch (Exception e){
                System.out.println(e.getMessage());
            }
            return lrc_data;
        }
        //任务执行前执行该方法
        protected void onPreExecute(){
            super.onPreExecute();
            lrc_nr.setText("歌词搜索中...");
        }
        //任务结束时执行该方法
        protected void onPostExecute(String result){
            super.onPostExecute(result);
            if (result != null) {
                try {
                    //写入文件
                    FileOutputStream outputStream = LrcActivity.this.openFileOutput(filename, Context.MODE_PRIVATE);
                    outputStream.write(result.getBytes());
                    outputStream.close();

                } catch (FileNotFoundException e){
                    e.printStackTrace();
                }catch (IOException e){
                    e.printStackTrace();
                }
                String string = drawLrcWord(filename);
                lrc_nr.setText(string);
            } else{
                lrc_nr.setText("没有找到歌词！");
            }
        }
    }

    private void get_lrc(Context context) throws IOException{
        String[] files = context.fileList();  //本程序内部储存空间的文件列表
        if (musicName != null){
            filename = musicName + ".lrc";
            List<String> fileList = Arrays.asList(files);
            if (fileList.contains(filename)){
                Log.d("TAG","lrc file");
                String string = drawLrcWord(filename);
                lrc_nr.setText(string);
            }else {
                //判断网络环境
                ConnectivityManager cwjManager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo info = cwjManager.getActiveNetworkInfo();
                //网络可用则进行下载
                if (info != null && info.isAvailable()){
                    Log.d("TAG","lrc not file");
                    asyncDownLoad = new AsyncDownLoad();
                    asyncDownLoad.execute(LrcFileDownLoad.LRC_SEARCH_URL,musicName);
                }else {
                    Toast.makeText(getApplicationContext(),"当前网络不给力喔，请检测网络配置！",Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    private String drawLrcWord(String filename){
        String lrc_word = "";
        Pattern pattern = Pattern.compile("\\[\\d{2}:\\d{2}.\\d{2}\\]");
        try {
            File file = new File(getApplicationContext().getFilesDir() + "/" + filename);
            System.out.println(file.getPath());
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line = "";
            while ((line = reader.readLine()) != null){
                Matcher m = pattern.matcher(line);
                line = m.replaceAll("");
                line = line.replace("[ti:", "");
                line = line.replace("[ar:", "");
                line = line.replace("[al:", "");
                line = line.replace("[by:", "");
                line = line.replace("[i:", "");
                line = line.replace("]", "");
                line = line.contains("offset")?"":line;
                line = line.replace("url", "歌词来源");
                line = line.replace("null", "");
                lrc_word +=line+"\n";
            }
            return lrc_word;
        }catch (IOException e){
            e.printStackTrace();
        }
        return null;
    }
    @Override
    protected void onDestroy(){
        super.onDestroy();
        unregisterReceiver(receiver);
        if (asyncDownLoad != null && !asyncDownLoad.isCancelled())
            asyncDownLoad.cancel(true);
    }

}
