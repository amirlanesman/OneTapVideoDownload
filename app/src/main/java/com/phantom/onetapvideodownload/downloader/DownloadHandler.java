package com.phantom.onetapvideodownload.downloader;

import android.app.NotificationManager;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import com.phantom.onetapvideodownload.R;
import com.phantom.onetapvideodownload.downloader.downloadinfo.DownloadInfo;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DownloadHandler {
    private Context mContext;
    private DownloadInfo mDownloadInfo;
    private final static String TAG = "DownloadHandler";
    private NotificationCompat.Builder mBuilder;
    private NotificationManager mNotifyManager;
    private final static AtomicInteger mNotificationId = new AtomicInteger(150);

    DownloadHandler(Context context, DownloadInfo downloadInfo) {
        mContext = context;
        mDownloadInfo = downloadInfo;
    }

    public void startDownload() {
        File filePath = new File(mDownloadInfo.getDownloadLocation());
        downloadFile(mDownloadInfo.getUrl(), filePath);
        mDownloadInfo.setStatus(DownloadInfo.Status.Downloading);
        mNotifyManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        mBuilder = new NotificationCompat.Builder(mContext);
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager manager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = manager.getActiveNetworkInfo();
        boolean isAvailable = false;
        if (networkInfo != null && networkInfo.isConnected()) {
            isAvailable = true;
        }
        return isAvailable;
    }

    private void downloadFile(String url, final File file) {
        if (isNetworkAvailable()) {
            OkHttpClient Client = new OkHttpClient();
            Request request = new Request.Builder().url(url).build();
            Call call = Client.newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    mDownloadInfo.setStatus(DownloadInfo.Status.NetworkProblem);
                }

                @Override
                public void onResponse(Call call, Response response) {
                    try {
                        InputStream in = response.body().byteStream();
                        mDownloadInfo.setContentLength(response.body().contentLength());
                        if (response.isSuccessful()) {
                            Log.v(TAG, file.getAbsolutePath());
                            BufferedOutputStream bw = new BufferedOutputStream(new FileOutputStream(file));
                            byte data[] = new byte[1024 * 4];
                            int count;
                            while ((count = in.read(data)) != -1) {
                                bw.write(data, 0, count);
                                mDownloadInfo.addDownloadedLength(count);
                            }
                            bw.close();
                            in.close();
                            mDownloadInfo.setStatus(DownloadInfo.Status.Completed);
                            showNotification();
                        } else {
                            mDownloadInfo.setStatus(DownloadInfo.Status.WriteFailed);
                        }
                    } catch (IOException e) {
                        Log.e("DownloadService", "expection is ", e);
                    }
                }
            });
        } else {
            mDownloadInfo.setStatus(DownloadInfo.Status.NetworkNotAvailable);
        }
    }

    public Integer getProgress() {
        return mDownloadInfo.getProgress();
    }

    public DownloadInfo.Status getStatus() {
        return mDownloadInfo.getStatus();
    }

    public long getContentLength() {
        return mDownloadInfo.getContentLength();
    }

    public String getFilename() {
        return mDownloadInfo.getFilename();
    }

    public String getUrl() {
        return mDownloadInfo.getUrl();
    }

    private void showNotification() {
        mBuilder.setSmallIcon(R.drawable.download);
        mBuilder.setLargeIcon(BitmapFactory.decodeResource(mContext.getResources(), R.mipmap.ic_launcher));
        mBuilder.setContentTitle(getFilename());
        mBuilder.setContentText(getNotificationContent());
        mBuilder.setAutoCancel(false);
        mBuilder.setOnlyAlertOnce(false);
        mNotifyManager.notify(mNotificationId.getAndIncrement(), mBuilder.build());
    }

    private String getNotificationContent() {
        return "Download Finished";
    }
}