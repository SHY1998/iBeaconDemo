package com.example.ibeacondemo;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.ibeacondemo.Util.BlueToothUtil;
import com.example.ibeacondemo.zxing.android.CaptureActivity;
import com.example.ibeacondemo.Util.BlueToothUtil;


public class FirstPageCode extends AppCompatActivity implements View.OnClickListener{

    //对接Mac
    private String targetName;
    private TextView tv_scanResult;
    private static final String TAG = "FirstPgeCode";
    private static final int REQUEST_CODE_SCAN = 0X0000;
    private static final String DECODED_CONTENT_KEY = "codedContent";
    private static final String DECODED_BITMAP_KEY = "codedBitmap";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState)  {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.firstpage);
        initUI();

    }

    /**
     * 初始化Ui
     */
    private void initUI() {
        Button btn_scan = findViewById(R.id.btn_scan);
        Button btn_jump = findViewById(R.id.conTest);
        tv_scanResult = findViewById(R.id.tv_scanResult);
        btn_scan.setOnClickListener(this);
        btn_jump.setOnClickListener(this);

    }

    /**
     * 开启二维码扫描
     */
    private void codeScan() {
        Intent intent = new Intent(FirstPageCode.this, CaptureActivity.class);
        startActivityForResult(intent, REQUEST_CODE_SCAN);
    }


    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            //点击二维码扫描
            case R.id.btn_scan:
                if (ContextCompat.checkSelfPermission(FirstPageCode.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(FirstPageCode.this, new String[]{Manifest.permission.CAMERA}, 1);
                } else {
                    codeScan();
                }
                break;
            //点击搜索设备
            case R.id.conTest:
                targetName = tv_scanResult.getText().toString();
                if (targetName.equals("")) {
                    BlueToothUtil.showDialog(FirstPageCode.this,"请先输入设备Mac地址");
                } else {
                    Intent intent = new Intent(FirstPageCode.this, SecondPageMain.class);
                    intent.putExtra("mac", targetName);
                    startActivity(intent);
                }
                break;
            default:
                break;
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults){
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    codeScan();
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
}
