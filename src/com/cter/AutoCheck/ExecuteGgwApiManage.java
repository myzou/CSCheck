package com.cter.AutoCheck;

import cn.hutool.core.util.StrUtil;
import com.cter.util.ggwApi.GgwApi;

import java.util.concurrent.Callable;

public class ExecuteGgwApiManage implements Callable<String > {

    private String deviceName;
    private String ip;
    private String command;

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public ExecuteGgwApiManage(String deviceName, String ip, String command) {
        this.deviceName = deviceName;
        this.ip = ip;
        this.command = command;
    }


    @Override
    public String call() throws Exception {
        if(StrUtil.isBlank(ip)){
           return GgwApi.getGGWRestStringByDeviceName(deviceName,command);
        }
        return GgwApi.getGGWStringRestByIP(ip,command);

    }
}
