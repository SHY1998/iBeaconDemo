package com.example.ibeacondemo;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;
import com.example.ibeacondemo.Util.BlueToothUtil;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@SuppressLint("NewApi")
public class MainActivity extends AppCompatActivity {


    private Handler mHandler;
    private String[] info ;
    //目标设备名
    private String targetName = "";
    //扫描指示参数
    private boolean mScanning;
    //广播指示参数
    private boolean mAdvertising;
    //蓝牙广播器
    private BluetoothLeAdvertiser mBluetoothLeAdvertiser;
    //蓝牙扫描器
    private BluetoothLeScanner mbLeScanner ;
    //蓝牙管理类
    private BluetoothManager bluetoothManager;
    private BluetoothLeScanner mBluetoothLeScanner;
    //广播包数据(必须，广播启动就会发送)
    private static AdvertiseData mAdvertiseData;
    //扫描响应数据(可选，当客户端扫描时才发送)
    private static AdvertiseData mScanResponseData;
    private static final String TAG = "MainActivity";
    //服务UUID
    private static final String SCAN_SERVICE = "0000180d-0000-1000-8000-00805f9b34fb";
    private static final String BROADCAST_SERVICE = "00001802-0000-1000-8000-00805f9b34fb";
    private static final boolean SET_INFO = true;
    private static final boolean GET_INFO = false;
    //蓝牙适配器
    private static BluetoothAdapter bluetoothAdapter;
    //接收到的报文
    private TextView receiveMessage;
    //设备名编辑框
    EditText receive_mac ;
    //上报频率
    EditText reportingFrequency;
    //电量阈值
    EditText powerThreshold;
    //移动信号阈值
    EditText signalThreshold;
    //开关机下拉框
    Spinner spinner;


    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        info = getResources().getStringArray(R.array.infoIndex);
        init();
        initUi(this);
    }

    /**
     * 设备初始化
     */
    @SuppressLint("NewApi")
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void init()  {
        mHandler = new Handler();
        if (!getPackageManager().hasSystemFeature (PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "不支持", Toast.LENGTH_LONG).show();
            finish();
        }
        bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent,1);
            Toast.makeText(this,"未配对蓝牙",Toast.LENGTH_SHORT).show();
            finish();
        }
        mBluetoothLeAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
        if (mBluetoothLeAdvertiser == null ) {
            Toast.makeText(this, "不支持BLE Peripheral", Toast.LENGTH_SHORT).show();
            finish();
        }
        mBluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
    }

    /**
     * 开始扫描
     */
    private void bleScan() {
        if (!bluetoothAdapter.isEnabled()) {
            Log.d(TAG, "bleScan: 不支持");
            return;
        }
        if (!mScanning) {
            if (Build.VERSION.SDK_INT >=21) {
                mScanning = true;
                if (mbLeScanner == null) {
                    mbLeScanner = bluetoothAdapter.getBluetoothLeScanner();
                }
                mbLeScanner.startScan(null,createScanSettings(),mScanCallback);
            } else {
                  mScanning = true;
                  bluetoothAdapter.startLeScan(mLeScanCallback);
            }
            Log.d(TAG, "bleScan: " + mHandler);
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "run: 官博");
                    if (Build.VERSION.SDK_INT >= 21) {
                        mScanning = false;
                        mbLeScanner.stopScan(mScanCallback);
                    } else {
                          mScanning = false;
                          bluetoothAdapter.stopLeScan(mLeScanCallback);
                    }
                }
            }, 10000);
        } else {
              Log.d(TAG, "bleScan: 是false");
              Toast.makeText(this,"手机正在扫描,请勿重复启动",Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 广播设置
     * @param connectable 是否可以连接
     * @param timeoutMillis 广播时长
     * @return
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public static AdvertiseSettings createAdvSettings(boolean connectable, int timeoutMillis) {
        //初始化广播设置
        AdvertiseSettings.Builder mSettingsbuilder = new AdvertiseSettings.Builder();
        //设置广播模式，以控制广播的功率和延迟。 ADVERTISE_MODE_LOW_LATENCY为高功率，低延迟
        mSettingsbuilder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED);
        //设置广告类型是可连接还是不可连接。
        mSettingsbuilder.setConnectable(connectable);
        //广播时限。最多180000毫秒。值为0将禁用时间限制。（不设置则为无限广播时长）
        mSettingsbuilder.setTimeout(timeoutMillis);
        //设置蓝牙广播发射功率级别
        mSettingsbuilder.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH);

        //初始化广播设置
        AdvertiseSettings mAdvertiseSettings = mSettingsbuilder.build();

        //如果广播设置不为
        if (mAdvertiseSettings == null) {
            Log.e(TAG, "mAdvertiseSettings == null");
        }
        return mAdvertiseSettings;
    }


    /**
     *
     * @param freq
     * @param power
     * @param sign
     * @param status
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public  void createAdvertiseData(String freq, String power, String sign, String status, boolean actionType, String mac) {
        //广播包设置
        mAdvertiseData =new AdvertiseData.Builder()
                .addServiceData(ParcelUuid.fromString(BROADCAST_SERVICE),BlueToothUtil.fieldShaping(freq, power, sign, status, actionType, mac))
                //广播包主要存储设备名字
//                .setIncludeDeviceName(true)
                .build();
        //扫描包设置
        mScanResponseData = new AdvertiseData.Builder()
                //扫描包主要存储数据信息
//                .addServiceData(ParcelUuid.fromString(HEART_RATE_SERVICE),text.getBytes())
                .build();
        //如果广播包为空,则提示错误
        if (mAdvertiseData == null) {
            Log.e(TAG, "mAdvertiseSettings == null");
        }
    }

    private void stopAdvertise() {
        if (mBluetoothLeAdvertiser != null) {
            mBluetoothLeAdvertiser.stopAdvertising(mAdvertiseCallback);
            //停止广播
//            mBluetoothLeAdvertiser = null;
            Log.e(TAG,"停止广播");
        }
    }

    /**
     * 初始化Ui
     * @param context
     */
    @SuppressLint("ClickableViewAccessibility")
    private void initUi(final Context context) {
        //布局信息
        final LinearLayout linearLayout = findViewById(R.id.LinearLayout);
        //发送广播按钮
        Button btn_startSend = findViewById(R.id.Btn_startSend);
        //停止广播按钮
        Button btn_stopSend = findViewById(R.id.Btn_stopSend);
        //开始接收按钮
        Button btn_startReceive = findViewById(R.id.Btn_startReceive);
        //下拉框设置
        spinner = findViewById(R.id.powerChange);
        SpinnerAdapter arrayAdapter = new com.example.ibeacondemo.Adapter.SpinnerAdapter(this,R.layout.support_simple_spinner_dropdown_item,getResources().getStringArray(R.array.powerOn));
        spinner.setAdapter(arrayAdapter);
        //报文显示文本框
        receiveMessage = findViewById(R.id.receiveMessage);
        //设备名编辑框
        receive_mac = findViewById(R.id.receive_mac);
        //上报频率
        reportingFrequency = findViewById(R.id.reportingFre);
        //电量阈值
        powerThreshold = findViewById(R.id.powerThreshold);
        //移动信号阈值
        signalThreshold = findViewById(R.id.signalThreshold);
        spinner.setAdapter(arrayAdapter);
        //当点击空白时，使其他所有组件失去焦点
        linearLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                linearLayout.setFocusable(true);
                linearLayout.setFocusableInTouchMode(true);
                linearLayout.requestFocus();
                return false;
            }
        });

        //停止广播事件
        btn_stopSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //判断是否在广播
                if (mAdvertising) {
                    //如果在广播,则关闭广播
                    stopAdvertise();
                    Toast.makeText(context,"停止广播",Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context,"手机处于非广播状态",Toast.LENGTH_SHORT).show();
                }
            }
        });

        //开始接收设备广播事件
        btn_startReceive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                targetName = receive_mac.getText().toString();
                if (targetName.equals("")) {
                    Toast.makeText(context,"请先输入想要接收的广播名",Toast.LENGTH_SHORT).show();
                } else {
                    bleScan();
                }
            }
        });

