package com.example.ibeacondemo;

import android.annotation.SuppressLint;
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
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import com.example.ibeacondemo.Bean.ReceiveMessage;
import com.example.ibeacondemo.Bean.SendMessage;
import com.example.ibeacondemo.Util.BaseDispatchTouchActivity;
import com.example.ibeacondemo.Util.BlueToothUtil;
import com.example.ibeacondemo.View.TasksCompletedView;


/**
 * 继承了BaseDispatchTouchActivity，20S不操作，就会跳回第一个界面
 */
public class SecondPageMain extends BaseDispatchTouchActivity implements View.OnClickListener {
    //操作类型
    private static final int initOp = -1;
    private static final int find = 0X00;
    private static final int cfgGet = 0X02;
    private static final int editionGet = 0X03;
    private static final int powerGet = 0X04;
    private static final String TAG = "SecondPageMain";
    //页面组件
    private TextView TV_conResult;
    private TextView TV_cfgResult;
    private TextView TV_editonResult;
    private TextView TV_batResult;
    private Button Btn_jump;

    //蓝牙配置信息
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBluetoothLeScanner;
    private BluetoothLeAdvertiser mBluetoothLeAdvertiser;
    private static final String BROADCAST_SERVICE = "00001802-0000-1000-8000-00805f9b34fb";
    //延迟处理
    private Handler mHandler;
    //当前的操作类型
    private int curOp;
    private int previousOp;
    private int successOp;
    //目的mac
    private String targetName;
    private ReceiveMessage totalMessage;
    private String simulationMsg = "0D093030303130323033303430350416000300";
//    private String simulationMsg = "0D093130303130323033303430350416000300";
    //状态参数
    //是否在检测
    private boolean mScanning;
    //是否找到指定mac的设备
    private boolean found = false;
    private int curNum = -1;
    private int mTotalProgress;
    private int mCurrentProgress;
    private TasksCompletedView mTasksView;
    //页面组件


