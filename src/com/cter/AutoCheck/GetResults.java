package com.cter.AutoCheck;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import com.cter.util.BaseLog;
import com.cter.util.LoadPropertiestUtil;
import com.cter.util.TempDBUtils;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

/**
 * 调用 接口获取结果集
 */
public class GetResults {

    private static Map<String, String> otherMap = LoadPropertiestUtil.loadProperties("config/other.properties");
    private static String remedyDevHostIP = otherMap.get("remedyDevHostIP");
    static BaseLog CSCheckLog = new BaseLog("CSCheckLog");
    static BaseLog caseIdLog = new BaseLog("CSCaseIDLog");


    //    static String url = "http://10.180.5.189:8089";
    static String url = "http://10.181.160.4:8089";

    //用于记录正在执行的 caseID Map
    public static HashMap<String, String> caseIdMap = new HashMap<>();

    /**
     * 骨干获取结果
     *
     * @param trunkName
     * @return
     */
    public static String trunkProblem(String trunkName) {
        String problem = "backbone_problem";
        String trunkUrl = url + "/" + problem;
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("trunkName", trunkName);
        String results = HttpUtil.get(trunkUrl, paramMap);
        return results;
    }


    /**
     * 单线丢包问题
     *
     * @param map
     * @return
     */
    public static String packetLossProblem(String interfaceName, String pe, String vrf) {
        String problem = "loss_local";
        //http://10.181.160.4:8089/loss_local?interface=e1-0/2/0:7&pe=CNDALDOR1001E&vrf=
        String tempUrl = url + "/" + problem + "?interface=" + interfaceName + "&pe=" + pe + "&vrf=" + vrf;
        String results = HttpUtil.get(tempUrl);
        return results;
    }


    /**
     * 单Internet Unreachable 互联网不能互访本地或外地网络 问题
     *
     * @param map
     * @return
     */
    public static String internetProblem(HashMap<String, String> paramMap) {
        String problem = "internet_unreachable_internet";
        //http://10.180.5.189:8089/internet_unreachable_internet?interface=ge-0/1/0.1114&pe=CNSUZCYY1004E&vrf=229466370&site_id=TW4663739157SuZ&prvoisioning_partner=CEC-CN&pe_wan_ip=10.117.238.133&dst_ip=8.8.8.8
        String tempUrl = url + "/" + problem + "?interface=" + paramMap.get("interfaceName") + "&pe=" + paramMap.get("pe") + "&vrf=" + paramMap.get("vrf")+ "&site_id=" + paramMap.get("site_id")+ "&prvoisioning_partner=" + paramMap.get("prvoisioning_partner")+ "&pe_wan_ip=" + paramMap.get("pe_wan_ip")+ "&dst_ip=" + paramMap.get("dst_ip");
        String results = HttpUtil.get(tempUrl);
        return results;
    }





    /**
     * AB丢包问题
     *
     * @param map
     * @return
     */
    public static String abPacketLossProblem(String interfaceName1, String pe1, String vrf1, String peWan1, String interfaceName2, String pe2, String vrf2, String peWan2,String destIP) {
        String problem = "ab_loss_site_site";
//http://10.180.5.189:8089/ab_loss_site_site?interface1=ge-0/1/0.605&pe1=CNSHHSJG1001E&vrf1=211374640&interface2=ge-0/3/0.1152&pe2=HKHKGCTT1001E&vrf2=108374640&pe_wan1=10.114.109.177&pe_wan2=10.114.110.121
        String tempUrl = url + "/" + problem + "?pe1=" + pe1 + "&interface1=" + interfaceName1 + "&vrf1=" + vrf1 + "&pe_wan1=" + peWan1 +
                "&pe2=" + pe2 + "&interface2=" + interfaceName2 + "&vrf2=" + vrf2 + "&pe_wan2=" + peWan2+ "&destIP=" + destIP;;
        String results = HttpUtil.get(tempUrl);
        return results;
    }

