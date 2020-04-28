package com.cter.job;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ReUtil;
import com.cter.util.BaseLog;
import com.cter.util.LoadPropertiestUtil;
import com.cter.util.SendMailUtil;
import com.cter.util.ggwApi.GgwApi;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Component("AutoCheckPingByHill")
/**
 * hill哥 提出的自动ping的命令
 * 2020/03/27
 */
public class AutoCheckPingByHill {
    BaseLog pingByHillLog = new BaseLog("PingByHillLog");

    public static Map<String, String> map = LoadPropertiestUtil.loadProperties("config/idcEmail.properties");
    public static final String dcSendHost = map.get("dcSendHost");
    public static final String dcEmailPassword = map.get("dcEmailPassword");
    public static final String formEmail = map.get("dcFormEmail");
    public static final String hillEail = map.get("hillEail");
    public static final String host = map.get("host");
    public static final String device_get_url = map.get("device_get_url");
    public static final String GGW_URL = map.get("GGW_URL");
    public static final String LOGIN_GGW_URL = map.get("LOGIN_GGW_URL");
    public static final String GGW_IP = map.get("GGW_IP");
    public static final String GGW_PORT = map.get("GGW_PORT");
    public static final String hillPingFile = map.get("hillPingFile");
    public static   boolean restBoolean=true;//结果是否异常



    public static String tempMethod(int i) {
        return "";
    }

    public static int insertDB(String temp) {
        return 0;
    }

    public static void main(String[] args) throws Exception {

        //求解怎么使用多线程 3分钟完成按顺序执行完程序
        for (int i = 0; i < 10; i++) {
            String temp = tempMethod(i);//3分钟才能返回结果
            insertDB(temp);//插入数据库
        }


        AutoCheckPingByHill byHill = new AutoCheckPingByHill();
        byHill.autoCheckPing();

    }

    class TempFuture implements Callable<String> {

        HashMap<String, String> cityMap = new HashMap<>();
        String destIp;
        String cityType;

        public HashMap<String, String> getCityMap() {
            return cityMap;
        }

        public void setCityMap(HashMap<String, String> cityMap) {
            this.cityMap = cityMap;
        }

        public String getDestIp() {
            return destIp;
        }

        public void setDestIp(String destIp) {
            this.destIp = destIp;
        }

        public String getCityType() {
            return cityType;
        }

        public void setCityType(String cityType) {
            this.cityType = cityType;
        }

        public TempFuture(HashMap<String, String> cityMap, String destIp, String cityType) {
            this.cityMap = cityMap;
            this.destIp = destIp;
            this.cityType = cityType;
        }

        @Override
        public String call() throws Exception {
            return AutoCheckPingByHill.getRestByDestIp(cityMap, destIp, cityType);
        }
    }


