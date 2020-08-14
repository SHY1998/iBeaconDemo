package com.example.ibeacondemo.Util;

import android.content.Context;
import android.content.Intent;
import android.os.CountDownTimer;

import com.example.ibeacondemo.UselessActivity.ScanActivity;

public class CountTimer extends CountDownTimer {
    private Context context;


    public CountTimer(long millisInFuture, long countDownInterval, Context context) {
        super(millisInFuture, countDownInterval);
        this.context = context;
    }

    @Override
    public void onTick(long l) {

    }

    @Override
    public void onFinish() {
        context.startActivity(new Intent(context, ScanActivity.class));
    }
}
