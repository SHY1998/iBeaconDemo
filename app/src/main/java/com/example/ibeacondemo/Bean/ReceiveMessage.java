package com.example.ibeacondemo.Bean;

import android.util.Log;

import com.example.ibeacondemo.Util.BlueToothUtil;

public class ReceiveMessage {
    //设备mac
    private String mac;
    //数据编号
    private int curNum;
    //命令类型
    private int comType;
    //开关机
    private String powerSet;
    //协议类型
    private String broadType;
    //上报频率
    private String repFre;
    //电量阈值
    private String powerAlarm;
    //移动信号告警阈值
    private String signAlarm;
    //版本号长度
    private String editLen;
    //版本号
    private String edition;

    public int getExeResult() {
        return exeResult;
    }

    public void setExeResult(int exeResult) {
        this.exeResult = exeResult;
    }

    //执行结果
    private int exeResult;
    //电池电量
    private String battery;





    public String getEditLen() {
        return editLen;
    }

    public void setEditLen(String editLen) {
        this.editLen = editLen;
    }

    public String getEdition() {
        return edition;
    }

    public void setEdition(String edition) {
        this.edition = edition;
    }

    public String getBattery() {
        return battery;
    }

    public void setBattery(String battery) {
        this.battery = battery;
    }


    @Override
    public String toString() {
        return "ReceiveMessage{" +
                "mac='" + mac + '\'' +
                ", curNum=" + curNum +
                ", comType=" + comType +
                ", powerSet='" + powerSet + '\'' +
                ", broadType='" + broadType + '\'' +
                ", repFre='" + repFre + '\'' +
                ", powerAlarm='" + powerAlarm + '\'' +
                ", signAlarm='" + signAlarm + '\'' +
                ", editLen='" + editLen + '\'' +
                ", edition='" + edition + '\'' +
                ", exeResult=" + exeResult +
                ", battery='" + battery + '\'' +
                '}';
    }

    public ReceiveMessage(String mac, int curNum, int comType, String powerSet, String broadType, String repFre, String powerAlarm, String signAlarm) {
        this.mac = mac;
        this.curNum = curNum;
        this.comType = comType;
        this.powerSet = powerSet;
        this.broadType = broadType;
        this.repFre = repFre;
        this.powerAlarm = powerAlarm;
        this.signAlarm = signAlarm;
    }

    public ReceiveMessage(String powerSet, String broadType, String repFre, String powerAlarm, String signAlarm) {
        this.powerSet = powerSet;
        this.broadType = broadType;
        this.repFre = repFre;
        this.powerAlarm = powerAlarm;
        this.signAlarm = signAlarm;
    }

    public ReceiveMessage(String editLen, String edition) {
        this.editLen = editLen;
        this.edition = edition;
    }

//    public SendMessage(String battery) {
//        this.battery = battery;
//    }
    public ReceiveMessage(String msg) {
        int fstLen = Integer.parseInt(msg.substring(0,2),16)+1;
        Log.d("213", "解析中" + msg.substring(4,28) );
        this.mac = BlueToothUtil.hexStringToString(msg.substring(4,fstLen*2));
        //Mac地址字段长度
        String dataPart = msg.substring(fstLen*2+4);
        Log.d("123", "dataPart" + dataPart);
        //数据编号
        this.curNum = Integer.parseInt(dataPart.substring(0,2),16);
        this.comType = Integer.parseInt(dataPart.substring(2,4),16);
        this.exeResult = Integer.parseInt(dataPart.substring(4,6),16);
        String lastData = dataPart.substring(6);
        Log.d("asd", "lastData " + lastData);
        if (lastData != null && !lastData.equals("")){
            switch (this.comType) {
                case 1:
                    break;
                case 2:
                    switch (Integer.parseInt(lastData.substring(0,2),16)) {
                        case 0:
                            this.broadType = "MQTT";
                            break;
                        case 1:
                            this.broadType = "JTT808";
                            break;
                        default:
                            break;
                    }
                    switch (Integer.parseInt(lastData.substring(2,4),16)) {
                        case 0:
                            this.powerSet = "关机";
                            break;
                        case 1:
                            this.powerSet = "开机";
                            break;
                        default:
                            break;
                    }
                    this.repFre = String.valueOf(Integer.parseInt(lastData.substring(4,6),16));
                    this.powerAlarm = String.valueOf(Integer.parseInt(lastData.substring(6,8),16));
                    this.signAlarm = String.valueOf(Integer.parseInt(lastData.substring(8,10),16));
                    break;
                case 3:
                        this.editLen = lastData.substring(0,2);
                        this.edition = lastData.substring(2);
                    break;
                case 4:
                    this.battery = String.valueOf(Integer.parseInt(lastData,16));
                    break;
                case 0XFF:
                    break;
                default:
                    break;
            }
        }

    }

    public ReceiveMessage(String mac, int curNum, int comType) {
        this.mac = mac;
        this.curNum = curNum;
        this.comType = comType;
    }



    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public int getCurNum() {
        return curNum;
    }

    public void setCurNum(int curNum) {
        this.curNum = curNum;
    }

    public int getComType() {
        return comType;
    }

    public void setComType(int comType) {
        this.comType = comType;
    }

    public String getPowerSet() {
        return powerSet;
    }

    public void setPowerSet(String powerSet) {
        this.powerSet = powerSet;
    }

    public String getRepFre() {
        return repFre;
    }

    public void setRepFre(String repFre) {
        this.repFre = repFre;
    }

    public String getPowerAlarm() {
        return powerAlarm;
    }

    public void setPowerAlarm(String powerAlarm) {
        this.powerAlarm = powerAlarm;
    }

    public String getSignAlarm() {
        return signAlarm;
    }

    public void setSignAlarm(String signAlarm) {
        this.signAlarm = signAlarm;
    }

    public String getBroadType() {
        return broadType;
    }

    public void setBroadType(String broadType) {
        this.broadType = broadType;
    }
}