    public void autoCheckPing() throws IOException {
//        errorSendEmail(true);
//        errorSendEmail(false);
//        int nowHour=Integer.valueOf(DateUtil.format(new Date(),"HH"));
        int nowHour = 11;
        String file = hillPingFile + "/" + DateUtil.format(new Date(), "yyyy-MM-dd") + "ping测试.xlsx";
        restBoolean=true;
        InputStream ins =null;
        if(nowHour==11){
            ins = LoadPropertiestUtil.class.getClassLoader().getResourceAsStream("config/PING测试.xlsx");//获取输入流
        }else{
            ins = new FileInputStream(file);
        }

//        InputStream ins = new FileInputStream(file);//获取输入流
        Workbook workbook = new XSSFWorkbook(ins);
        XSSFSheet sheet = (XSSFSheet) workbook.getSheetAt(0);
        System.out.println(sheet.getSheetName());
        //开始行结束行
        int minRow = 0;
        int maxRow = 0;
        switch (nowHour) {
            case 11:
                minRow = 1;
                maxRow = 11;
                break;
            case 16:
                minRow = 14;
                maxRow = 24;
                break;
            case 19:
                minRow = 27;
                maxRow = 37;
                break;
            case 22:
                minRow = 40;
                maxRow = 50;
                break;
            default:
        }

        HashMap<String, String> cityMap = getCityIp(sheet, minRow);
        for (int i = minRow + 1; i <= maxRow; i++) {
            String destIp = getIp(sheet.getRow(i).getCell(0).getStringCellValue());//目的ip

            ExecutorService pool = Executors.newFixedThreadPool(3);
            Future<String> BJFuture = pool.submit(new TempFuture(cityMap, destIp, "BJ"));
            ThreadUtil.sleep(10000);
            Future<String> SHFuture = pool.submit(new TempFuture(cityMap, destIp, "SH"));
            ThreadUtil.sleep(10000);
            Future<String> GZFuture = pool.submit(new TempFuture(cityMap, destIp, "GZ"));
            ThreadUtil.sleep(10000);
            pool.shutdown(); // 不允许再想线程池中增加线程
            try {
                System.out.println("开始========" + DateUtil.now() + "=========");
                boolean isFlag = true;
                int num = 1;//循环次数
                while (isFlag) {
                    if (pool.isTerminated() || num == 10) {
                        isFlag = false;
                    } else {
                        ++num;
                        System.out.println("我进来休息了15秒！！！");
                        Thread.sleep(15000);
                    }
                }

                String BJRest = BJFuture.get();
                String SHRest = SHFuture.get();
                String GZRest = GZFuture.get();
                System.out.println("结束========" + DateUtil.now() + "=========");
                CellStyle cellStyle = workbook.createCellStyle();
                Font font1 = workbook.createFont();
                font1.setFontHeightInPoints((short) 11);//设置字体大小
                cellStyle.setFont(font1);//选择需要用到的字体格式
                cellStyle.setAlignment(HSSFCellStyle.ALIGN_LEFT); // 居中
                cellStyle.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND);
                //cellStyle.setFillForegroundColor(IndexedColors.YELLOW.getIndex());  //设置前景色
                cellStyle.setFillForegroundColor(HSSFColor.BRIGHT_GREEN.index);  //设置背景色

                cellStyle.setBorderBottom(HSSFCellStyle.BORDER_THIN); //下边框
                cellStyle.setBorderLeft(HSSFCellStyle.BORDER_THIN);//左边框
                cellStyle.setBorderTop(HSSFCellStyle.BORDER_THIN);//上边框
                cellStyle.setBorderRight(HSSFCellStyle.BORDER_THIN);//右边框

                updateColorByRest(cellStyle, sheet, i, "BJ", getRest(BJRest));
                updateColorByRest(cellStyle, sheet, i, "SH", getRest(SHRest));
                updateColorByRest(cellStyle, sheet, i, "GZ", getRest(GZRest));


            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        FileOutputStream out = new FileOutputStream(file);
        workbook.write(out);
        out.flush();
        out.close();
        sendEmail2Hill();
    }

    /**
     * 根据结果来调整 excel单元格的颜色为
     * 黄色是异常的
     *
     * @param sheet
     * @param row
     * @param cityType
     * @param bjRest
     */
    public synchronized static void updateColorByRest(CellStyle cellStyle, XSSFSheet sheet, int row, String cityType, String rest) {
        Cell cell;
        XSSFRow xssfRow = sheet.getRow(row);
        cellStyle.setFillForegroundColor(HSSFColor.BRIGHT_GREEN.index);  //设置背景色
        rest=rest.replace("F","");
        switch (cityType) {
            case "BJ":
                if (rest.indexOf("1000/1000") == -1 || rest.indexOf("异常") > -1) {
                    restBoolean=false;
                    cellStyle.setFillForegroundColor(HSSFColor.YELLOW.index);  //设置背景色
                }
                cell=xssfRow.getCell((short) 1);
                cell.setCellStyle(cellStyle);
                cell.setCellValue(rest);
                break;
            case "SH":
                if (rest.indexOf("1000/1000") == -1 || rest.indexOf("异常") > -1) {
                    restBoolean=false;
                    cellStyle.setFillForegroundColor(HSSFColor.YELLOW.index);  //设置背景色
                }
                cell=xssfRow.getCell((short) 2);
                cell.setCellStyle(cellStyle);
                cell.setCellValue(rest);
                break;
            case "GZ":
                if (rest.indexOf("1000/1000") == -1 || rest.indexOf("异常") > -1) {
                    restBoolean=false;
                    cellStyle.setFillForegroundColor(HSSFColor.YELLOW.index);  //设置背景色
                }
                cell=xssfRow.getCell((short) 3);
                cell.setCellStyle(cellStyle);
                cell.setCellValue(rest);
                break;
            default:
        }
    }

    /**
     * 根据城市类型 来获取不同结果
     *
     * @param cityMap
     * @param destIp
     * @param cityType
     * @return
     */
    public static String getRestByDestIp(HashMap<String, String> cityMap, String destIp, String cityType) {
        String BJ = cityMap.get("BJ");//CNBEJDEX1004E  ge-0/0/2.0  183.91.156.33
        String SH = cityMap.get("SH");//CNSHHFTX1004E  xe-2/0/3.0  218.97.29.225
        String GZ = cityMap.get("GZ");//CNGUZYUJ1001E  xe-0/2/0.2814 210.5.31.125
        //  ping interface ge-0/0/2.0 source 183.91.156.33 rapid count 1000  【目的IP是最左一列】
        String rest = "";
        switch (cityType) {
            case "BJ":
                String BJCommand = "ping interface ge-0/0/2.0 source 183.91.156.33 rapid count  1000 " + destIp;
                rest = GgwApi.getGGWRestStringByDeviceName("CNBEJDEX1004E", BJCommand);
                System.out.println("CNBEJDEX1004E>" + BJCommand);
                System.out.println(rest);
                return rest;
            case "SH":
                String SHCommand = "ping interface xe-2/0/3.0 source 218.97.29.225 rapid count  1000 " + destIp;
                rest = GgwApi.getGGWRestStringByDeviceName("CNSHHFTX1004E", SHCommand);
                System.out.println("CNSHHFTX1004E>" + SHCommand);
                System.out.println(rest);
                return rest;
            case "GZ":
                String GZCommand = "ping interface xe-0/2/0.2814 source 210.5.31.125 rapid count  1000 " + destIp;
                rest = GgwApi.getGGWRestStringByDeviceName("CNGUZYUJ1001E", GZCommand);
                System.out.println("CNGUZYUJ1001E>" + GZCommand);
                System.out.println(rest);
                return rest;
            default:
                return "异常";
        }
    }

    /**
     * 获取城市ip
     *
     * @return
     */
    public static HashMap<String, String> getCityIp(XSSFSheet sheet, int row) {
        HashMap<String, String> cityMap = new HashMap<>();
        cityMap.put("BJ", getIp(sheet.getRow(row).getCell(1).getStringCellValue()));
        cityMap.put("SH", getIp(sheet.getRow(row).getCell(1).getStringCellValue()));
        cityMap.put("GZ", getIp(sheet.getRow(row).getCell(1).getStringCellValue()));
        return cityMap;
    }

    /**
     * 获取ip
     *
     * @return
     */
    public static String getIp(String content) {
        String regex = "[1-9][0-9]{0,2}\\.([0-9]{1,3}\\.){2}([0-9]{1,3})";
        return ReUtil.get(regex, content, 0).trim();
    }


    /**
     * lossRegex:1000
     * minRegex:39.821
     * packtLossRegex:0%
     * 4ms,0%,1000/1000
     * packtLossRegex
     *
     * @param content
     * @return
     */
    public static String getRest(String content) {
        String rest = null;
        try {
            String lossRegex = "transmitted,\\s\\d{1,4}\\s";
            String minRegex = "[1-9]/[0-9]*\\.[0-9]*";
            String packtLossRegex = ",\\s\\d{1,4}%";
            System.out.println(content);
            String possNum = ReUtil.get(lossRegex, content, 0).replace("transmitted, ", "").trim();
            rest = NumberUtil.roundDown(Double.valueOf(ReUtil.get(minRegex, content, 0).trim().substring(2, ReUtil.get(minRegex, content, 0).trim().length())), 1) + "ms," +
                    ReUtil.get(packtLossRegex, content, 0).replace(", ", "").trim() + "," +
                    possNum + "/1000" + (possNum.equals("1000") ? "" : "F");
        } catch (Exception e) {
            e.printStackTrace();
            return "异常";
        }
//        System.out.println("lossRegex:"+ReUtil.get(lossRegex,content,0).replace("transmitted, ",""));
//        System.out.println("minRegex:"+ReUtil.get(minRegex,content,0).trim().substring(2,ReUtil.get(minRegex,content,0).trim().length()));
//        System.out.println("packtLossRegex:"+ReUtil.get(packtLossRegex,content,0).replace("received, ",""));
        return rest;
    }

    /**
     * 宏检查结果的excel 给hill
     */
    public static void sendEmail2Hill() {
        String subject = "";
        String content = "";
        if (restBoolean) {
            subject = DateUtil.format(new Date(), "yyyy-MM-dd HH:mm") + " Ping结果正常";
            content = DateUtil.format(new Date(), "yyyy-MM-dd HH:mm") + " <br> Ping 正常，详情请查看附件";
        } else {
            subject = DateUtil.format(new Date(), "yyyy-MM-dd HH:mm") + " Ping结果异常";
            content = DateUtil.format(new Date(), "yyyy-MM-dd HH:mm") + " <br> Ping 存在异常，详情请查看附件";
        }
        SendMailUtil mailUtil = SendMailUtil.getInstance();
        String to[] = hillEail.split(";");//收件人的地址
        String cs[] = null;
        String ms[] = null;
        String file = hillPingFile + "/" + DateUtil.format(new Date(), "yyyy-MM-dd") + "ping测试.xlsx";
        String fromEmail = formEmail;//发件人的地址
        String[] fileList = {file};
        try {
            mailUtil.send(to, cs, ms, subject, content, formEmail, fileList, host, "");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