    /**
     * 获取搜索在 待处理的case_id
     * @param caseView
     * @return
     */
    public static HashMap<String ,String> getCaseLogIds(){

        String username="root";
        String password="root1234root";
        String url="jdbc:log4jdbc:mysql://210.5.3.30:3306/empower?characterEncoding=utf-8";
        String driver="net.sf.log4jdbc.DriverSpy";
        TempDBUtils tempDBUtils=new TempDBUtils(username,password,driver,url);
        HashMap<String ,String> dbCaseIdMap=new HashMap<>();
        String sql = "select DISTINCT(case_id) as  case_id from  case_log where STR_TO_DATE(insert_time,'%Y-%m-%d %k:%i:%s')>\n" +
                "DATE_SUB(now(), Interval 20 minute) ";
        List<Object> params = new ArrayList<>();

        List<Map<String, Object>>  list = new LinkedList<Map<String, Object>>();
        try {
            list=tempDBUtils.executeQueryCE(sql,params);
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            tempDBUtils.closeDB();
        }
        for (Map<String, Object> map : list) {
            dbCaseIdMap.put(map.get("case_id").toString(),map.get("case_id").toString());
        }
        return dbCaseIdMap;
    }


    /**
     * 根据caseView 插入到日志表
     * @param caseView
     * @return
     */
    public static int insertCaseLog(CaseView caseView){

        String username="root";
        String password="root1234root";
        String url="jdbc:log4jdbc:mysql://210.5.3.30:3306/empower?characterEncoding=utf-8";
        String driver="net.sf.log4jdbc.DriverSpy";
        TempDBUtils tempDBUtils=new TempDBUtils(username,password,driver,url);

        String sql = "INSERT INTO `case_log`(`case_id`,  `web_item`, `site_id`,`insert_data`, `rest`,  `insert_time`, `update_time`) VALUES " +
                "(?,?,?,?,?,?,?)";
        List<Object> params = new ArrayList<>();

        params.add(caseView.getCaseId());
        params.add(caseView.getWebItem());
        params.add(caseView.getSiteId());
        params.add(JSONUtil.toJsonStr(caseView));
        params.add("待处理");
        params.add(DateUtil.now());
        params.add(DateUtil.now());
        int i=0;
        try {
            i=tempDBUtils.executeUpdate(sql,params);
        } catch (SQLException e) {
            e.printStackTrace();
        }finally {
            tempDBUtils.closeDB();
        }
        return i;
    }



    /**
     * 插入到workLog
     *
     * @param results 结果
     * @param caseId  case编号
     * @return
     */
    public static int insertWorkLog(String results, String caseId, Connection conn) {
        String rest="正常";

        List<Object> params = new ArrayList<>();
        String summary = resultsGetSummary(results);
        CSCheckLog.info("caseId:" + caseId);
        CSCheckLog.info("summary\n<<<<<" + summary + ">>>>>");
        CSCheckLog.info("results\n<<<<<" + results + ">>>>>");
        if (results.equals("Work Log")){
            summary=results;
            caseIdLog.info("caseId  (" + caseId + ") " + results);
        } else if (results.indexOf("is null") > -1 || summary.equals("param is null")) {
            summary = results;
            caseIdLog.info("caseId  (" + caseId + ") " + results);
        } else if (results.indexOf("GGWAPI cannot connect") > -1) {
            summary = "ggwAPI Abnormal connection, unable to automatically query line status";
            results = "ggwAPI Abnormal connection, unable to automatically query line status";
            caseIdLog.info("caseId  (" + caseId + ")  查询返回为空 异常");
        } else if (summary.equals("Automatic query line status ggwpapi return parameter exception, please check and contact the developer to check the parameters")) {
            rest="异常";
            results = summary;
            caseIdLog.info("caseId  (" + caseId + ") " + results);
        }
        String sql = "declare\n" +
                "results clob := ?;  \n" +
                "summary clob := ?;  \n" +
                "begin\n" +
                " INSERT INTO TIDESTONE.CPCTIDES_HELPDESK_TICKET (HPD_CASE_ID, SYS_Create_User,TEMP_WORKLOG,TEST_RESULT,CS_FEEDBACK,NOC_FEEDBACK) " +
                "VALUES (?,'Auto_Check_Type',summary,results,'0','0');\n" +
                "end;";
        params.add(results);
        params.add(summary);
        params.add(caseId);
        int i = OracleDbUtil.executeUpdate(sql, params, conn);


        String username="root";
        String password="root1234root";
        String url="jdbc:log4jdbc:mysql://210.5.3.30:3306/empower?characterEncoding=utf-8";
        String driver="net.sf.log4jdbc.DriverSpy";
        TempDBUtils tempDBUtils=new TempDBUtils(username,password,driver,url);
        String updateSql = "UPDATE `case_log` SET `rest` = ?, `summary` = ?, `work_log` = ?, `update_time` = ? WHERE `case_id` = ?";
        params=new ArrayList<Object>();

        if(summary.indexOf("为空")>-1){
            rest="线路参数不全";
        }else if(summary.indexOf("Work Log")>-1){
            rest="未处理类型";
        }else  if(summary.indexOf("异常")>-1&&summary.indexOf("+++++")==-1){
            rest="异常";
        }

        params.add(rest);
        params.add(summary);
        params.add(results);
        params.add(DateUtil.now());
        params.add(caseId);
        try {
            tempDBUtils.executeUpdate(updateSql,params);
        } catch (SQLException e) {
            e.printStackTrace();
        }finally {
            tempDBUtils.closeDB();
        }

        return i;
    }

