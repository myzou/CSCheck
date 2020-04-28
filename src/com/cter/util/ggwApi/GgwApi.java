package com.cter.util.ggwApi;

import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.cter.util.BaseLog;
import com.cter.util.ggwApi.rsa.RSAEncrypt;
import com.cter.util.ggwApi.totp.Totp;
import org.springframework.util.StringUtils;

import java.nio.charset.Charset;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class GgwApi {

    static String getDeviceIp = "http://210.5.3.177:48888/get_device_ip/deviceName";
    static String GGW_URL = "http://210.5.3.177:48888/ExecuteCommand/RSA";
    static String LOGIN_GGW_URL = "http://210.5.3.177:48888/GetLoginSession/RSA?crypto_sign=";
    static String getPasswordUrl = "http://210.5.3.30:8082/mtp/login_getPassword.action";
    static Map<String, Map<String, String>> ipLastLoginTimeMap = new HashMap<>();
    static int loginNum = 0;//登录次数
    static BaseLog log = new BaseLog("ExecuteCommandLog");

    public static void main(String[] args) {
//        String command = "show interfaces |match ge-11 ";
//        System.out.println("根据ip 218.96.240.93 获取结果：" +getGGWStringRestByIP("" + "218.96.240.93", command) );
//        System.out.println("根据设备 CNGUZYUJ1001C 获取结果：" + getGGWRestStringByDeviceName("CNGUZYUJ1001C", command) );

//        String command = "ping interface ge-0/0/2.0 source 183.91.156.33 rapid count  1000 218.204.242.242";
//        System.out.println("结果："+getGGWStringRestByIP("218.96.240.104",command));
//        System.out.println("根据设备 CNBEJDEX1004E 获取结果：" + getGGWRestStringByDeviceName("CNBEJDEX1004E", command) );


//        String command1 = "ping interface xe-2/0/3.0 source 218.97.29.225 rapid count 50 218.204.242.242";
//        System.out.println("结果："+getGGWStringRestByIP("218.96.240.85",command1));
//        System.out.println("获取结果：" + getGGWRestStringByDeviceName("CNSHHFTX1004E", command1) );


        String command2 = " ping interface xe-0/2/0.2814 source 210.5.31.125 rapid count 50 202.98.0.68 ";
        System.out.println("结果：" + getGGWStringRestByIP("218.96.240.95", command2));
        System.out.println("获取结果：" + getGGWRestStringByDeviceName("CNGUZYUJ1001E", command2) );

    }

    /**
     * 根据设备名称 直接获取调用结果字符串
     * @param deviceName
     * @param command
     * @return
     */
    public static String getGGWRestStringByDeviceName(String deviceName, String command) {
        Map<String, String> restMap =  getGGWRestByDeviceName(deviceName, command);
        String code=restMap.get("code");
        String data=restMap.get("data");
        if(code.equals("1")){
            return data;
        }else{
            return "无法获取结果";
        }
    }

    /**
     * 根据ip直接获取调用结果字符串
     * @param ip
     * @param command
     * @return
     */
    public static String getGGWStringRestByIP(String ip, String command) {
        Map<String, String> restMap =  getGGWRestByIP(ip, command);
        String code=restMap.get("code");
        String data=restMap.get("data");
        if(code.equals("1")){
           return data;
        }else{
            return "无法获取结果";
        }
    }

    /**
     * 根据 设备名 和command 获取 map 结果
     * @param deviceName
     * @param command
     * @return
     */
    public static Map<String, String> getGGWRestByDeviceName(String deviceName, String command) {
        Map<String, String> restMap = new HashMap<>();

        String ip = "";
        try {
            ip = HttpUtil.get(getDeviceIp.replace("deviceName", deviceName));
            if (StrUtil.isBlank(ip) || ip.indexOf("none") > -1) {
                restMap.put("code","1001");
                restMap.put("data","无法查询到对应设备");
                return restMap;
            }
        } catch (Exception e) {
            restMap.put("code","1002");
            restMap.put("data","GGWAPI 访问出错");
            return restMap;
        }
        return getGGWRestByIP(ip, command);
    }

    /**
     * 根据ip和command 获取 map 结果
     * @param ip
     * @param command
     * @return
     */
    public static Map<String, String>  getGGWRestByIP(String ip, String command) {
        Map<String, String> restMap = new HashMap<>();
        Map<String, String> paramMap = new HashMap<>();
        String passwordJson = "";
        try {
            passwordJson = HttpUtil.get(getPasswordUrl);
        } catch (Exception e) {
            restMap.put("code","1003");
            restMap.put("data","获取密匙 访问出错");
            return restMap;
        }
        JSONObject jsonJSONObject = JSONUtil.parseObj(passwordJson);
        paramMap.put("opName", jsonJSONObject.getStr("op"));
        paramMap.put("opPassword", jsonJSONObject.getStr("password"));
        paramMap.put("secretBase32", jsonJSONObject.getStr("secretBase32"));
        paramMap.put("command", command);
        paramMap.put("ip", ip);
        return executeMain(paramMap,1);
    }

    /**
     * 1001 无法查询到对应设备
     * 1002 GGWAPI 访问出错
     * 1003 获取密匙 访问出错
     * 1004 登录ggw失败
     * 1005 无法获取结果
     * 1006 无法获取结果异常
     * @param paramMap
     * @param executeNumber
     * @return
     */
    public static Map<String, String> executeMain(Map<String, String> paramMap,int executeNumber) {
        Map<String, String> restMap = new HashMap<>();

        if (!loginTest(paramMap, 1)) {
            restMap.put("code","1004");
            restMap.put("data","登录ggw失败");
            return restMap;
        }
        return  execute(paramMap,1);
    }

    /**
     * 转换字符为文件编码
     *
     * @param str 字符
     * @return
     */
    public static String urlReplace(String str) {
//        str= str.replace("\"","%22");
//        str= str.replace("\\+","%2B");
//        str= str.replace("\\/ ","%2F");
        str = str.replace(" ", "%20");
//        str= str.replace("\\?","%3F");
//        str= str.replace("\\%","%25");
//        str= str.replace("\\#","%23");
//        str= str.replace("\\&","%26");
        return str;
    }

    /**
     * 不加密调用
     */
    public static Map<String, String>  execute(Map<String, String> paramMap, int executeNumber) {
        Map<String, String> restMap = new HashMap<>();

        String rest = "";
        String nowTime = Long.toString(new Date().getTime() / 1000);
        String secretBase32 = paramMap.get("secretBase32");
        long refreshTime = 30L;
        long createTime = 0L;
        String crypto = "HmacSHA1";
        String codeDigits = "6";
        String verificationCode = Totp.GenerateVerificationCode(secretBase32, refreshTime, createTime, crypto, codeDigits);

        int sign = 123456;
        String restNotTotp = "";
        String restTotp = "http://210.5.3.177:48888/ExecuteCommand?username=" + paramMap.get("opName") + "&&password=" +
                paramMap.get("opPassword") + verificationCode + "&&sign=" + (sign + executeNumber - 1) + "&&timestamp=" + nowTime + "&&command=" + urlReplace(paramMap.get("command")) + "&&ip=" + paramMap.get("ip");

        int code = 0;
        try {
            rest = HttpUtil.get(restTotp);
            JSONObject resultJsonObject = JSONUtil.parseObj(rest);
            code = resultJsonObject.getInt("code");
            String data = resultJsonObject.getStr("data");
            data = (StringUtils.isEmpty(data)) ? data : data.replace("\\n", "\n");
            if (code == 0) {
                restMap.put("code","1");
                restMap.put("data",data);
                return restMap;
            } else {
                loginTest(paramMap,sign + executeNumber);
                nowTime = Long.toString(new Date().getTime() / 1000);
                restNotTotp = "http://210.5.3.177:48888/ExecuteCommand?username=" + paramMap.get("opName") + "&&password=" +
                        paramMap.get("opPassword") + "&&sign=" + (sign + executeNumber) + "&&timestamp=" + nowTime + "&&command=" + urlReplace(paramMap.get("command")) + "&&ip=" + paramMap.get("ip");
                rest = HttpUtil.get(restTotp);
                resultJsonObject = JSONUtil.parseObj(rest);
                code = resultJsonObject.getInt("code");
                data = resultJsonObject.getStr("data");
                data = (StringUtils.isEmpty(data)) ? data : data.replace("\\n", "\n");
                if (code == 0) {
                    restMap.put("code","1");
                    restMap.put("data",data);
                    return restMap;
                } else {
                    if (executeNumber == 3) {
                        log.info("无法获取结果：\n" + "restTotp:\n" + restTotp + "restNotTotp:\n" + restNotTotp + "\n" + JSONUtil.toJsonStr(paramMap));
                        restMap.put("code","1005");
                        restMap.put("data","无法获取结果");
                        return restMap;
                    } else {
                        return execute(paramMap, executeNumber + 1);
                    }
                }
            }
        } catch (Exception e) {
            if (executeNumber == 3) {
                log.printStackTrace(e);
                log.info("：\n" + "restTotp:\n" + restTotp + "restNotTotp:\n" + restNotTotp + "\n" + JSONUtil.toJsonStr(paramMap));
                restMap.put("code","1006");
                restMap.put("data","无法获取结果异常");
                return restMap;
            } else {
                return execute(paramMap, executeNumber + 1);
            }
        }
    }

    public static boolean loginTest(Map<String, String> paramMap, int loginNumber) {
        String rest = "";
        try {
            String nowTime = Long.toString(new Date().getTime() / 1000);
            String url = "http://210.5.3.177:48888/GetLoginSession?username=" + paramMap.get("opName") + "&&password=" +
                    paramMap.get("opPassword") + "&&sign=123456&&timestamp=" + nowTime;
            rest = HttpUtil.get(url);
            JSONObject resultJsonObject = JSONUtil.parseObj(rest);
            int code = resultJsonObject.getInt("code");
            String data = resultJsonObject.getStr("data");
            if (code == 0 || code == 10003) {
                return true;
            } else {
                if (loginNumber == 3) {
                    log.info("登录失败:" + rest);
                    return false;
                } else {
                    return loginTest(paramMap, loginNumber + 1);
                }
            }
        } catch (Exception e) {
            if (loginNumber == 3) {
                log.info("登录异常:" + rest);
                return false;
            } else {
                return loginTest(paramMap, loginNumber + 1);
            }
        }
    }


    /**
     * 加密调用的方法
     * 根据 paramMap 参数来执行command来活去执行的结果
     * get("error") 有内容为错误，否则 get("data")  为执行结果
     * op1768 验证码密匙 secretBase32：gmp7bb3kpghainowhr7jthvkkuy4buds
     *
     * @param paramMap
     * @param log
     * @return
     */
    public static Map<String, String> executeEncrypt (Map<String, String> paramMap) {
        Map<String, String> returnMap = new HashMap<>();
        String tempSgin = (StringUtils.isEmpty(paramMap.get("sign")) ? "123456" : paramMap.get("sign"));
        String nowTime = Long.toString(new Date().getTime() / 1000);

        try {
            Map<String, String> lastTimeMap = new HashMap<>();
            lastTimeMap = ipLastLoginTimeMap.get(paramMap.get("ip"));
            Long intervalTime = 601L;//默认是已经超时，并没有登录状态
            if (lastTimeMap != null && lastTimeMap.get(tempSgin) != null) {
                //ip>sign>op;lastTime
                String opName = ipLastLoginTimeMap.get(paramMap.get("ip")).get(tempSgin).split(";")[0];
                String tempLoginLastTime = ipLastLoginTimeMap.get(paramMap.get("ip")).get(tempSgin).split(";")[1];
                Long ipLastLoginTime = Long.valueOf(tempLoginLastTime) * 1000L;
                //对应的ip距离上次登陆的时间间隔 600 秒登录状态失效
                intervalTime = cn.hutool.core.date.DateUtil.between(DateUtil.date(ipLastLoginTime), new Date(), DateUnit.SECOND);
            }
            if (intervalTime.intValue() < 600) {
                int loginNumber = 3;//参数登录次数
                Map<String, String> tempMap = new HashMap<>();
                Map<String, String> tempTotpMap = new HashMap<>();

                for (int i = 1; i <= loginNumber; i++) {
                    String methodType = "execute";
                    String url = GGW_URL + getGGWParamAssemble(paramMap, "", methodType);
//                    log.info("第" + i + "次,无验证码方式执行命令 url：\n" + url);
                    tempMap = executeCommandOrLogin(url, paramMap, methodType);
                    if (!StrUtil.isBlank(tempMap.get("error")) && loginNum < 10) {
                        paramMap.put("sign", "123457");
                        String tempLoginUrl = LOGIN_GGW_URL + getGGWParamAssemble(paramMap, "", "login");
//                        log.info("验证码 登录到ggw获取session url：\n" + url);
                        executeCommandOrLogin(tempLoginUrl, paramMap, "login");

                        String totpUrl = GGW_URL + getGGWParamAssemble(paramMap, "totp", methodType);
//                        log.info("第" + i + "次,验证码执方式行命令 url：\n" + totpUrl);
                        tempTotpMap = executeCommandOrLogin(totpUrl, paramMap, methodType);
                        if ((!StrUtil.isBlank(tempTotpMap.get("error")) && i == loginNumber) || !StrUtil.isBlank(tempTotpMap.get("pass"))) {
                            if ((!StrUtil.isBlank(tempTotpMap.get("error")) && i == loginNumber)) {
                                log.info("执行失败：\n" + JSONUtil.toJsonStr(paramMap));
                            }
                            return tempTotpMap;
                        } else {
                            continue;
                        }
                    } else if (!StrUtil.isBlank(tempMap.get("pass"))) {
                        return tempMap;
                    }
                }
            } else {
                paramMap.put("sign", "123456");
                String methodType = "login";
                int loginNumber = 2;//参数登录次数
                Map<String, String> tempMap = new HashMap<>();
                for (int i = 1; i <= loginNumber; i++) {
                    String url = LOGIN_GGW_URL + getGGWParamAssemble(paramMap, "", methodType);
//                    log.info("第" + i + "次,登录到ggw获取session url：\n" + url);
                    tempMap = executeCommandOrLogin(url, paramMap, methodType);
                    if ((!StrUtil.isBlank(tempMap.get("error")) && i == loginNumber)) {
                        log.info("登录失败：\n" + JSONUtil.toJsonStr(paramMap));
                        return tempMap;
                    } else if (!StrUtil.isBlank(tempMap.get("pass"))) {
                        return executeEncrypt(paramMap);
                    }
                    continue;
                }
            }
        } catch (Exception e) {
            log.info(JSONUtil.toJsonStr(paramMap));
            log.printStackTrace(e);
            returnMap.put("error", "error");
            returnMap.put("message", "execute command exception,Please contact your administrator");
            return returnMap;
        }
        return null;
    }

    /**
     * 根据参数 获取 ggw 拼接的必须参数
     *
     * @param paramMap
     * @param passwordType 密码类型,默认不加上 totp 6位数验证码
     * @param urlType      登录类型 login execute
     * @return
     */
    public static String getGGWParamAssemble(Map<String, String> paramMap, String passwordType, String urlType) {
        String secretBase32 = paramMap.get("secretBase32");// "gmp7bb3kpghainowhr7jthvkkuy4buds";
        long refreshTime = 30L;
        long createTime = 0L;
        String crypto = "HmacSHA1";
        String codeDigits = "6";
        String verificationCode = Totp.GenerateVerificationCode(secretBase32, refreshTime, createTime, crypto, codeDigits);
        verificationCode = (StrUtil.isBlank(passwordType) ? "" : new String(verificationCode));
        //System.out.println("verificationCode:" + verificationCode);
        Map<String, Object> urlMap = new HashMap<>();
        String nowTime = Long.toString(System.currentTimeMillis() / 1000);
        //System.out.println("nowTime:"+ DateUtil.now());

        String encrypt = "username=" + paramMap.get("opName")
                + "&&password=" + paramMap.get("opPassword") + verificationCode
                + "&&sign=" + paramMap.get("sign")
                + "&&timestamp=" + nowTime;
        String encryptAfterStr = RSAEncrypt.privateKeyEncryptForGGWPublic(encrypt);
        if (!StrUtil.isBlank(urlType) && urlType.equals("login")) {
            String loginGGWSuffix = encryptAfterStr + "&&command=" + paramMap.get("command").toString() + "&&ip=" + paramMap.get("ip");
            return loginGGWSuffix;
        } else if (!StrUtil.isBlank(urlType) && "execute".equals(urlType)) {
            String executeSuffix = "?ip=" + paramMap.get("ip") + "&&command=" + RSAEncrypt.urlReplace(paramMap.get("command")) + "&&crypto_sign=" + encryptAfterStr;
            return executeSuffix;
        }
        return null;
    }

    /**
     * 执行命令类型
     *
     * @param url
     * @param paramMap
     * @param methodType 执行的方法，登录：login，执行：execute
     * @param log
     * @return
     */
    public static Map<String, String> executeCommandOrLogin(String url, Map<String, String> paramMap, String methodType) {

        Map<String, String> returnMap = new HashMap<>();
        try {
            Charset charset = Charset.forName("utf8");

            String result = HttpUtil.get(url, charset);

            if (StrUtil.isBlank(result)) {
                returnMap.put("error", "error");
                returnMap.put("message", "Login ggwapi return result is null");
                loginNum += 1;
                return returnMap;
            }
            JSONObject resultJsonObject = JSONUtil.parseObj(result);
            int code = resultJsonObject.getInt("code");
            String data = resultJsonObject.getStr("data");
            data = (StringUtils.isEmpty(data)) ? data : data.replace("\\n", "\n");

            if (!StrUtil.isBlank(methodType) && methodType.equals("login")) {
                if (code == 0 || code == 10003) {
                    returnMap.put("pass", "pass");
                    returnMap.put("data", data);
                    Map<String, String> tempIpLastLoginTimeMap = new HashMap<>();
                    tempIpLastLoginTimeMap.put(paramMap.get("sign"), paramMap.get("opName") + ";" + System.currentTimeMillis() / 1000L);
                    ipLastLoginTimeMap.put(paramMap.get("ip"), tempIpLastLoginTimeMap);
                    loginNum = 0;
                    return returnMap;
                } else {
                    returnMap.put("error", "error");
                    returnMap.put("message", "Login ggwapi fail\n" + result);
                    loginNum += 1;
                    return returnMap;
                }
            } else if (!StrUtil.isBlank(methodType) && methodType.equals("execute")) {

                if (code == 0) {
                    Map<String, String> tempIpLastLoginTimeMap = new HashMap<>();
                    tempIpLastLoginTimeMap.put(paramMap.get("sign"), paramMap.get("opName") + ";" + System.currentTimeMillis() / 1000L);
                    ipLastLoginTimeMap.put(paramMap.get("ip"), tempIpLastLoginTimeMap);
                    returnMap.put("pass", "pass");
                    returnMap.put("data", data);
                    loginNum = 0;
                    return returnMap;
                } else {
                    returnMap.put("error", "error");
                    returnMap.put("message", "execute  for ggwapi fail (" + result + ")");
                    loginNum += 1;
                    return returnMap;
                }
            }
            returnMap.put("data", data);
            loginNum = 0;
            return returnMap;
        } catch (Exception e) {
            log.printStackTrace(e);
            returnMap.put("error", "error");
            returnMap.put("message", "Login ggwapi exception,Please contact your administrator");
            loginNum += 1;
            return returnMap;
        }

    }


}
