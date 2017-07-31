package com.sergey.geo;

/**
 * Created by sober on 31.07.2017.
 */

public enum NetworkType {

    UNKNOWN(0), MOBILE_DATA(1), WIFI(2);

    private int code;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    private String name;

    NetworkType(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }
}