    /**
     * 根据骨干名称和 caseId 更新数据库
     *
     * @param trunkName
     * @param caseId
     * @return
     */
    public static int trunkDispose(String trunkName, String caseId, int connTotal) {
        String results = "";
        if (StrUtil.isBlank(trunkName)) {
            results = "trunk Can not be empty";
            Connection conn = OracleDbUtil.getRemedyProConnection();
            int i = insertWorkLog(results, caseId, conn);
            OracleDbUtil.closeConnection(conn);
            GetResults.caseIdMap.remove(caseId);
            return i;
        }
        try {
            results = trunkProblem(trunkName);
            if (results.length() < 100) {
                if (connTotal == 4) {
                    results = "GGWAPI cannot connect" +
                            "Auto check  faled,Please check manually";
                    Connection conn = OracleDbUtil.getRemedyProConnection();
                    int i = insertWorkLog(results, caseId, conn);
                    GetResults.caseIdMap.remove(caseId);
                    return 0;
                } else {
                    return trunkDispose(trunkName, caseId, connTotal + 1);
                }
            } else {
                Connection conn = OracleDbUtil.getRemedyProConnection();
                int i = insertWorkLog(results, caseId, conn);
                OracleDbUtil.closeConnection(conn);
                GetResults.caseIdMap.remove(caseId);
                return i;
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (connTotal == 4) {
                results = "GGWAPI cannot connect" +
                        "Auto check  faled,Please check manually";
                Connection conn = OracleDbUtil.getRemedyProConnection();
                int i = insertWorkLog(results, caseId, conn);
                GetResults.caseIdMap.remove(caseId);
                return 0;
            } else {
                return trunkDispose(trunkName, caseId, connTotal + 1);
            }
        }
    }

    /**
     * Internet Unreachable 互联网不能互访本地或外地网络  更新数据库
     *
     * @param caseId
     * @return
     */
    public static int internetDispose(HashMap<String, String> paramMap, String caseId, int connTotal) {
        String results = "";
        String interfaceName = paramMap.get("interfaceName");
        String pe = paramMap.get("pe");
        String vrf = paramMap.get("vrf");
        if (StrUtil.isBlank(interfaceName) || StrUtil.isBlank(pe)  ) {
            results = "According to the route site_id query interface, PE has one or more items empty";
            Connection conn = OracleDbUtil.getRemedyProConnection();
            int i = insertWorkLog(results, caseId, conn);
            GetResults.caseIdMap.remove(caseId);
            OracleDbUtil.closeConnection(conn);
            return i;
        }
        try {

            results = internetProblem(paramMap);
            if (results.length() < 100) {
                if (connTotal == 4) {
                    results = "GGWAPI cannot connect" +
                            "Auto check  faled,Please check manually";
                    Connection conn = OracleDbUtil.getRemedyProConnection();
                    int i = insertWorkLog(results, caseId, conn);
                    GetResults.caseIdMap.remove(caseId);
                    return 0;
                } else {
                    return internetDispose(paramMap, caseId, connTotal + 1);
                }
            } else {
                Connection conn = OracleDbUtil.getRemedyProConnection();
                int i = insertWorkLog(results, caseId, conn);
                GetResults.caseIdMap.remove(caseId);
                return i;
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (connTotal == 4) {
                results = "GGWAPI cannot connect" +
                        "Auto check  faled,Please check manually";
                Connection conn = OracleDbUtil.getRemedyProConnection();
                int i = insertWorkLog(results, caseId, conn);
                GetResults.caseIdMap.remove(caseId);
                return 0;
            } else {
                return internetDispose(paramMap, caseId, connTotal + 1);
            }
        }
    }


    /**
     * 根据丢包 单线 和 caseId 更新数据库
     *
     * @param caseId
     * @return
     */
    public static int packetLossDispose(HashMap<String, String> paramMap, String caseId, int connTotal) {
        String results = "";
        String interfaceName = paramMap.get("interfaceName");
        String pe = paramMap.get("pe");
        String vrf = paramMap.get("vrf");
        if (StrUtil.isBlank(interfaceName) || StrUtil.isBlank(pe)  ) {
            results = "According to the route site_id query interface, PE has one or more items empty";
            Connection conn = OracleDbUtil.getRemedyProConnection();
            int i = insertWorkLog(results, caseId, conn);
            GetResults.caseIdMap.remove(caseId);
            OracleDbUtil.closeConnection(conn);
            return i;
        }
        try {
            results = packetLossProblem(interfaceName, pe, vrf);
            if (results.length() < 100) {
                if (connTotal == 4) {
                    results = "GGWAPI cannot connect" +
                            "Auto check  faled,Please check manually";
                    Connection conn = OracleDbUtil.getRemedyProConnection();
                    int i = insertWorkLog(results, caseId, conn);
                    GetResults.caseIdMap.remove(caseId);
                    return 0;
                } else {
                    return packetLossDispose(paramMap, caseId, connTotal + 1);
                }
            } else {
                Connection conn = OracleDbUtil.getRemedyProConnection();
                int i = insertWorkLog(results, caseId, conn);
                GetResults.caseIdMap.remove(caseId);
                return i;
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (connTotal == 4) {
                results = "GGWAPI cannot connect" +
                        "Auto check  faled,Please check manually";
                Connection conn = OracleDbUtil.getRemedyProConnection();
                int i = insertWorkLog(results, caseId, conn);
                GetResults.caseIdMap.remove(caseId);
                return 0;
            } else {
                return packetLossDispose(paramMap, caseId, connTotal + 1);
            }
        }
    }




    /**
     * ab丢包 和 caseId 更新数据库
     *
     * @param trunkName
     * @param caseId
     * @return
     */
    public static int abPacketLossDispose(HashMap<String, String> paramMap, String caseId, int connTotal) {
        String results = "";
        String interfaceName1 = paramMap.get("interfaceName1");
        String pe1 = paramMap.get("pe1");
        String vrf1 = paramMap.get("vrf1");
        String peWan1 = paramMap.get("peWan1");
        String interfaceName2 = paramMap.get("interfaceName2");
        String pe2 = paramMap.get("pe2");
        String vrf2 = paramMap.get("vrf2");
        String peWan2 = paramMap.get("peWan2");
        if (StrUtil.isBlank(interfaceName1) || StrUtil.isBlank(pe1)  || StrUtil.isBlank(peWan1) ||
                StrUtil.isBlank(interfaceName2) || StrUtil.isBlank(pe2) || StrUtil.isBlank(peWan2)) {
            results = "According to the route site_id query interface, PE has one or more items empty";
            Connection conn = OracleDbUtil.getRemedyProConnection();
            int i = insertWorkLog(results, caseId, conn);
            OracleDbUtil.closeConnection(conn);
            GetResults.caseIdMap.remove(caseId);
            return i;
        }
        try {
            results = abPacketLossProblem(interfaceName1, pe1, vrf1, peWan1, interfaceName2, pe2, vrf2, peWan2,paramMap.get("destIP"));
            if (results.length() < 100) {
                if (connTotal == 4) {
                    results = "GGWAPI cannot connect" +
                            "Auto check  faled,Please check manually";
                    Connection conn = OracleDbUtil.getRemedyProConnection();
                    int i = insertWorkLog(results, caseId, conn);
                    GetResults.caseIdMap.remove(caseId);
                    return 0;
                } else {
                    return abPacketLossDispose(paramMap, caseId, connTotal + 1);
                }
            } else {
                Connection conn = OracleDbUtil.getRemedyProConnection();
                int i = insertWorkLog(results, caseId, conn);
                GetResults.caseIdMap.remove(caseId);
                return i;
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (connTotal == 4) {
                results = "GGWAPI cannot connect" +
                        "Auto check  faled,Please check manually";
                Connection conn = OracleDbUtil.getRemedyProConnection();
                int i = insertWorkLog(results, caseId, conn);
                GetResults.caseIdMap.remove(caseId);
                return 0;
            } else {
                return abPacketLossDispose(paramMap, caseId, connTotal + 1);
            }
        }
    }

    /**
     * 从视图中获取所有的case
     */
    public static void getAllCaseByView() {
        String sql = "select * from  aradmin.HPD_HELPDESK_ACT_VIEW where status=1   order by case_id desc";
        Connection con = OracleDbUtil.getRemedyProConnection();
        List<TreeMap<String, Object>> list = OracleDbUtil.executeQueryMap(sql, null, con);
        //丢包
        HashMap<String, ArrayList<CaseView>> repeatMap = new HashMap<>();//用于记录重复的site Case Map
        HashMap<String, CaseView> onlyOneMap = new HashMap<>();//用于记录只有一条site的 Case Map
        List<CaseView> trunkList = new ArrayList<>();//骨干类型的
        List<CaseView> errorList = new ArrayList<>();//没有 pe 或者interface
        List<CaseView> otherList = new ArrayList<>();//未处理


        HashMap<String, String> caseLogIds = getCaseLogIds();

        for (int i = 0; i < list.size(); i++) {
            TreeMap<String, Object> tempMap = list.get(i);
            String caseId = tempMap.get("CASE_ID").toString();
            if (!StrUtil.isBlank(caseLogIds.get(caseId))) {//如果是重复的caseId,之前没有处理完 跳过
                continue;
            } else {//不存在重复
                GetResults.caseIdMap.put(caseId, caseId);
            }
            CaseView caseView = new CaseView();
            caseView.setCaseId(tempMap.get("CASE_ID").toString());
            caseView.setPeName(tempMap.get("PE_ROUTER").toString());
            caseView.setInterfaceName(tempMap.get("PE_PORT_INTERFACE").toString());
            caseView.setVrf(StrUtil.isBlank(tempMap.get("VRF").toString())?"":tempMap.get("VRF").toString());
            caseView.setCaseSummary(tempMap.get("SUMMARY").toString());
            caseView.setWebType(tempMap.get("Type").toString());
            caseView.setWebItem(tempMap.get("item").toString());
            caseView.setCeWanIp(tempMap.get("CE_WAN_IP").toString());
            caseView.setPeWanIp(tempMap.get("PE_WAN_IP").toString());
            caseView.setSiteId(tempMap.get("SITE_ID").toString());
            caseView.setStatus(tempMap.get("STATUS").toString());
            caseView.setDestinationIp(tempMap.get("DESTINATION_IP").toString());
            caseView.setSourceIp(tempMap.get("SOURCE_IP").toString());
            caseView.setTrunkName(tempMap.get("Trunk_Name").toString());
            caseView.setPrvoisioningPartner(StrUtil.isBlank(tempMap.get("PRVOISIONING_PARTNER").toString())?"":tempMap.get("PRVOISIONING_PARTNER").toString());
            HashMap<String, String> items = new HashMap<>();
            items.put("Internet Unreachable 互联网不能互访本地或外地网络","Internet Unreachable 互联网不能互访本地或外地网络");
            items.put("Intranet Unreachable 公司内部网不能互访","Intranet Unreachable 公司内部网不能互访");
            items.put("Other  其它","Other  其它");
            items.put("Routing issue/Config issue 路由问题/配置问题","Routing issue/Config issue 路由问题/配置问题");
            items.put("Inquiry RFO/Historical log 查询平台/系统/历史告警/原因","Inquiry RFO/Historical log 查询平台/系统/历史告警/原因");
            items.put("Circuit/Service latency / Packet loss / Unstable 线路/服务延迟/ 丢包/ 不稳定","Circuit/Service latency / Packet loss / Unstable 线路/服务延迟/ 丢包/ 不稳定");
            items.put("Circuit/Service down (Current) 线路/服务中断","Circuit/Service down (Current) 线路/服务中断");
            items.put("Low Utilization","Low Utilization");
            items.put("Virtual Network (VM Network)","Virtual Network (VM Network)");
            HashMap<String, String> types = new HashMap<>();
            types.put("General Inquiry  一般查询","General Inquiry  一般查询");
            types.put("BGP","BGP");
            types.put("Voice /  VC","Voice /  VC");



            if (caseView.getWebType().indexOf("Core Network") > -1) {//骨干类型
                trunkList.add(caseView);
            } else if (!StrUtil.isBlank(caseView.getPeName())&&!StrUtil.isBlank(caseView.getInterfaceName())) {//丢包等类型
                boolean site_Id_12_contrast=false;
                try {
                    site_Id_12_contrast= onlyOneMap.get(caseView.getCaseId()).getSiteId().equals(caseView.getSiteId());//site1 site2 对比
                } catch (Exception e) {

                }
                if (onlyOneMap.containsKey(caseView.getCaseId())&&!site_Id_12_contrast) {//如果有多条A-B丢包,再根据是否存在dstIp 区分(site2site Packet loss)(site-dst Packet loss)
                    ArrayList<CaseView> tempList = new ArrayList<>();
                    tempList.add(onlyOneMap.get(caseView.getCaseId()));
                    tempList.add(caseView);
                    onlyOneMap.remove(caseView.getCaseId());
                    repeatMap.put(caseView.getCaseId(), tempList);
                } else {//一条 然后再根据是否存在dstIp 区分(Local packet loss)()
                    onlyOneMap.put(caseView.getCaseId(), caseView);
                }
            }else if(StrUtil.isBlank(caseView.getPeName())||StrUtil.isBlank(caseView.getInterfaceName())){
                errorList.add(caseView);
            }else {

            }
        }
        OracleDbUtil.closeConnection(con);
        for (int i = 0; i < trunkList.size(); i++) {//骨干类型
            CaseView caseView = trunkList.get(i);
            TrunkManage trunkManage = new TrunkManage(caseView);
            trunkManage.start();
            ThreadUtil.sleep(2000);
        }

        for (String caseId : onlyOneMap.keySet()) {//单线丢包/单线丢包对端监测/路由问题配置问题/Inquiry RFO Historical log/互联网不能互访本地或外地网络

            CaseView caseView = onlyOneMap.get(caseId);
            OnlyOneSiteManage onlyOneSiteManage = new OnlyOneSiteManage(caseView);
            onlyOneSiteManage.start();
            ThreadUtil.sleep(2000);


        }

        for (String caseId : repeatMap.keySet()) {//a-b丢包/互联网不能互访本地或外地网络/公司内部网不能互访
            ArrayList<CaseView> caseViews = repeatMap.get(caseId);
            ABSiteManage abSiteManage = new ABSiteManage(caseViews);
            abSiteManage.start();
            ThreadUtil.sleep(2000);
        }

        for (int i = 0; i < errorList.size(); i++) {//错误
            CaseView caseView=errorList.get(i);
            String results = "According to the route site_id query interface, PE has one or more items empty";
            Connection conn = OracleDbUtil.getRemedyProConnection();
            int t = insertWorkLog(results, caseView.getCaseId(), conn);
            GetResults.caseIdMap.remove(caseView.getCaseId());
            OracleDbUtil.closeConnection(conn);
        }

        for (int i = 0; i < otherList.size(); i++) {//其他
            CaseView caseView=otherList.get(i);
            ClearOtherManage clearOtherManage=new ClearOtherManage(caseView);
            clearOtherManage.start();
            ThreadUtil.sleep(2000);
        }
        caseIdLog.info("repeatMap:" + JSONUtil.toJsonStr(repeatMap));
        caseIdLog.info("onlyOneMap:" + JSONUtil.toJsonStr(onlyOneMap));
        caseIdLog.info("trunkList" + JSONUtil.toJsonStr(trunkList));
        caseIdLog.info("errorList" + JSONUtil.toJsonStr(errorList));


    }

    /**
     * 判断是不是存在于 之前正在执行的 Case
     *
     * @param caseId
     * @return
     */
    public static HashMap<String, String> checkIsInCaseIdMap(List<TreeMap<String, Object>> list) {

        HashMap<String, String> repeatCaseIdMap = new HashMap<>();
        for (int i = 0; i < list.size(); i++) {
            TreeMap<String, Object> tempMap = list.get(i);
            String caseId = tempMap.get("CASE_ID").toString();
            if (!StrUtil.isBlank(GetResults.caseIdMap.get(caseId))) {
                repeatCaseIdMap.put(caseId, caseId);
            }
        }
        return repeatCaseIdMap;

    }

    /**
     * 测试方法类
     */
    public static void testMain() {
        GetResults getResults = new GetResults();
        String caseId = "HD0000002828188";//case编号
        int connTotal = 1;//连接ggw次数


        //骨干获取结果
        String trunkName = "CNGUZYUJ1001C.ge-11/0/8.50-CNGUZYUJ1004E.ge-0/0/0.50";
//        System.out.println(trunkProblem(trunkName));
//        System.out.println( GetResults.trunkDispose(trunkName,caseId,connTotal));


        //闪断获取结果
//        http://10.181.160.4:8089/flapping_problem?interface=ge-0/1/2.303&pe=CNSHYCXL1001E&vrf=373449250
        HashMap<String, String> paramMap = new HashMap<>();
        String interfaceName = "ge-0/1/2.303";
        String pe = "CNSHYCXL1001E";
        String vrf = "373449250";
        paramMap.put("interfaceName", interfaceName);
        paramMap.put("pe", pe);
        paramMap.put("vrf", vrf);
//        System.out.println(flappingProblem(interfaceName,pe,vrf));
//        System.out.println(GetResults.flappingDispose(paramMap,caseId,connTotal));


        //a-b丢包
        String interfaceName1 = "ge-0/1/0.666";
        String pe1 = "CNSHHSJG1001E";
        String vrf1 = "211450800";
        String pewan1 = "10.117.120.145";
        String interfaceName2 = "CNSHHFTX1001E";
        String pe2 = "ge-1/0/3.318";
        String vrf2 = "213450800";
        String pewan2 = "10.117.137.77";
        paramMap.put("interfaceName1", interfaceName1);
        paramMap.put("pe1", pe1);
        paramMap.put("vrf1", vrf1);
        paramMap.put("peWan1", pewan1);
        paramMap.put("interfaceName2", interfaceName2);
        paramMap.put("pe2", pe2);
        paramMap.put("vrf2", vrf2);
        paramMap.put("peWan2", pewan2);
//        System.out.println("case:"+caseId+"\ttype:丢包"+"\t"+GetResults.abPacketLossDispose(paramMap,caseId,connTotal));

    }


    /**
     * 截取结果的简要信息
     *
     * @param results
     * @return
     */
    public static String resultsGetSummary(String results) {
        if (StrUtil.isBlank(results)) {
            return "参数为空";
        } else {
            String summary = null;
            if (results.indexOf("====================================================================================================") == -1) {
                if(results.equals("According to the route site_id query interface, PE has one or more items empty")){
                    return results;
                }
                summary = "Automatic query line status ggwpapi return parameter exception, please check and contact the developer to check the parameters";
                return summary;
            } else {
                summary = results.substring(0, results.indexOf("===================================================================================================="));
                return summary;
            }
        }


    }



    public static void main(String[] args) {
        getAllCaseByView();
    }

}
