package com.example.ibeacondemo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.AnimationDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.ibeacondemo.Util.BlueToothUtil;
import com.example.ibeacondemo.zxing.android.CaptureActivity;

import java.util.List;

public class ScanActivity extends AppCompatActivity implements View.OnClickListener {
    private Handler mHandler;
    private static final String TAG = "ScanActivity";
    private static final String DECODED_CONTENT_KEY = "codedContent";
    private static final String DECODED_BITMAP_KEY = "codedBitmap";
    private static final int REQUEST_CODE_SCAN = 0x0000;
    private String targetName;
    private boolean found = false;
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBluetoothLeScanner;
    private BluetoothLeAdvertiser mBluetoothLeAdvertiser;

    //是否正在扫描
    private boolean mScanning;
    private TextView tv_scanResult;
    private RelativeLayout loadPart;
    private ImageView imgPgbar;
    private AnimationDrawable ad;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.scan);
        initUI();
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_scan:
                if (ContextCompat.checkSelfPermission(ScanActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(ScanActivity.this, new String[]{Manifest.permission.CAMERA}, 1);
                } else {
                    goScan();
                }
                break;
            case R.id.conTest:
                targetName = tv_scanResult.getText().toString();
                if (targetName.equals("")) {
                    Toast.makeText(this,"请先输入想要接收的广播名",Toast.LENGTH_SHORT).show();
                } else {
                    loadPart.setVisibility(View.VISIBLE);
                    bleScan(targetName);
                }
            default:
                break;
        }

    }

    /**
     * 点击扫描二维码
     */
    private void goScan(){
        Intent intent = new Intent(ScanActivity.this, CaptureActivity.class);
        startActivityForResult(intent, REQUEST_CODE_SCAN);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults){
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    goScan();
                } else {
                    Toast.makeText(this, "你拒绝了权限申请，可能无法打开相机扫码哟！", Toast.LENGTH_SHORT).show();
                }
                break;
            default:
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // 扫描二维码/条码回传
        if (requestCode == REQUEST_CODE_SCAN && resultCode == RESULT_OK) {
            if (data != null) {
                //返回的文本内容
                String content = data.getStringExtra(DECODED_CONTENT_KEY);
                //返回的BitMap图像
                Bitmap bitmap = data.getParcelableExtra(DECODED_BITMAP_KEY);

                tv_scanResult.setText(content);
            }
        }
    }

    /**
     * 开始接收广播
     * @param mac 对应的广播名
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void bleScan(final String mac) {
        init();
        if (!mBluetoothAdapter.isEnabled()) {
            Log.d(TAG, "bleScan: 不支持");
            return;
        }
        if (!mScanning) {
            if (Build.VERSION.SDK_INT >= 21) {
                mScanning = true;
                if (mBluetoothLeScanner == null) {
                    mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
                }
                mBluetoothLeScanner.startScan(null, createScanSettings(), mScanCallback);
            } else {
                mScanning = true;
                mBluetoothAdapter.startLeScan(mLeScanCallback);
            }
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (Build.VERSION.SDK_INT >= 21) {
                        mScanning =false;
                        mBluetoothLeScanner.stopScan(mScanCallback);
                        if (found) {
                            loadPart.setVisibility(View.INVISIBLE);
                            Log.d(TAG, "run: 当前的mac = " + mac);
                            Intent intent = new Intent(ScanActivity.this, MainActivity.class);
                            intent.putExtra("connectMac",mac);
                            startActivity(intent);
                        } else {
                            loadPart.setVisibility(View.INVISIBLE);
                            showDialog();
//                            Toast.makeText(ScanActivity.this,"不跳转", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        mScanning =false;
                        mBluetoothAdapter.stopLeScan(mLeScanCallback);
                        if (found) {
                            loadPart.setVisibility(View.INVISIBLE);
                            Log.d(TAG, "run: 当前的mac = " + mac);
                            Intent intent = new Intent(ScanActivity.this, MainActivity.class);
                            intent.putExtra("connectMac",mac);
                            startActivity(intent);
                        } else {
                            loadPart.setVisibility(View.INVISIBLE);
                            showDialog();
                            Toast.makeText(ScanActivity.this,"为搜索到改设备，请重试", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            },10000);
        }
        Log.d(TAG, "bleScan: 执行完毕");

    }

    @SuppressLint("NewApi")
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void init() {
        mHandler = new Handler();

        if(!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this,"不支持", Toast.LENGTH_LONG).show();
            finish();
        }
        mBluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent,1);
            Toast.makeText(this,"未配对蓝牙", Toast.LENGTH_LONG).show();
            finish();
        }
        mBluetoothLeAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
        if (mBluetoothLeAdvertiser == null ) {
            Toast.makeText(this, "不支持BLE Peripheral", Toast.LENGTH_SHORT).show();
            finish();
        }
        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
    }

    /**
     * 设置扫描设置
     * @return
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private ScanSettings createScanSettings(){
        Log.d(TAG, "createScanSettings: 进入setting");
        ScanSettings.Builder mSettingsBuilder = new ScanSettings.Builder();
        mSettingsBuilder.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY);
        if(Build.VERSION.SDK_INT >= 23) {
            mSettingsBuilder.setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES);
            mSettingsBuilder.setMatchMode(ScanSettings.MATCH_MODE_STICKY);
        }
        if (mBluetoothAdapter.isOffloadedScanBatchingSupported()) {
            mSettingsBuilder.setReportDelay(0L);
        }
        return mSettingsBuilder.build();
    }

    /**
     * 扫描回调
     */
    @SuppressLint("NewApi")
    private ScanCallback mScanCallback = new ScanCallback() {
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            Log.d(TAG, "onScanResult: 正在扫描");
            if (result!=null) {
                if (result.getScanRecord().getBytes() != null) {
                    String raDataStr = BlueToothUtil.bytesToHexString(result.getScanRecord().getBytes());
                    Log.d(TAG, "\nonScanResult:  = " + raDataStr);
                    Log.d(TAG, "\n名字: " + result.getDevice().getName());
                    if (raDataStr.substring(4,16).equals(targetName)) {
                        found = true;
                    }
                }
            }
        }
        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);

        }
        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
        }
    };

    /**
     * 扫描回调（低版本）
     */
    @SuppressLint("NewApi")
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(BluetoothDevice bluetoothDevice, int i, byte[] bytes) {
            if (bluetoothDevice!=null) {
                Log.d(TAG, "onScanResult:Address " + bluetoothDevice.getAddress()+"name = " +bluetoothDevice.getName());
            }
        }
    };


    /**
     * 初始化UI
     */
    private void initUI(){
        //二维码扫描按钮
        Button btn_scan = findViewById(R.id.btn_scan);
        //设备搜索按钮
        Button btn_search = findViewById(R.id.conTest);
        //设置点击事件
        btn_search.setOnClickListener(this);
        btn_scan.setOnClickListener(this);
        //mac显示按钮
        tv_scanResult = findViewById(R.id.tv_scanResult);
        //加载条父组件
        loadPart = findViewById(R.id.loadPart);
        //加载框
        imgPgbar = findViewById(R.id.loading);
        ad = (AnimationDrawable) imgPgbar.getDrawable();
        //设置动画
        imgPgbar.postDelayed(new Runnable() {
            @Override
            public void run() {
                ad.start();
            }
        },100);
    }

    /**
     * 提示框
     */
    private void showDialog(){
        new AlertDialog.Builder(this)
                .setTitle("提示")
                .setMessage("未找到该设备，请滑动正确手势或者重新输入Mac")
                .setPositiveButton("确定",null)
                .show();
    }
}