    @Override
    @SuppressLint("NewApi")
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.secondpage);
        initBasicCfg();
        initBroad();
        getCfg(find);
    }

    /**
     * 初始化界面Ui
     */
    private void initBasicCfg(){
        //获取页面传来的Mac
        Intent intent = getIntent();
        targetName = intent.getStringExtra("mac");
        TextView mac = findViewById(R.id.mac);
        TV_conResult = findViewById(R.id.TV_conResult);
        TV_cfgResult = findViewById(R.id.TV_cfgResult);
        TV_editonResult = findViewById(R.id.TV_editonResult);
        TV_batResult = findViewById(R.id.TV_batResult);
        Btn_jump = findViewById(R.id.Btn_jump);
        mTasksView = (TasksCompletedView) findViewById(R.id.tasks_view);
        Btn_jump.setOnClickListener(this);

        totalMessage = new ReceiveMessage();
        mac.setText("您输入的Mac地址为：" + targetName);
        mTotalProgress = 50;
        mCurrentProgress = 1;
        //设置加载条
        mTasksView.setProgress(mCurrentProgress);
        new Thread(new ProgressRunable()).start();
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
     * 开始扫描
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void bleScan() {
        //检测是否支持
        if( !mBluetoothAdapter.isEnabled()) {
            BlueToothUtil.showDialog(this,"该设备不支持蓝牙！");
        }
            if (Build.VERSION.SDK_INT >= 21) {
                if (mBluetoothLeScanner == null) {
                    mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
                }
                mBluetoothLeScanner.startScan(null, createScanSetting(), mScanCallback);
            } else {
                BlueToothUtil.showDialog(SecondPageMain.this, "手机版本过低， 该软件不支持！");
            }

    }



    /**
     * 解析收到的报文
     * @param msg
     */
    @SuppressLint("ResourceAsColor")
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void parseMsg(String msg) {
        Log.d(TAG, "parseMsg: 当前的Op" + curOp);
        Log.d(TAG, "parseMsg: 当前的num" + curNum);

        switch (curOp) {

            case find:
                found = true;
                successOp = curOp;
                mTotalProgress = 50;
                mCurrentProgress = 25;
                TV_conResult.setText("成功");
                TV_conResult.setTextColor(R.color.result_points);
                getCfg(cfgGet);
                break;
            case cfgGet:
                Log.d(TAG, "parseMsg: = cfgGet");
                try {
                    ReceiveMessage receiveMessage = new ReceiveMessage(msg);
                    if (receiveMessage.getExeResult() == 0) {
                        Log.d(TAG, "当前的模拟数据" + msg);
                        Log.d(TAG, "当前的receiveMessage" + receiveMessage);
                        if (BlueToothUtil.messageCorrect(receiveMessage, curNum, targetName, curOp)) {
                            successOp = curOp;
                            totalMessage.setBroadType(receiveMessage.getBroadType());
                            totalMessage.setPowerSet(receiveMessage.getPowerSet());
                            totalMessage.setRepFre(receiveMessage.getRepFre());
                            totalMessage.setPowerAlarm(receiveMessage.getPowerAlarm());
                            totalMessage.setSignAlarm(receiveMessage.getSignAlarm());
                            TV_cfgResult.setText("成功");
                            TV_cfgResult.setTextColor(R.color.result_points);
                            mTotalProgress = 75;
                            mCurrentProgress = 50;
                            getCfg(editionGet);
                        } else {
                            Log.d(TAG, "当前的信息: curNum =" + curNum + "curOp=" + curOp);
                        }
                    }
                } catch (Exception e) {
                    Toast.makeText(SecondPageMain.this,e.toString(), Toast.LENGTH_SHORT).show();
                }
                break;
            case editionGet:
                Log.d(TAG, "parseMsg: = editionGet");
                try {
                    ReceiveMessage receiveMessage = new ReceiveMessage(msg);
                    if (receiveMessage.getExeResult() == 0) {
                        if (BlueToothUtil.messageCorrect(receiveMessage, curNum, targetName, curOp)) {
                            successOp =curOp;
                            totalMessage.setEdition(receiveMessage.getEdition());
                            TV_editonResult.setText("成功");
                            TV_editonResult.setTextColor(R.color.result_points);
                            mTotalProgress = 100;
                            mCurrentProgress = 75;
                            getCfg(powerGet);
                        } else {
                            Log.d(TAG, "当前的信息: curNum =" + curNum + "curOp=" + curOp);
                        }
                    }
                }catch (Exception e) {
                    Toast.makeText(SecondPageMain.this,e.toString(), Toast.LENGTH_SHORT).show();
                }
                break;
            case powerGet:
                Log.d(TAG, "parseMsg: = powerGet");
                try {
                    ReceiveMessage receiveMessage = new ReceiveMessage(msg);
                    if (receiveMessage.getExeResult() == 0) {
                        if (BlueToothUtil.messageCorrect(receiveMessage, curNum, targetName, curOp)) {
                            Log.d(TAG, "parseMsg: 进入了power");
                            successOp = curOp;
                            curOp = initOp;
                            totalMessage.setBattery(receiveMessage.getBattery());
                            TV_batResult.setText("成功");
                            TV_batResult.setTextColor(R.color.result_points);
                            mCurrentProgress = 99;
                            Btn_jump.setEnabled(true);
                            stopAdvertise();
                            stopScan();
                            Toast.makeText(SecondPageMain.this, "读取完成",Toast.LENGTH_SHORT).show();
                        } else {
                            Log.d(TAG, "当前的信息: curNum =" + curNum + "curOp=" + curOp);
                        }
                    }
                }catch (Exception e) {
                    Toast.makeText(SecondPageMain.this,e.toString(), Toast.LENGTH_SHORT).show();
                }
                break;

        }
    }

    /**
     * 解析报文后的操作
     * @param Op
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void getCfg(int Op){
        switch (Op) {
            case find:
                Log.d(TAG, "getCfg: 进入find");
                curOp = Op;
                bleScan();
                break;
            case cfgGet:
                Log.d(TAG, "getCfg: cfgGet");
                previousOp = curOp;
                Log.d(TAG, " previousOp = " + previousOp + "successOp=" + successOp );
                if (previousOp == successOp) {
                    Log.d(TAG, "getCfg: 成功进入对比");
                    curOp = Op;
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            simulationMsg = "0D093030303130323033303430350A160002000101001F1F1F";
//                            simulationMsg = "0D093130303130323033303430350A160002000101001F1F1F";
                        }
                    },2000);
                    mBluetoothLeAdvertiser.stopAdvertising(mAdvertiseCallback);
                    mBluetoothLeAdvertiser.startAdvertising(createAdvSettings(false, 0),createAdvertiseData(), mAdvertiseCallback);
                }
                break;
            case editionGet:
                Log.d(TAG, "getCfg: editionGet");
                Log.d(TAG, "getCfg: 进入case Edition");
                Log.d(TAG, "getCfg: 当前的操作码 = " + curNum);
                previousOp = curOp;
                Log.d(TAG, " previousOp = " + previousOp + "successOp=" + successOp );
                if (previousOp == successOp) {
                    curOp = Op;
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            simulationMsg = "0D09303030313032303330343035 0916 01 03 00 04 31 30 31 30";
//                            simulationMsg = "0D093130303130323033303430350A160002000101001F1F1F";
                        }
                    },2000);
                    mBluetoothLeAdvertiser.stopAdvertising(mAdvertiseCallback);
                    mBluetoothLeAdvertiser.startAdvertising(createAdvSettings(false, 0),createAdvertiseData(), mAdvertiseCallback);
                }
                break;
            case powerGet:
                Log.d(TAG, "getCfg: 进入case powerGet");
                previousOp = curOp;
                if (previousOp == successOp) {
                    curOp = Op;
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            simulationMsg = "0D093030303130323033303430350616020400001F";
//                            simulationMsg = "0D093130303130323033303430350A160002000101001F1F1F";

                        }
                    },2000);
                    mBluetoothLeAdvertiser.stopAdvertising(mAdvertiseCallback);
                    mBluetoothLeAdvertiser.startAdvertising(createAdvSettings(false, 0),createAdvertiseData(), mAdvertiseCallback);
                }
                break;
        }
        createHandle(curOp);
    }


    private void errorUI() {
        switch (curOp) {
            case find:
                TV_conResult.setText("失败");
                TV_conResult.setTextColor(getResources().getColor(R.color.viewfinder_laser));
//                break;
            case cfgGet:
                TV_cfgResult.setText("失败");
                TV_cfgResult.setTextColor(getResources().getColor(R.color.viewfinder_laser));
//                break;
            case editionGet:
                TV_editonResult.setText("失败");
                TV_editonResult.setTextColor(getResources().getColor(R.color.viewfinder_laser));
//                break;
            case powerGet:
                TV_batResult.setText("失败");
                TV_batResult.setTextColor(getResources().getColor(R.color.viewfinder_laser));
                break;
            default:
                break;
        }
    }











    /**
     * 加载条改变事件
     */
    class ProgressRunable implements Runnable {
        @Override
        public void run() {
            while (mCurrentProgress < mTotalProgress) {
                Log.d(TAG, "run: 当前最大的进度" + mTotalProgress);
                Log.d(TAG, "run:当前加的进度" + mCurrentProgress);
                mCurrentProgress += 1;
                mTasksView.setProgress(mCurrentProgress);
                try {
                    Thread.sleep(1000);
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.d(TAG, "run: 错误" + e.toString());
                }
            }

        }
    }

    /**
     * 延迟检测是否交互成功
     * @param Op
     */
    private void createHandle(final int Op) {
        int time  = 5000;
        String tips = "配置失败，请将设备靠近手机并保持设备开启";
        if (Op == find) {
            time = 30000;
            tips = "搜索失败，请将设备靠近手机并确认红灯快速闪烁2次";
        }
        final String finalTips = tips;
        mHandler.postDelayed(new Runnable() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void run() {
                if (curOp == Op) {
                    BlueToothUtil.showDialog(SecondPageMain.this, finalTips);
                    mTotalProgress = mCurrentProgress;
                    errorUI();
                    stopScan();
                    stopAdvertise();
                }

            }
        },time);
    }




    /**
     * 组件点击事件
     * @param view
     */
    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.Btn_jump) {
            Intent intent = new Intent(SecondPageMain.this, ThirdPageSend.class);
            intent.putExtra("info", totalMessage);
            intent.putExtra("mac", targetName);
            startActivity(intent);
            finish();
        }
    }

    /**
     * 扫描设定
     * @return
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private ScanSettings createScanSetting() {
        ScanSettings.Builder mSettingsBuilder = new ScanSettings.Builder();
        mSettingsBuilder.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY);
        if (Build.VERSION.SDK_INT >= 23) {
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
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            if (result != null) {
                if (result.getScanRecord().getBytes() != null) {
                    String reDataStr = BlueToothUtil.bytesToHexString(result.getScanRecord().getBytes());
                    Log.d(TAG, "\nonScanResult:  = " + reDataStr);
//                    reDataStr = "0D093030303130323033303430350416000300";
//                    simulationMsg
                    if(BlueToothUtil.initTest(reDataStr, targetName)) {
//                        found = true;
                        parseMsg(reDataStr);
                    }

                }
            }
        }
    };

    /**
     * 停止扫描
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void stopScan(){
        mBluetoothLeScanner.stopScan(mScanCallback);
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
     * 广播报文设置
     * @return
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private AdvertiseData createAdvertiseData() {
        SendMessage sendMessage = new SendMessage(targetName, ++curNum, curOp,null);
        AdvertiseData mAdvertiseData = new AdvertiseData.Builder()
                .addServiceData(ParcelUuid.fromString(BROADCAST_SERVICE), BlueToothUtil.fieldShaping(sendMessage))
                .build();
        return mAdvertiseData;
    }

    /**
     * 广播回调
     */
    @SuppressLint("NewApi")
    private AdvertiseCallback mAdvertiseCallback = new AdvertiseCallback() {
        @Override
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
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
     * 关闭广播
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void stopAdvertise() {
        boolean n = mBluetoothLeAdvertiser == null;
        Log.d(TAG, "stopAdvertise: 是否为null" + n);
        Log.d(TAG, "stopAdvertise: " + mBluetoothLeAdvertiser);
        if (mBluetoothLeAdvertiser != null) {
            mBluetoothLeAdvertiser.stopAdvertising(mAdvertiseCallback);
            Log.e(TAG, "停止广播");
        }
    }

}