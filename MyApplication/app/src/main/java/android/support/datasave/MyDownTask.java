package android.support.datasave;

import android.os.AsyncTask;
import android.os.Environment;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by pxw on 2018/4/1.
 */

/**
 * AsyncTask属于抽象类，所以使用的时候，必须创建子类去继承，其中三个参数的含义如下
 * params:在执行AsyncTask是需要传入的参数，可用于在后台任务中使用，doInBackground方法的参数类型
 * 在execute() 传入，可变长参数，跟doInBackground(Void… params) 这里的params类型一致
 * progress:后台任务执行的时候，如果需要界面上显示当前的进度，则使用这里指定的泛型作为进度单位
 * ，跟onProgressUpdate(Integer… values) 的values的类型一致
 * result:当任务执行完毕后，如果需要对结果进行返回，则使用这里指定的泛型作为返回值类型
 * 跟doInBackground 返回的参数类型一致，且跟onPostExecute(Boolean s) 的s参数一致，在耗时操作执行完毕调用。
 */
public class MyDownTask extends AsyncTask<String, Integer, Integer> {

    public static final int TYPE_SUCESS = 0;
    public static final int TYPE_FAILED = 1;
    public static final int TYPE_PAUSED = 2;
    public static final int TYPE_CANCELED = 3;

    private DownloadListener mListener;
    private boolean isCanceled = false;
    private boolean isPaused = false;

    private int lastProgress;

    public MyDownTask(DownloadListener listener) {
        mListener = listener;
    }

    //该方法会在后台任务开始执行之前调用，用于进行一些界面上的初始化操作，比如显示一个进度条对话框
    @Override
    protected void onPreExecute() {
        //显示进度对话框
        super.onPreExecute();
    }

    //此方法子子线程中执行，其他方法都是在主线程中执行，执行完成后通过return语句来将任务的执行结果返回
    //在内部调用publishProgress(Progress...)来通知onProgressUpdate的方法的调用
    @Override
    protected Integer doInBackground(String... params) {
        InputStream inputStream = null;
        RandomAccessFile saveFiel = null;
        File file = null;
        try {
            //记录以下载的文件长度
            long downloadLength = 0;
            String downloadUrl = params[0];
            String fileName = downloadUrl.substring(downloadUrl.lastIndexOf("/"));
            String directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
            file = new File(directory, fileName);
            if (file.exists()) {
                //获取当前文件已下载的文件大小
                downloadLength = file.length();
            }
            //根据下载地址获取下载文件的大小
            long contentLength = getContentLength(downloadUrl);
            if (contentLength == 0) {
                return TYPE_FAILED;
            } else if (contentLength == downloadLength) {
                return TYPE_SUCESS;
            }
            OkHttpClient okHttpClient = new OkHttpClient();
            Request request = new Request.Builder()
                    //断点下载 指定从哪个字节开始下载
                    .addHeader("RANGE", "byte=" + downloadLength + "-")
                    .url(downloadUrl)
                    .build();
            Response response = okHttpClient.newCall(request).execute();
            if (response != null) {
                inputStream = response.body().byteStream();
                //获取读写权限
                saveFiel = new RandomAccessFile(file, "rw");
                //跳过已下载的字节
                saveFiel.seek(downloadLength);
                byte[] bytes = new byte[1024];
                int total = 0;
                int len;
                //判断数据已经复制完，完全从网络读取到本地
                while ((len = inputStream.read(bytes)) != -1) {
                    //在读取过程中，判断用户有没取消 或者暂停下载
                    if (isCanceled) {
                        return TYPE_CANCELED;
                    } else if (isPaused) {
                        return TYPE_PAUSED;
                    } else {
                        total += len;
                        saveFiel.write(bytes, 0, len);
                        // 计算已经下载好的百分比
                        int progress = (int) ((total + downloadLength) * 100 / contentLength);
                        publishProgress(progress);
                    }
                }
                response.body().close();
                return TYPE_SUCESS;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
                if(saveFiel != null) {
                    saveFiel.close();
                }
                if(isCanceled && file != null) {
                    file.delete();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return TYPE_FAILED;
    }

    //对外提供改变变量控制方法
    public void pauseDownload(){
        isPaused = true;
    }
    public  void cancelDownload(){
        isCanceled = true;
    }
    //根据url获取长度
    private long getContentLength(String downloadUrl) throws IOException {
        OkHttpClient okHttpClient = new OkHttpClient();
        Request request = new Request.Builder()
                .url(downloadUrl)
                .build();
        Response response = okHttpClient.newCall(request).execute();
        if(response != null && response.isSuccessful()) {
            long contentLength = response.body().contentLength();
            response.body().close();
            return contentLength;
        }
        return 0;
    }

    //在后台调用了publishProgress(Progress...)后，该方法很快就会被调用，其参数就是由publishProgress（）传递过来
    //实现对UI界面的更新
    @Override
    protected void onProgressUpdate(Integer... values) {
        //更新具体的进度
        int progress = values[0];
        if(progress > lastProgress) {
            //回调出去
            mListener.onProgress(progress);
            lastProgress = progress;
        }
    }

    //当后台任务执行完毕，即doInBackground通过rerurn语句进行返回时，该方法很快会被调用
    //可以做提醒任务执行结果的反馈，和关闭对话框等
    @Override
    protected void onPostExecute(Integer status) {
        switch (status) {
            case TYPE_SUCESS :
                mListener.onSucess();
                break;
            case TYPE_FAILED:
                mListener.onFailed();
                break;
            case TYPE_CANCELED :
                mListener.onCanceled();
                break;
            case TYPE_PAUSED :
                mListener.onPaused();
                break;
            default:
                break;
        }
    }
}
