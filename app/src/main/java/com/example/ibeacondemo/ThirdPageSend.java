package com.example.ibeacondemo;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.example.ibeacondemo.Bean.DataStructure;
import com.example.ibeacondemo.Bean.ReceiveMessage;
import com.example.ibeacondemo.Bean.SendMessage;
import com.example.ibeacondemo.Util.BaseDispatchTouchActivity;
import com.example.ibeacondemo.Util.BlueToothUtil;

import static com.example.ibeacondemo.SecondPageMain.createAdvSettings;

/**
 * 继承了BaseDispatchTouchActivity，20S不操作，就会跳回第一个界面
 */
public class ThirdPageSend extends BaseDispatchTouchActivity implements View.OnClickListener {


    private Handler mHandler;
    private int curOp;
    private boolean mScanning;
    private int curNum = -1;
    private boolean backResult = false;
    private boolean mAdvertising = false;

    private String targetName;
    private EditText receive_mac;
    private Spinner broadType;
    private Spinner powerChange;
    private EditText reportingFre;
    private EditText powerThreshold;
    private EditText signalThreshold;
    private Button Btn_startSend;
    private Button exit;
    //广播包数据(必须，广播启动就会发送)
    private static AdvertiseData mAdvertiseData;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBluetoothLeScanner;
    private BluetoothLeAdvertiser mBluetoothLeAdvertiser;
    private static final String BROADCAST_SERVICE = "00001802-0000-1000-8000-00805f9b34fb";
    private static final String TAG = "ThirdPageSend";

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.thirdpage);
        initUi();
        initBroad();
    }

    private void initUi() {
        Btn_startSend  = findViewById(R.id.Btn_startSend);
        exit = findViewById(R.id.exit);
        TextView TV_infoDisplay = findViewById(R.id.TV_infoDisplay);
        receive_mac = findViewById(R.id.receive_mac);
        broadType = findViewById(R.id.broadType);
        powerChange = findViewById(R.id.powerChange);
        reportingFre = findViewById(R.id.reportingFre);
        powerThreshold = findViewById(R.id.powerThreshold);
        signalThreshold = findViewById(R.id.signalThreshold);
        Btn_startSend.setOnClickListener(this);
        exit.setOnClickListener(this);

        SpinnerAdapter broadTypeAdapter = new com.example.ibeacondemo.Adapter.SpinnerAdapter(this,R.layout.support_simple_spinner_dropdown_item,getResources().getStringArray(R.array.broadType));
        broadType.setAdapter(broadTypeAdapter);
        SpinnerAdapter powerChangeAdapter = new com.example.ibeacondemo.Adapter.SpinnerAdapter(this,R.layout.support_simple_spinner_dropdown_item,getResources().getStringArray(R.array.powerOn));
        powerChange.setAdapter(powerChangeAdapter);


        Intent intent = getIntent();
        targetName = intent.getStringExtra("mac");
        ReceiveMessage msg = (ReceiveMessage) intent.getSerializableExtra("info");
        Log.d(TAG, "最后的成绩" + msg.toString());
        assert msg != null;
        if (msg.getBroadType().equals("MQTT")) {
            broadType.setSelection(0);
        } else {
            broadType.setSelection(1);
        }
        if (msg.getPowerSet().equals("开机")) {
            powerChange.setSelection(0);
        } else {
            powerChange.setSelection(1);
        }


        TV_infoDisplay.setText("当前版本：" + msg.getEdition() + "\n当前电量：" + msg.getBattery());
        receive_mac.setText(targetName);
        reportingFre.setText(msg.getRepFre());
        powerThreshold.setText(msg.getPowerAlarm());
        signalThreshold.setText(msg.getSignAlarm());




    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onClick(View view) {
        banBTN(false);
        switch (view.getId()) {
            case R.id.Btn_startSend:
                goCfg(1);
                break;
            case R.id.exit:
                goCfg(0XFF);
                break;
            default:
                break;
        }

    }

    /**
     * 初始化蓝牙配置
     */
    @SuppressLint("NewApi")
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void initBroad() {
        mHandler = new Handler();
        //判断是否支持蓝牙
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this,"不支持", Toast.LENGTH_LONG).show();
            finish();
        }

        //蓝牙配置信息
        BluetoothManager mBluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
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

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void goCfg(int type) {

        if (mAdvertising) {
            BlueToothUtil.showDialog(this, "正在配置设备，请稍后");
        } else {
            curOp = type;
            boolean nNull = true;
            SendMessage sendMessage = null;
            targetName = receive_mac.getText().toString();
            switch (type) {
                case 1:
                    if (targetName.equals("") || reportingFre.getText().toString().equals("") || powerThreshold.getText().toString().equals("") || signalThreshold.getText().toString().equals("")){
                        nNull = false;
                    } else {
                        sendMessage = new SendMessage(targetName, ++curNum, type, new DataStructure(broadType.getSelectedItem().toString(), powerChange.getSelectedItem().toString(), reportingFre.getText().toString(), powerThreshold.getText().toString(), signalThreshold.getText().toString()));
                    }
                    break;
                case 0XFF:
                    sendMessage = new SendMessage(targetName, ++ curNum,type,null);
                    break;
                default:
                    break;
            }
            if (nNull && sendMessage != null) {
                if (mAdvertising) {
                    stopAdvertise();
                }
                mBluetoothLeAdvertiser.startAdvertising(createAdvSettings(false,0), createAdvertiseData(sendMessage), mAdvertiseCallback);
                mAdvertising = true;
            } else {
                BlueToothUtil.showDialog(this,"请将配置信息填写完整");
            }

        }
    }


    /**
     * 广播报文设置
     * @return
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private AdvertiseData createAdvertiseData(SendMessage sendMessage) {
        AdvertiseData mAdvertiseData = new AdvertiseData.Builder()
                .addServiceData(ParcelUuid.fromString(BROADCAST_SERVICE), BlueToothUtil.fieldShaping(sendMessage))
                .build();
        return mAdvertiseData;
    }

    /**
     * 关闭广播
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void stopAdvertise() {
        boolean n = mBluetoothLeAdvertiser == null;
        Log.d("TAG", "stopAdvertise: 是否为null" + n);
        Log.d("TAG", "stopAdvertise: " + mBluetoothLeAdvertiser);
        if (mBluetoothLeAdvertiser != null) {
            mAdvertising = false;
            mBluetoothLeAdvertiser.stopAdvertising(mAdvertiseCallback);
            Log.e("TAG", "停止广播");
        }
    }

    /**
     * 开始扫描
     */
    private void bleScan() {
        if (!mBluetoothAdapter.isEnabled()) {
            Log.d(TAG, "bleScan: 不支持");
            return;
        }
        if (!mScanning) {
            mScanning = true;
            if (Build.VERSION.SDK_INT >=21) {

                if (mBluetoothLeScanner == null) {
                    mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
                }
                Log.d(TAG, "bleScan: 多次之前出发1");
                mBluetoothLeScanner.startScan(null,createScanSettings(),mScanCallback);
            }
            Log.d(TAG, "bleScan: " + mHandler);
            mHandler.postDelayed(new Runnable() {
                @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
                @Override
                public void run() {
                    Log.d(TAG, "run: 官博");
                    stopScan();
                    if (!backResult) {
                        mAdvertising = false;
                        banBTN(true);
                        BlueToothUtil.showDialog(ThirdPageSend.this,"配置失败，请重试");
                    }
                }
            }, 15000);
        } else {
            BlueToothUtil.showDialog(ThirdPageSend.this,"手机正在配置,请勿操作");
        }
    }

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

    /**
     * 扫描设置
     * @return
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private   ScanSettings createScanSettings() {
        Log.d(TAG, "createScanSettings: 进入setting");
        ScanSettings.Builder mSettingsbulider = new ScanSettings.Builder();
        mSettingsbulider.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY);
        if (Build.VERSION.SDK_INT >= 23) {
            mSettingsbulider.setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES);
            mSettingsbulider.setMatchMode(ScanSettings.MATCH_MODE_STICKY);
        }
        if (mBluetoothAdapter.isOffloadedScanBatchingSupported()) {
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
            if (result!=null) {
                Log.d(TAG, "欻的raData" + result);
                if (result.getScanRecord().getBytes() != null && result.getScanRecord().getBytes().length > 38) {
                    parseMsg(result.getScanRecord().getBytes());
                }
            }
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void parseMsg(byte[] returnData) {
        String returnDataStr = BlueToothUtil.bytesToHexString(returnData);
        Log.d(TAG, "\nonScanResult:  = " + returnDataStr);
        //配置设备
//        returnDataStr = "0D 09 303030313032303330343035 04 16 00 01 00";
        //退出配置
//        returnDataStr = "0D 09 303030313032303330343035 04 16 00 FF 00";
        if (BlueToothUtil.initTest(returnDataStr, targetName)) {
            try{
                ReceiveMessage receiveMessage = new ReceiveMessage(returnDataStr);
                if (receiveMessage.getExeResult() == 0 ) {
                    Log.d(TAG, "parseMsg: getExeResult" + receiveMessage.getExeResult());
                    exeMsg(receiveMessage);
                }
            } catch (Exception e) {
                Toast.makeText(ThirdPageSend.this,e.toString(),Toast.LENGTH_SHORT).show();

            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void exeMsg(ReceiveMessage receiveMessage) {
        if (BlueToothUtil.messageCorrect(receiveMessage,curNum,targetName,curOp)) {
            switch (curOp) {
                case 0X01:
                    stopScan();
                    banBTN(true);
                    BlueToothUtil.showDialog(ThirdPageSend.this,"配置成功!");
                    Log.d(TAG, "exeMsg: 配置成功");
                    break;
                case 0XFF:
                    banBTN(true);
                    stopScan();
                    new AlertDialog.Builder(this)
                            .setTitle("提示")
                            .setMessage("设备已退出配置模式")
                            .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
//                                    Intent intent = new Intent(ThirdPageSend.this, FirstPageCode.class);
//                                    startActivity(intent);
                                    finish();
//                                    onDestroy();

                                }
                            })
                            .show();
                    break;
            }
            backResult = true;
        } else {
//            BlueToothUtil.showDialog(this,"未搜索到相对应的设备，请重试");
            backResult = false;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void stopScan() {
        mScanning = false;
        if (Build.VERSION.SDK_INT >= 21) {
            Log.d(TAG, "stopScan: 大于21停止");
            stopAdvertise();
            mBluetoothLeScanner.stopScan(mScanCallback);
//            isEnd = true;
            Log.d(TAG, "stopScan: 执行结束");
        }
    }

    private void banBTN(boolean b) {
        broadType.setEnabled(b);
        powerChange.setEnabled(b);
        reportingFre.setEnabled(b);
        powerThreshold.setEnabled(b);
        signalThreshold .setEnabled(b);
        Btn_startSend.setEnabled(b);
        exit.setEnabled(b);
    }
}
