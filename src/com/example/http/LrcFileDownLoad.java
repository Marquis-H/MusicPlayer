package com.example.http;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URLEncoder;

/**
 * Created by Marquis on 2014/10/28.
 */
public class LrcFileDownLoad {
    public static final String LRC_SEARCH_URL = "http://geci.me/api/lyric/";
    private static final int REQUEST_TIMEOUT = 15*1000;   //设置请求超时15s
    private static final int SO_TIMEOUT = 15*1000;       //设置等待数据超时时间15s

    public static String getLrcSearchUrl(String path,String soneName)
        throws Exception{
        String url = null;
        String str_json = null;

        //歌词名为空返回null
        if (soneName == null)
        {
            return null;
        }
        String name = URLEncoder.encode(soneName,"utf-8");   //编码转换
        str_json = getHtmlCode(path+name);

        //超时以及其他异常时返回null
        if (str_json == null) return null;

        JSONObject jsonObject = new JSONObject(str_json);
        int count = jsonObject.getInt("count");

        //如果count为0表示没有歌词
        if (count == 0) return null;

        //获取得到是歌词的url列表，这里要去第一个url为歌词下载
        JSONArray jsonArray = jsonObject.getJSONArray("result");
        JSONObject item = jsonArray.getJSONObject(0);

        url = item.getString("lrc");
        return url;
    }
    //该方法用于设置超时
    public static HttpClient getHttpClient(){
        BasicHttpParams httpParams = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(httpParams,REQUEST_TIMEOUT);
        HttpConnectionParams.setSoTimeout(httpParams,SO_TIMEOUT);
        HttpClient client = new DefaultHttpClient(httpParams);
        return client;
    }

    //获取网页源码
    public static String getHtmlCode(String path){
        String result = null;
        try {
            HttpClient httpClient = getHttpClient();
            HttpGet get = new HttpGet(path);
            HttpResponse response =  httpClient.execute(get);

            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK){
                HttpEntity entity = response.getEntity();
                BufferedReader br = new BufferedReader(new InputStreamReader(entity.getContent(),"utf-8"));
                String line;
                result = "";
                while ((line = br.readLine())!= null){
                    result += line + "\n";
                }
            }
        }
        catch (ConnectTimeoutException e)
        {
            System.out.println("ConnectTimeoutException timeout");
            return null;
        }
        catch (SocketTimeoutException e)
        {
            System.out.println("SocketTimeoutException timeout");
            return null;
        }
        catch (Exception e)
        {
            System.out.println(e.getMessage() + ":" + e.toString());
            return null;
        }
        return result;

    }
}
