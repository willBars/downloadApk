package android.support.datasave;

/**
 * Created by pxw on 2018/4/1.
 * 下载接口回调,分别是下载进度 成功 失败 暂停 取消
 */

public interface DownloadListener {

    void onProgress(int progress);

    void onSucess();

    void onFailed();

    void onPaused();

    void onCanceled();

}
