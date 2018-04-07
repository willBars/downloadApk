package android.support.datasave;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import java.io.File;

public class DownloadService extends Service {
    private MyDownTask mMyDownTask;
    private String downloadUrl;
    public DownloadService() {
    }
    private DownloadListener mListener = new DownloadListener() {
        @Override
        public void onProgress(int progress) {
            getNoticationManager().notify(1,getNotication("正在下载中...",progress));
        }

        @Override
        public void onSucess() {
            mMyDownTask = null;
            //下载成功时将前台服务通知关闭，并创建一个成功的通知,startForeground让服务处于前台
            stopForeground(true);
            getNoticationManager().notify(1,getNotication("下载成功",-1));
            Toast.makeText(DownloadService.this,"下载成功",Toast.LENGTH_LONG).show();
        }

        @Override
        public void onFailed() {
            mMyDownTask = null;
            //下载成功时将前台服务通知关闭，并创建一个成功的通知,startForeground让服务处于前台
            stopForeground(true);
            getNoticationManager().notify(1,getNotication("下载失败",-1));
            Toast.makeText(DownloadService.this,"下载失败",Toast.LENGTH_LONG).show();
        }

        @Override
        public void onPaused() {
            mMyDownTask = null;
            Toast.makeText(DownloadService.this,"暂停下载",Toast.LENGTH_LONG).show();
        }

        @Override
        public void onCanceled() {
            mMyDownTask = null;
            //下载成功时将前台服务通知关闭，并创建一个成功的通知,startForeground让服务处于前台
            stopForeground(true);
            Toast.makeText(DownloadService.this,"取消下载",Toast.LENGTH_LONG).show();
        }
    };

    //在notification中获取具体的下载信息, 并点开延迟的界面
    private Notification getNotication(String title, int progress) {
        Intent intent = new Intent(this,MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setSmallIcon(R.mipmap.ic_launcher_round);
        builder.setLargeIcon(BitmapFactory.decodeResource(getResources(),R.mipmap.ic_launcher));
        builder.setContentIntent(pendingIntent);
        builder.setContentTitle(title);
        if(progress >= 0) {
            //当progress大于或者等于0时，才需要显示下载进度
            builder.setContentText(progress+"%");
            builder.setProgress(100,progress,false);
        }
        return builder.build();
    }

    private NotificationManager getNoticationManager(){
        return (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    }
    private DownLoadBinder mDownLoadBinder = new DownLoadBinder();
    class DownLoadBinder extends Binder{
        public void startDownload(String URL){
            if(mMyDownTask == null) {
                downloadUrl = URL;
                mMyDownTask = new MyDownTask(mListener);
                mMyDownTask.execute(downloadUrl);
                //开启前台服务,使系统状态栏能够有个持续运行的通知
                startForeground(1,getNotication("downloading....",0));
                Toast.makeText(DownloadService.this,"downloading....",Toast.LENGTH_LONG).show();
            }
        }
        public void pauseDownload(){
            if(mMyDownTask != null) {
                mMyDownTask.pauseDownload();
            }
        }
        public void cancelDownLoad(){
            if(mMyDownTask != null) {
                mMyDownTask.cancelDownload();
            }
            if(downloadUrl != null) {
                //取消下载的时候需要将文件删除，并将通知关闭
                String fileName = downloadUrl.substring(downloadUrl.lastIndexOf("/"));
                String directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
                File file = new File(directory+fileName);
                if(file.exists()) {
                    file.delete();
                }
                getNoticationManager().cancel(1);
                stopForeground(true);
                Toast.makeText(DownloadService.this,"CancelDownload",Toast.LENGTH_LONG).show();
            }
        }
    }
    @Override
    public IBinder onBind(Intent intent) {
       return new DownLoadBinder();
    }
}
