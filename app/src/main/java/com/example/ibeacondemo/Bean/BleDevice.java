package com.example.ibeacondemo.Bean;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanRecord;

public class BleDevice {
    private BluetoothDevice device;
    private int rssi;
    private Byte scanRecordBytes;
    private Boolean isConnectable;
    private ScanRecord scanRecord;

    public BleDevice(BluetoothDevice device, int rssi, Byte scanRecordBytes, Boolean isConnectable, ScanRecord scanRecord) {
        this.device = device;
        this.rssi = rssi;
        this.scanRecordBytes = scanRecordBytes;
        this.isConnectable = isConnectable;
        this.scanRecord = scanRecord;
    }

    public BluetoothDevice getDevice() {
        return device;
    }

    public void setDevice(BluetoothDevice device) {
        this.device = device;
    }

    public int getRssi() {
        return rssi;
    }

    public void setRssi(int rssi) {
        this.rssi = rssi;
    }

    public Byte getScanRecordBytes() {
        return scanRecordBytes;
    }

    public void setScanRecordBytes(Byte scanRecordBytes) {
        this.scanRecordBytes = scanRecordBytes;
    }

    public Boolean getConnectable() {
        return isConnectable;
    }

    public void setConnectable(Boolean connectable) {
        isConnectable = connectable;
    }

    public ScanRecord getScanRecord() {
        return scanRecord;
    }

    public void setScanRecord(ScanRecord scanRecord) {
        this.scanRecord = scanRecord;
    }
}
