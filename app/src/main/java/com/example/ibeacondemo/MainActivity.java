package com.example.ibeacondemo;

import androidx.annotation.RequiresApi;

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
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.example.ibeacondemo.Bean.DataStructure;
import com.example.ibeacondemo.Bean.ReceiveMessage;
import com.example.ibeacondemo.Bean.SendMessage;
import com.example.ibeacondemo.Util.BaseDispatchTouchActivity;
import com.example.ibeacondemo.Util.BlueToothUtil;

import java.util.List;

@SuppressLint("NewApi")
public class MainActivity  extends BaseDispatchTouchActivity implements View.OnClickListener{


    //当前报文数据编号
    private int curNum = -1;
    private Handler mHandler;
    //目标设备名
    private String targetName = "";
    //扫描指示参数
    private boolean mScanning;
    //广播指示参数
    private boolean mAdvertising = false;
    //是否成功
    private boolean backResult = false;
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
    //协议类型
    Spinner broadType;
    int curOp;


    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initUi(this);
        init();
    }

    /**
     * 设备初始化
     */
    @SuppressLint("NewApi")
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void init()  {
        //消息处理器
        mHandler = new Handler();
        //获取上一个页面传过来的mac
        Intent intent1 = getIntent();
        targetName = intent1.getStringExtra("connectMac");
        receive_mac.setText(targetName);
        bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        mBluetoothLeAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
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
                    stopScan();
                    if (!backResult) {
                        mAdvertising = false;
                        BlueToothUtil.showDialog(MainActivity.this,"配置失败，请重试");
                    }
                }
            }, 10000);
        } else {
            BlueToothUtil.showDialog(MainActivity.this,"手机正在配置,请勿操作");
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

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public  void createAdvertiseData(SendMessage sendMessage) {
        //广播包设置
        mAdvertiseData =new AdvertiseData.Builder()
                .addServiceData(ParcelUuid.fromString(BROADCAST_SERVICE),BlueToothUtil.fieldShaping(sendMessage))
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
            mAdvertising =false;
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
        //下拉框设置
        spinner = findViewById(R.id.powerChange);
        //协议类型
        broadType = findViewById(R.id.broadType);
        //设备名编辑框
        receive_mac = findViewById(R.id.receive_mac);
        //电量阈值
        powerThreshold = findViewById(R.id.powerThreshold);
        //上报频率
        reportingFrequency = findViewById(R.id.reportingFre);
        //移动信号阈值
        signalThreshold = findViewById(R.id.signalThreshold);
        //配置设备按钮
        Button btn_startSend = findViewById(R.id.Btn_startSend);
        //查询配置按钮
        Button btn_getInfo = findViewById(R.id.Btn_getInfo);
        //退出配置
        Button btn_exit = findViewById(R.id.exit);
        //下发点击事件
        btn_exit.setOnClickListener(this);
        btn_startSend.setOnClickListener(this);
        btn_getInfo.setOnClickListener(this);
        //配置报文类型
        SpinnerAdapter adapter = new com.example.ibeacondemo.Adapter.SpinnerAdapter(this,R.layout.support_simple_spinner_dropdown_item,getResources().getStringArray(R.array.broadType));
        broadType.setAdapter(adapter);
        //配置开关机
        SpinnerAdapter arrayAdapter = new com.example.ibeacondemo.Adapter.SpinnerAdapter(this,R.layout.support_simple_spinner_dropdown_item,getResources().getStringArray(R.array.powerOn));
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
                    parseMsg(result.getScanRecord().getBytes());
                }
            }
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

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            //发送广播（配置设备）
            case R.id.Btn_startSend:
                goCfg(1);
                break;
            case R.id.Btn_getInfo:
                jumpToRead();
                break;
            case R.id.exit:
                goCfg(0XFF);
            default:
                break;
        }
    }

    /**
     * 开启配置
     */
    private void goCfg(int type) {
        if (mAdvertising) {
            BlueToothUtil.showDialog(this,"正在配置设备，如需操作，请关闭广播后重新操作");
        } else {
            curOp = type;
            SendMessage sendMessage;
            //获取对接设备Mac
            targetName = receive_mac.getText().toString();
            //频率
            String freq;
            String power;
            //信号阈值
            String sign;
            //开关机
            String status;
            //广播类型
            String bType;
            //不为空
            boolean nNull = true;
            switch (type) {
                case 1:
                    freq = reportingFrequency.getText().toString();
                    power = powerThreshold.getText().toString();
                    sign = signalThreshold.getText().toString();
                    status = spinner.getSelectedItem().toString();
                    bType = broadType.getSelectedItem().toString();
                    if (targetName.equals("") || freq.equals("") || power.equals("") || sign.equals("")) {
                        nNull = false;
                    }
                    sendMessage = new SendMessage(targetName,++curNum,type,new DataStructure(bType,status,freq,power,sign));
                    break;
                case 0XFF:
                    sendMessage =  new SendMessage(targetName,++curNum,type,null);
                    break;
                default:
                    sendMessage = null;
                    break;
            }
            if (nNull && sendMessage != null) {
                if (mAdvertising) {
                    stopAdvertise();
                }
                createAdvertiseData(sendMessage);
                mBluetoothLeAdvertiser.startAdvertising(createAdvSettings(false,0), mAdvertiseData, mAdvertiseCallback);
                mAdvertising = true;
            } else {
                Log.d(TAG, "goCfg: nNull" + nNull);
                Log.d(TAG, "goCfg: sendMessage" + sendMessage);
                BlueToothUtil.showDialog(this,"请将配置信息填写完整");
            }
        }
    }

    /**
     * 跳转页面
     */
    private void jumpToRead() {
        if (mAdvertising) {
            BlueToothUtil.showDialog(this,"正在配置，请稍后");
        } else {
            Intent intent = new Intent(this,ReadActivity.class);
            intent.putExtra("connectMac",targetName);
            startActivity(intent);
        }
    }

    private void parseMsg(byte[] returnData) {
        String returnDataStr = BlueToothUtil.bytesToHexString(returnData);
        Log.d(TAG, "\nonScanResult:  = " + returnDataStr);
        ReceiveMessage receiveMessage = new ReceiveMessage(returnDataStr);
        if (receiveMessage.getExeResult() == 0) {
            Log.d(TAG, "parseMsg: getExeResult" + receiveMessage.getExeResult());
            exeMsg(receiveMessage);
        }
    }

    private void exeMsg(ReceiveMessage receiveMessage) {
        if (BlueToothUtil.messageCorrect(receiveMessage,curNum,targetName,curOp)) {
            switch (curOp) {
                case 0X01:
                    BlueToothUtil.showDialog(this,"配置成功!");
                    break;
                case 0XFF:
                    BlueToothUtil.showDialog(this,"设备已退出配置模式！");
            }

            backResult = true;
            stopScan();
        } else {
            backResult = false;
        }
    }

    private void stopScan() {
        mScanning = false;
        if (Build.VERSION.SDK_INT >= 21) {
            Log.d(TAG, "stopScan: 大于21停止");
            mbLeScanner.stopScan(mScanCallback);
            Log.d(TAG, "stopScan: 执行结束");
            stopAdvertise();
        } else {
            Log.d(TAG, "stopScan: 小于21停止");
            bluetoothAdapter.stopLeScan(null);
        }
    }
}