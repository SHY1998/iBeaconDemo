package com.example.ibeacondemo.UselessActivity;

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
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.example.ibeacondemo.Bean.ReceiveMessage;
import com.example.ibeacondemo.Bean.SendMessage;
import com.example.ibeacondemo.R;
import com.example.ibeacondemo.Util.BlueToothUtil;

import java.util.List;
import java.util.Objects;

public class ReadActivity extends AppCompatActivity implements View.OnClickListener {


    //页面组件
    TextView receiveMac;
    Button btn_getConfig;
    Button btn_getEdition;
    Button btn_getBattery;
    EditText status;
    EditText tready;
    EditText freq;
    EditText powerThreshold;
    EditText signalThreshold;
    EditText Edition;
    EditText Battery;
    LinearLayout configFather;
    LinearLayout editionFather;
    LinearLayout batterFather;

    private Handler mHandler;
    //是否再扫描
    private boolean mScanning = false;
    //是否在广播
    private boolean mBroading = false;
    //对接mac
    private String targetName;
    //当前操作类型
    private int curOp;
    //执行结果
    private boolean backResult = false;
    //当前数据编号
    private int curNum=-1;
    private boolean communicating = false;
    private static final String TAG = "ReadActivity";
    //蓝牙广播器
    private BluetoothLeAdvertiser mBluetoothLeAdvertiser;
    //蓝牙扫描器
    private BluetoothLeScanner mbLeScanner;
    //蓝牙管理类
    private BluetoothManager bluetoothManager;
    private BluetoothLeScanner mBluetoothLeScanner;
    //蓝牙适配器
    private static BluetoothAdapter bluetoothAdapter;
    //广播包数据(必须，广播启动就会发送)
    private static AdvertiseData mAdvertiseData;
    private static final String BROADCAST_SERVICE = "00001802-0000-1000-8000-00805f9b34fb";

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.readconfig);
        init();
        bleInit();
    }

    private void init() {
        Intent intent1 = getIntent();
        targetName = intent1.getStringExtra("connectMac");

        receiveMac = findViewById(R.id.receive_mac);
        btn_getBattery = findViewById(R.id.btn_getBattery);
        btn_getConfig = findViewById(R.id.btn_getConfig);
        btn_getEdition = findViewById(R.id.btn_getEdition);
        status = findViewById(R.id.status);
        tready = findViewById(R.id.treaty);
        freq = findViewById(R.id.freq);
        powerThreshold = findViewById(R.id.powerThreshold);
        signalThreshold = findViewById(R.id.signalThreshold);
        Edition = findViewById(R.id.Edition);
        Battery = findViewById(R.id.Battery);
        configFather = findViewById(R.id.configFather);
        editionFather = findViewById(R.id.editionFather);
        batterFather = findViewById(R.id.batterFather);

        receiveMac.setText(targetName);
        btn_getEdition.setOnClickListener(this);
        btn_getConfig.setOnClickListener(this);
        btn_getBattery.setOnClickListener(this);

    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_getConfig:
                startProcess(2);
                break;
            case R.id.btn_getEdition:
                startProcess(3);
                break;
            case R.id.btn_getBattery:
                startProcess(4);
                break;
            default:
                break;
        }
    }
    @SuppressLint("NewApi")
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void bleInit() {
        mHandler = new Handler();
        bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        mBluetoothLeAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
        mBluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
    }

    /**
     * 关闭广播的回调
     */
    @SuppressLint("NewApi")
    private AdvertiseCallback closeAdvertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
        }

        @Override
        public void onStartFailure(int errorCode) {
            super.onStartFailure(errorCode);
        }
    };

    @SuppressLint("NewApi")
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

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void createAdvertiseData(){
        SendMessage sendMessage = new SendMessage(targetName,++curNum,curOp,null);
        mAdvertiseData = new AdvertiseData.Builder()
                .addServiceData(ParcelUuid.fromString(BROADCAST_SERVICE),BlueToothUtil.fieldShaping(sendMessage))
                .build();

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
     * 开始扫描
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void bleScan() {
        if (!bluetoothAdapter.isEnabled()) {
            Log.d(TAG, "bleScan: 不支持");
            return;
        }
        if (!mScanning) {
            mScanning = true;
            if (Build.VERSION.SDK_INT >=21) {
                Log.d(TAG, "bleScan: 大于21启动");
                if (mbLeScanner == null) {
                    mbLeScanner = bluetoothAdapter.getBluetoothLeScanner();
                }
                mbLeScanner.startScan(null,createScanSettings(),mScanCallback);
            } else {
                Log.d(TAG, "bleScan: 小于21启动");
                bluetoothAdapter.startLeScan(mLeScanCallback);
            }
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                   Log.d(TAG, "run: 官博");
                   stopScan();
                   if (!backResult) {
                       communicating = false;
                       BlueToothUtil.showDialog(ReadActivity.this,"请求失败，请重试");
                   }
                }
            }, 10000);
        } else {
            BlueToothUtil.showDialog(ReadActivity.this,"手机正在扫描,请勿重复启动");
        }
    }

    /**
     * 扫描设置
     * @return
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
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
    @SuppressLint("NewApi")
    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            Log.d(TAG, "多次出发");
            //如果报文不为空
            if (result!=null) {
                if (result.getScanRecord().getBytes() != null) {
                    //解析报文
                    parseMsg(result.getScanRecord().getBytes());
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
            if (errorCode == SCAN_FAILED_INTERNAL_ERROR) {
                BlueToothUtil.showDialog(ReadActivity.this, "SCAN_FAILED_INTERNAL_ERROR");
                Log.e(TAG, "数据大于31个字节");
            } else if (errorCode == SCAN_FAILED_APPLICATION_REGISTRATION_FAILED) {
                BlueToothUtil.showDialog(ReadActivity.this, "SCAN_FAILED_APPLICATION_REGISTRATION_FAILED");
                Log.e(TAG, "数据大于31个字节");
            } else if (errorCode == SCAN_FAILED_FEATURE_UNSUPPORTED) {
                BlueToothUtil.showDialog(ReadActivity.this, "SCAN_FAILED_FEATURE_UNSUPPORTED");
                Log.e(TAG, "正在连接的，无法再次连接");
            } else if (errorCode == SCAN_FAILED_INTERNAL_ERROR) {
                BlueToothUtil.showDialog(ReadActivity.this, "SCAN_FAILED_INTERNAL_ERROR");
                Log.e(TAG, "由于内部错误操作失败");
            }
        }
    };


    @SuppressLint("NewApi")
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(BluetoothDevice bluetoothDevice, int i, byte[] bytes) {
            if (bluetoothDevice!=null) {
                Log.d(TAG, "onScanResult:Address " + bluetoothDevice.getAddress()+"name = " +bluetoothDevice.getName());
            }
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void changeUi(ReceiveMessage receiveMessage) {
        switch (receiveMessage.getComType()) {
            //参数报文
            case 2:
                editionFather.setVisibility(View.GONE);
                batterFather.setVisibility(View.GONE);
                configFather.setVisibility(View.VISIBLE);
                status.setText(Objects.requireNonNull(receiveMessage).getPowerSet());
                tready.setText(receiveMessage.getBroadType());
                freq.setText(receiveMessage.getRepFre());
                powerThreshold.setText(receiveMessage.getPowerAlarm());
                signalThreshold.setText(receiveMessage.getSignAlarm());
                break;
            //版本号报文
            case 3:
                editionFather.setVisibility(View.VISIBLE);
                batterFather.setVisibility(View.GONE);
                configFather.setVisibility(View.GONE);
                assert receiveMessage != null;
                Edition.setText(receiveMessage.getEdition());
                break;
            //电池电量报文
            case 4:
                editionFather.setVisibility(View.GONE);
                batterFather.setVisibility(View.VISIBLE);
                configFather.setVisibility(View.GONE);
                assert receiveMessage != null;
                Battery.setText(receiveMessage.getBattery());
                break;
            default:
                break;
        }
    }

    @SuppressLint("NewApi")
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void exeMsg(ReceiveMessage receiveMessage) {
        //如果mac、数据编号、操作类型都符合
        if (BlueToothUtil.messageCorrect(receiveMessage,curNum,targetName,curOp)) {
            Log.d(TAG, "exeMsg: 相同");
            changeUi(receiveMessage);
            //标定正确（使扫描结束后判断是否出现提示框）
            backResult = true;
            //改变后停止扫描
            stopScan();

        } else {
            Log.d(TAG, "exeMsg: 不同");
            backResult = false;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void parseMsg(byte[] returnData) {
        String returnDataStr = BlueToothUtil.bytesToHexString(returnData);
        Log.d(TAG, "\nonScanResult:  = " + returnDataStr);
        //配置参数
//        returnDataStr = "0D09303030313032303330343035 0A160002000101001F1F1F";
        //版本号
//        returnDataStr = "0D0930303031303230333034303509160003000431303130";
        //电量
//        returnDataStr = "0D093030303130323033303430350616000400001F";
        if (BlueToothUtil.initTest(returnDataStr,targetName)) {
            try {
                ReceiveMessage receiveMessage = new ReceiveMessage(returnDataStr);
                Log.d(TAG, "模拟: " + receiveMessage.toString());
                if (receiveMessage.getExeResult() == 0) {
                    exeMsg(receiveMessage);
                }
            } catch (Exception e) {
                Log.d(TAG, "Exception: " + e);
                Toast.makeText(ReadActivity.this,e.toString(),Toast.LENGTH_SHORT).show();
            }

        }


    }

    /**
     * 停止扫描
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void stopScan(){
            Log.d(TAG, "stopScan: 执行停止扫描");
            mScanning = false;
            if (Build.VERSION.SDK_INT >= 21) {
                Log.d(TAG, "stopScan: 大于21停止");
                mbLeScanner.stopScan(mScanCallback);
                Log.d(TAG, "stopScan: 执行结束");
                stopAdvertise();
            } else {
                Log.d(TAG, "stopScan: 小于21停止");
                bluetoothAdapter.stopLeScan(mLeScanCallback);
            }
    }

    /**
     * 停止广播
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void stopAdvertise() {
        boolean n = mBluetoothLeAdvertiser == null;
        Log.d(TAG, "stopAdvertise: 是否为null" + n);
        Log.d(TAG, "stopAdvertise: " + mBluetoothLeAdvertiser);
        if (mBluetoothLeAdvertiser != null) {
            communicating = false;
            mBroading =false;
            mBluetoothLeAdvertiser.stopAdvertising(mAdvertiseCallback);
            Log.e(TAG, "停止广播");
        }
    }

    /**
     * 开始点击按钮后的流程
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void startProcess(int Op) {
        if (communicating) {
            BlueToothUtil.showDialog(this,"正在查询数据，请稍后");
        } else {
            if (mBroading) {
                stopAdvertise();
            }
            curOp = Op;
            createAdvertiseData();
            curNum = curNum % 0XFF;
            //交流锁
            communicating = true;
            mBluetoothLeAdvertiser.startAdvertising(createAdvSettings(false,0), mAdvertiseData,mAdvertiseCallback);
        }
    }
}