//        //停止广播事件
//        btn_stopReceive.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                if (Build.VERSION.SDK_INT >= 21) {
//                    mScanning = false;
//                    mbLeScanner.stopScan(mScanCallback);
//                } else {
//                    mScanning = false;
//                    bluetoothAdapter.stopLeScan(mLeScanCallback);
//                }
//            }
//        });
    }

    /**
     * 扫描设置
     * @return
     */
    private  static ScanSettings createScanSettings() {
        Log.d(TAG, "createScanSettings: 进入setting");
        ScanSettings.Builder mSettingsbulider = new ScanSettings.Builder();
        mSettingsbulider.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY);
        if (Build.VERSION.SDK_INT >= 23) {
            mSettingsbulider.setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES);
            mSettingsbulider.setMatchMode(ScanSettings.MATCH_MODE_STICKY);
        }
        if (bluetoothAdapter.isOffloadedScanBatchingSupported()) {
            mSettingsbulider.setReportDelay(0L);
        }
        return mSettingsbulider.build();
    }

    /**
     * 扫描回调
     */
    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            if (result!=null) {
                if (result.getScanRecord().getBytes() != null) {
                    String raDataStr = BlueToothUtil.bytesToHexString(result.getScanRecord().getBytes());
                    Log.d(TAG, "\nonScanResult:  = " + raDataStr);
                    Log.d(TAG, "\n名字: " + result.getDevice().getName());
                    if (result.getDevice().getName() == null) {
                        Log.d(TAG, " 为空");
                    }
                    if (result.getDevice().getName()!=null && result.getDevice().getName().equals(targetName) ) {
                        receiveMessage.setText(String.valueOf(raDataStr));
                        if (Build.VERSION.SDK_INT >= 21) {
                            mScanning = false;
                            mbLeScanner.stopScan(mScanCallback);
                            Log.d(TAG, "调用暂停");
                        } else {
                            mScanning = false;
                            bluetoothAdapter.stopLeScan(mLeScanCallback);
                        }
                        stopAdvertise();
//                        mBluetoothLeAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
//                        Log.d(TAG, "onScanResult: " + (mBluetoothLeAdvertiser == null));
//                        createAdvertiseData("0","0","0","0",);
//                        mBluetoothLeAdvertiser.startAdvertising(createAdvSettings(false,0),mAdvertiseData,mScanResponseData,mAdvertiseCallback);
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

    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(BluetoothDevice bluetoothDevice, int i, byte[] bytes) {
            if (bluetoothDevice!=null) {
                Log.d(TAG, "onScanResult:Address " + bluetoothDevice.getAddress()+"name = " +bluetoothDevice.getName());
            }
        }
    };

    private AdvertiseCallback callback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
        }
    };

    private AdvertiseCallback mAdvertiseCallback = new AdvertiseCallback() {
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        //成功
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
            if (settingsInEffect != null) {
                bleScan();
                Log.d(TAG, "onStartSuccess TxPowerLv=" + settingsInEffect.getTxPowerLevel() + " mode=" + settingsInEffect.getMode()
                        + " timeout=" + settingsInEffect.getTimeout());
            } else {
                Log.e(TAG, "onStartSuccess, settingInEffect is null");
            }
            Log.e(TAG, "onStartSuccess settingsInEffect" + settingsInEffect);
        }

        //失败
        @Override
        public void onStartFailure(int errorCode) {
            super.onStartFailure(errorCode);
            Log.e(TAG, "onStartFailure errorCode" + errorCode);//返回的错误码
            if (errorCode == ADVERTISE_FAILED_DATA_TOO_LARGE) {
                Log.e(TAG, "数据大于31个字节");
            } else if (errorCode == ADVERTISE_FAILED_TOO_MANY_ADVERTISERS) {
                Log.e(TAG, "未能开始广播，没有广播实例");
            } else if (errorCode == ADVERTISE_FAILED_ALREADY_STARTED) {
                Log.e(TAG, "正在连接的，无法再次连接");
            } else if (errorCode == ADVERTISE_FAILED_INTERNAL_ERROR) {
                Log.e(TAG, "由于内部错误操作失败");
            } else if (errorCode == ADVERTISE_FAILED_FEATURE_UNSUPPORTED) {
                Log.e(TAG, "不支持此功能");
            }
        }
    };

    public void startBroadCast(View view) {
        targetName = receive_mac.getText().toString();
        String freq = reportingFrequency.getText().toString();
        String power = powerThreshold.getText().toString();
        String sign = signalThreshold.getText().toString();
        String status = spinner.getSelectedItem().toString();
        if (!targetName.equals("") && !freq.equals("") && !power.equals("") && !sign.equals("")) {
            stopAdvertise();
            Toast.makeText(this,"开始广播",Toast.LENGTH_SHORT).show();
            String text = freq+power+sign;
            createAdvertiseData(freq,power,sign,status,SET_INFO,targetName);
            Log.d(TAG, "检测NUll" + mAdvertiseData+"结果");
            Log.d(TAG, "检测NUll" + mScanResponseData+"结果");
            Log.d(TAG, "检测null" + mBluetoothLeAdvertiser+"结果");
            mBluetoothLeAdvertiser.startAdvertising(createAdvSettings(false,0),mAdvertiseData,mScanResponseData,mAdvertiseCallback);
            //设置为正在广播
            mAdvertising = true;
        } else {
            Log.d(TAG, "target =" + targetName +"feq =" + freq + "power =" + power + "sign =" + sign);
            Toast.makeText(this,"请将配置信息填写完整",Toast.LENGTH_LONG).show();
        }
    }

    public void getInfo(View view) {

        //获取对接mac
        targetName = receive_mac.getText().toString();
        if (!targetName.equals("")) {
            stopAdvertise();
            Toast.makeText(this,"正在查询配置",Toast.LENGTH_SHORT).show();
            createAdvertiseData("","","","",GET_INFO,targetName);
            mBluetoothLeAdvertiser.startAdvertising(createAdvSettings(false,0),mAdvertiseData,mScanResponseData,mAdvertiseCallback);
            mAdvertising = true;
        } else {
            Toast.makeText(this,"请填写要查询的设备mac",Toast.LENGTH_LONG).show();
        }
    }
}