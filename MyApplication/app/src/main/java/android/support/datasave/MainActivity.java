package android.support.datasave;

import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private Button mStartDownload;
    private Button mCancelDownLoad;
    private Button mPauseDownLoad;
    private Button mBack;
    private DownloadService.DownLoadBinder mDownLoadBinder;

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mDownLoadBinder = (DownloadService.DownLoadBinder) service;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mStartDownload = (Button) findViewById(R.id.start_download);
        mCancelDownLoad = (Button) findViewById(R.id.cancel_download);
        mPauseDownLoad = (Button) findViewById(R.id.pause_download);
        mBack = (Button) findViewById(R.id.back);
        mBack.setOnClickListener(this);
        mStartDownload.setOnClickListener(this);
        mCancelDownLoad.setOnClickListener(this);
        mPauseDownLoad.setOnClickListener(this);
        Intent intent = new Intent(this,DownloadService.class);
        startService(intent);
        bindService(intent,mConnection,BIND_AUTO_CREATE);
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 1 :
                if(grantResults.length >0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(MainActivity.this,"拒绝权限将无法使用",Toast.LENGTH_LONG).show();
                    finish();
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void onClick(View v) {
        if(mDownLoadBinder == null) {
            return;
        }
        switch (v.getId()) {
            case R.id.cancel_download :
                mDownLoadBinder.cancelDownLoad();
                break;
            case R.id.start_download:
                String URL = "http://dlsw.baidu.com/sw-search-sp/soft/24/13406/XiuXiu_V4.0.1.2002_BdSetup.1437647987.exe";
                mDownLoadBinder.startDownload(URL);
                break;
            case R.id.pause_download:
                mDownLoadBinder.pauseDownload();
                break;
            case R.id.back:
                Intent intent = new Intent(MainActivity.this,LoginActivity.class);
                startActivity(intent);
                finish();
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mConnection);
    }
}