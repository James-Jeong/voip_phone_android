package com.jamesj.voip_phone_android.signal.base;

public class RegiInfo {

    private final String fromNo;
    private final String ip;
    private final int port;
    private final int expires;

    public RegiInfo(String fromNo, String ip, int port, int expires) {
        this.fromNo = fromNo;
        this.ip = ip;
        this.port = port;
        this.expires = expires;
    }

    public String getFromNo() {
        return fromNo;
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    public int getExpires() {
        return expires;
    }

}
