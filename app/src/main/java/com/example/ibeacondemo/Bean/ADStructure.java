package com.example.ibeacondemo.Bean;


public class ADStructure {
    private int length;
    private String type;
    private String data;

    public ADStructure(int length, String type, String data) {
        this.length = length;
        this.type = type;
        this.data = data;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
}
