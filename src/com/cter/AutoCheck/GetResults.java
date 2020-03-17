package com.cter.AutoCheck;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import com.cter.util.BaseLog;
import com.cter.util.LoadPropertiestUtil;

import java.sql.Connection;
import java.util.*;

/**
 * ���� �ӿڻ�ȡ�����
 */
public class GetResults {

    private static Map<String, String> otherMap = LoadPropertiestUtil.loadProperties("config/other.properties");
    private static String remedyDevHostIP = otherMap.get("remedyDevHostIP");
    static BaseLog CSCheckLog = new BaseLog("CSCheckLog");
    static BaseLog caseIdLog = new BaseLog("CSCaseIDLog");


    //    static String url = "http://10.180.5.189:8089";
    static String url = "http://10.181.160.4:8089";

    //���ڼ�¼����ִ�е� caseID Map
    public static HashMap<String, String> caseIdMap = new HashMap<>();

    /**
     * �Ǹɻ�ȡ���
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
     * ��������
     *
     * @param map
     * @return
     */
    public static String flappingProblem(String interfaceName, String pe, String vrf) {
        String problem = "flapping_problem";
//        http://10.180.5.189:8089/flapping_problem?interface=ge-0/1/0.605&pe=CNSHHSJG1001E&vrf=211374640
        String tempUrl = url + "/" + problem + "?interface=" + interfaceName + "&pe=" + pe + "&vrf=" + vrf;
        String results = HttpUtil.get(tempUrl);
        return results;
    }

    /**
     * ���߶�������
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
     * ���߶��� �Զ�
     * ab_loss_internet
     *
     * @param map
     * @return
     */
    public static String packetLossDestinationProblem(String interfaceName, String pe, String ceWanIp, String peWanIp, String dstIP) {
        String problem = "loss_dstip";
        //        http://10.181.160.4:8089/loss_dstip?interface=ge-0/1/0.605&pe=CNSHHSJG1001E&vrf=&ce_wan_ip=10.114.109.178&pe_wan_ip=10.114.109.177&dst_ip=8.8.8.8
        String tempUrl = url + "/" + problem + "?interface=" + interfaceName + "&pe=" + pe + "&ce_wan_ip=" + ceWanIp + "&pe_wan_ip=" + peWanIp + "&dst_ip=" + dstIP;
        String results = HttpUtil.get(tempUrl);
        return results;
    }

    /**
     * AB��������
     *
     * @param map
     * @return
     */
    public static String abPacketLossProblem(String interfaceName1, String pe1, String vrf1, String peWan1, String interfaceName2, String pe2, String vrf2, String peWan2) {
        String problem = "ab_loss_site_site";
//http://10.180.5.189:8089/ab_loss_site_site?interface1=ge-0/1/0.605&pe1=CNSHHSJG1001E&vrf1=211374640&interface2=ge-0/3/0.1152&pe2=HKHKGCTT1001E&vrf2=108374640&pe_wan1=10.114.109.177&pe_wan2=10.114.110.121
        String tempUrl = url + "/" + problem + "?pe1=" + pe1 + "&interface1=" + interfaceName1 + "&vrf1=" + vrf1 + "&pe_wan1=" + peWan1 +
                "&pe2=" + pe2 + "&interface2=" + interfaceName2 + "&vrf2=" + vrf2 + "&pe_wan2=" + peWan2;
        String results = HttpUtil.get(tempUrl);
        return results;
    }

    /**
     * ���뵽workLog
     *
     * @param results ���
     * @param caseId  case���
     * @return
     */
    public static int insertWorkLog(String results, String caseId, Connection conn) {
        List<Object> params = new ArrayList<>();
        String summary = resultsGetSummary(results);
        CSCheckLog.info("caseId:" + caseId);
        CSCheckLog.info("summary\n<<<<<" + summary + ">>>>>");
        CSCheckLog.info("results\n<<<<<" + results + ">>>>>");
        if (results.equals("Work Log")){
            summary=results;
            caseIdLog.info("caseId  (" + caseId + ") " + results);
        } else if (results.indexOf("Ϊ��") > -1 || summary.equals("����Ϊ��")) {
            summary = results;
            caseIdLog.info("caseId  (" + caseId + ") " + results);
        } else if (results.indexOf("GGWAPI cannot connect") > -1) {
            summary = "ggwAPI �����쳣���޷������Զ���ѯ ��·״̬";
            results = "ggwAPI �����쳣���޷������Զ���ѯ ��·״̬";
            caseIdLog.info("caseId  (" + caseId + ")  ��ѯ����Ϊ�� �쳣");
        } else if (summary.equals("�Զ���ѯ ��·״̬ ggwAPI ���ز����쳣��������ϵ������Ա������ ")) {
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
        return i;
    }

    /**
     * ���ݹǸ����ƺ� caseId �������ݿ�
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
     * ����������Ϣ �� caseId �������ݿ�
     *
     * @param caseId
     * @return
     */
    public static int flappingDispose(HashMap<String, String> paramMap, String caseId, int connTotal) {
        String results = "";
        String interfaceName = paramMap.get("interfaceName");
        String pe = paramMap.get("pe");
        String vrf = paramMap.get("vrf");
        if (StrUtil.isBlank(interfaceName) || StrUtil.isBlank(pe) || StrUtil.isBlank(vrf)) {
            results = "������·site_id ��ѯ�� interface��pe,vrf ��һ����߶���Ϊ��";
            Connection conn = OracleDbUtil.getRemedyProConnection();
            int i = insertWorkLog(results, caseId, conn);
            OracleDbUtil.closeConnection(conn);
            GetResults.caseIdMap.remove(caseId);
            return i;
        }
        try {
            results = flappingProblem(interfaceName, pe, vrf);
            if (results.length() < 100) {
                if (connTotal == 4) {
                    results = "GGWAPI cannot connect" + "Auto check  faled,Please check manually";
                    Connection conn = OracleDbUtil.getRemedyProConnection();
                    int i = insertWorkLog(results, caseId, conn);
                    GetResults.caseIdMap.remove(caseId);
                    return 0;
                } else {
                    return flappingDispose(paramMap, caseId, connTotal + 1);
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
                results = "GGWAPI cannot connect" + "Auto check  faled,Please check manually";
                Connection conn = OracleDbUtil.getRemedyProConnection();
                int i = insertWorkLog(results, caseId, conn);
                GetResults.caseIdMap.remove(caseId);
                return 0;
            } else {
                return flappingDispose(paramMap, caseId, connTotal + 1);
            }
        }
    }

    /**
     * ���ݶ��� ���� �� caseId �������ݿ�
     *
     * @param caseId
     * @return
     */
    public static int packetLossDispose(HashMap<String, String> paramMap, String caseId, int connTotal) {
        String results = "";
        String interfaceName = paramMap.get("interfaceName");
        String pe = paramMap.get("pe");
        String vrf = paramMap.get("vrf");
        if (StrUtil.isBlank(interfaceName) || StrUtil.isBlank(pe) || StrUtil.isBlank(vrf)) {
            results = "������·site_id ��ѯ�� interface��pe,vrf ��һ����߶���Ϊ��";
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
     * ���߶��� �Զ�ip
     *
     * @param caseId
     * @return
     */
    public static int packetLossDestinationDispose(HashMap<String, String> paramMap, String caseId, int connTotal) {
        String results = "";
        String interfaceName = paramMap.get("interfaceName");
        String pe = paramMap.get("pe");
        String dstIP = paramMap.get("dstIP");
        String ceWanIp = paramMap.get("ceWanIp");
        String peWanIp = paramMap.get("peWanIp");

        if (StrUtil.isBlank(interfaceName) || StrUtil.isBlank(pe) || StrUtil.isBlank(ceWanIp) || StrUtil.isBlank(peWanIp)) {
            results = "������·site_id ��ѯ�� interface,pe,ceWanIp,peWanIp ��һ����߶���Ϊ��";
            Connection conn = OracleDbUtil.getRemedyProConnection();
            int i = insertWorkLog(results, caseId, conn);
            OracleDbUtil.closeConnection(conn);
            GetResults.caseIdMap.remove(caseId);
            return i;
        }
        try {
            results = packetLossDestinationProblem(interfaceName, pe, ceWanIp, peWanIp, dstIP);
            if (results.length() < 100) {
                if (connTotal == 4) {
                    results = "GGWAPI cannot connect" +
                            "Auto check  faled,Please check manually";
                    Connection conn = OracleDbUtil.getRemedyProConnection();
                    int i = insertWorkLog(results, caseId, conn);
                    GetResults.caseIdMap.remove(caseId);
                    return 0;
                } else {
                    return packetLossDestinationDispose(paramMap, caseId, connTotal + 1);
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
                return packetLossDestinationDispose(paramMap, caseId, connTotal + 1);
            }
        }
    }


    /**
     * ab���� �� caseId �������ݿ�
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

        if (StrUtil.isBlank(interfaceName1) || StrUtil.isBlank(pe1) || StrUtil.isBlank(vrf1) || StrUtil.isBlank(peWan1) ||
                StrUtil.isBlank(interfaceName2) || StrUtil.isBlank(pe2) || StrUtil.isBlank(vrf2) || StrUtil.isBlank(peWan2)) {
            results = "������·site_id ��ѯ�� interface��pe,vrf,pe_wan_ip��һ����߶���Ϊ��";
            Connection conn = OracleDbUtil.getRemedyProConnection();
            int i = insertWorkLog(results, caseId, conn);
            OracleDbUtil.closeConnection(conn);
            GetResults.caseIdMap.remove(caseId);
            return i;
        }
        try {
            results = abPacketLossProblem(interfaceName1, pe1, vrf1, peWan1, interfaceName2, pe2, vrf2, peWan2);
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
     * ����ͼ�л�ȡ���е�case
     */
    public static void getAllCaseByView() {
        String sql = "select * from  aradmin.HPD_HELPDESK_ACT_VIEW    order by case_id desc";
        Connection con = OracleDbUtil.getRemedyProConnection();
        List<TreeMap<String, Object>> list = OracleDbUtil.executeQueryMap(sql, null, con);
        //����
        HashMap<String, ArrayList<CaseView>> repeatMap = new HashMap<>();//���ڼ�¼�ظ���site Case Map
        HashMap<String, CaseView> onlyOneMap = new HashMap<>();//���ڼ�¼ֻ��һ��site�� Case Map
        List<CaseView> trunkList = new ArrayList<>();//�Ǹ����͵�
        List<CaseView> flappingList = new ArrayList<>();//�������͵�
        List<CaseView> otherList = new ArrayList<>();//�����������͵�


        HashMap<String, String> repeatCaseIdMap = checkIsInCaseIdMap(list);
        for (int i = 0; i < list.size(); i++) {
            TreeMap<String, Object> tempMap = list.get(i);
            String caseId = tempMap.get("CASE_ID").toString();
            if (!StrUtil.isBlank(repeatCaseIdMap.get(caseId))) {//������ظ���caseId,֮ǰû�д����� ����
                continue;
            } else {//�������ظ�
                GetResults.caseIdMap.put(caseId, caseId);
            }
            CaseView caseView = new CaseView();
            caseView.setCaseId(tempMap.get("CASE_ID").toString());
            caseView.setPeName(tempMap.get("PE_ROUTER").toString());
            caseView.setInterfaceName(tempMap.get("PE_PORT_INTERFACE").toString());
            caseView.setVrf(tempMap.get("VRF").toString());
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
            System.out.println(JSONUtil.toJsonStr(caseView));
            if (caseView.getWebType().indexOf("Core Network") > -1) {//�Ǹ�����
                trunkList.add(caseView);
            } else if (caseView.getWebItem().indexOf("Packet loss") > -1) {//����
                if (onlyOneMap.containsKey(caseView.getCaseId())) {//����ж���A-B����,�ٸ����Ƿ����dstIp ����(site2site Packet loss)(site-dst Packet loss)
                    ArrayList<CaseView> tempList = new ArrayList<>();
                    tempList.add(onlyOneMap.get(caseView.getCaseId()));
                    tempList.add(caseView);
                    onlyOneMap.remove(caseView.getCaseId());
                    repeatMap.put(caseView.getCaseId(), tempList);
                } else {//һ�� Ȼ���ٸ����Ƿ����dstIp ����(Local packet loss)()
                    onlyOneMap.put(caseView.getCaseId(), caseView);
                }
            } else if (caseView.getWebItem().indexOf("Service down") > -1) {//��������
                flappingList.add(caseView);
            }else if(!StrUtil.isBlank(caseView.getWebItem())&&!StrUtil.isBlank(caseView.getWebType())){
                otherList.add(caseView);
            }
        }
        OracleDbUtil.closeConnection(con);
        for (int i = 0; i < trunkList.size(); i++) {//�Ǹ�����
            CaseView caseView = trunkList.get(i);
            TrunkManage trunkManage = new TrunkManage(caseView);
            trunkManage.start();
            ThreadUtil.sleep(2000);
        }

        for (int i = 0; i < flappingList.size(); i++) {//��������
            CaseView caseView = flappingList.get(i);
            FlappingManage flappingManage = new FlappingManage(caseView);
            flappingManage.start();
            ThreadUtil.sleep(2000);

        }

        for (String caseId : onlyOneMap.keySet()) {//���߶���/���߶����Զ˼��
            CaseView caseView = onlyOneMap.get(caseId);
            OnlyOneSiteManage onlyOneSiteManage = new OnlyOneSiteManage(caseView);
            onlyOneSiteManage.start();
            ThreadUtil.sleep(2000);


        }

        for (String caseId : repeatMap.keySet()) {//a-b����
            ArrayList<CaseView> caseViews = repeatMap.get(caseId);
            ABSiteManage abSiteManage = new ABSiteManage(caseViews);
            abSiteManage.start();
            ThreadUtil.sleep(2000);
        }

        for (int i = 0; i < otherList.size(); i++) {//�� ���� �ж� ���͵����
            CaseView caseView=otherList.get(i);
            ClearOtherManage clearOtherManage=new ClearOtherManage(caseView);
            clearOtherManage.start();
            ThreadUtil.sleep(2000);
        }
        caseIdLog.info("repeatMap:" + JSONUtil.toJsonStr(repeatMap));
        caseIdLog.info("onlyOneMap:" + JSONUtil.toJsonStr(onlyOneMap));
        caseIdLog.info("flappingList:" + JSONUtil.toJsonStr(flappingList));
        caseIdLog.info("trunkList" + JSONUtil.toJsonStr(trunkList));
        caseIdLog.info("otherList" + JSONUtil.toJsonStr(otherList));


    }

    /**
     * �ж��ǲ��Ǵ����� ֮ǰ����ִ�е� Case
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
     * ���Է�����
     */
    public static void testMain() {
        GetResults getResults = new GetResults();
        String caseId = "HD0000002828188";//case���
        int connTotal = 1;//����ggw����


        //�Ǹɻ�ȡ���
        String trunkName = "CNGUZYUJ1001C.ge-11/0/8.50-CNGUZYUJ1004E.ge-0/0/0.50";
//        System.out.println(trunkProblem(trunkName));
//        System.out.println( GetResults.trunkDispose(trunkName,caseId,connTotal));


        //���ϻ�ȡ���
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


        //a-b����
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
//        System.out.println("case:"+caseId+"\ttype:����"+"\t"+GetResults.abPacketLossDispose(paramMap,caseId,connTotal));

    }


    /**
     * �ٵ���ʱ���������ڽ������
     *
     * @return
     */
    public static String getTempParam() {
        String tempParam = "�Ǹ� ���\n" +
                "\n" +
                "+++++PE1:CNGUZYUJ1001C+++++Interface1:ge-11/0/8.50+++++PE2:CNGUZYUJ1004E+++++Interface2:ge-0/0/0.50+++++\n" +
                "PE1:CNGUZYUJ1001C    �˿�:Up.\n" +
                "PE1:CNGUZYUJ1001C    �շ�������.\n" +
                "PE1:CNGUZYUJ1001C    COS��Drop.\n" +
                "PE2:CNGUZYUJ1004E    �˿�:Up.\n" +
                "PE2:CNGUZYUJ1004E    �շ�������.\n" +
                "PE2:CNGUZYUJ1004E    COS��Drop.\n" +
                "PE1:CNGUZYUJ1001C    Ĭ���շ���: 0% packet loss.\n" +
                "PE1:CNGUZYUJ1001C    ����շ�: 0% packet loss.\n" +
                "PE1:CNGUZYUJ1001C    ISISЭ��UP, Last transition:117w6d 04:59:58 ago\n" +
                "====================================================================================================\n" +
                "----------------------------------------------------------------------------------------------------\n" +
                "CNGUZYUJ1001C>show interfaces ge-11/0/8\n" +
                "Physical interface: ge-11/0/8, Enabled, Physical link is Up\n" +
                "  Interface index: 163, SNMP ifIndex: 564\n" +
                "  Link-level type: Flexible-Ethernet, MTU: 9192, MRU: 9200, LAN-PHY mode, Speed: 1000mbps, BPDU Error: None, MAC-REWRITE Error: None, Loopback: Disabled, Source filtering: Disabled, Flow control: Enabled, Auto-negotiation: Enabled, Remote fault: Online\n" +
                "  Pad to minimum frame size: Disabled\n" +
                "  Device flags   : Present Running\n" +
                "  Interface flags: SNMP-Traps Internal: 0x4000\n" +
                "  CoS queues     : 8 supported, 8 maximum usable queues\n" +
                "  Current address: 44:f4:77:88:1b:1f, Hardware address: 44:f4:77:88:1b:1f\n" +
                "  Last flapped   : 2017-04-21 18:05:35 HKT (147w6d 17:33 ago)\n" +
                "  Input rate     : 404832 bps (152 pps)\n" +
                "  Output rate    : 226488 bps (78 pps)\n" +
                "  Active alarms  : None\n" +
                "  Active defects : None\n" +
                "  Interface transmit statistics: Disabled\n" +
                "\n" +
                "  Logical interface ge-11/0/8.50 (Index 402) (SNMP ifIndex 635)\n" +
                "    Description: Trunk 1000M to CNGUZYUJ1004E (ge-0/0/0.50) via Direct Fiber (2017/04/22 by Charlie)\n" +
                "    Flags: Up SNMP-Traps 0x4000 VLAN-Tag [ 0x8100.50 ]  Encapsulation: ENET2\n" +
                "    Input packets : 13326173865\n" +
                "    Output packets: 6201894166\n" +
                "    Protocol inet, MTU: 9170\n" +
                "      Flags: Sendbcast-pkt-to-re, User-MTU\n" +
                "      Addresses, Flags: Is-Preferred Is-Primary\n" +
                "        Destination: 218.96.234.216/30, Local: 218.96.234.218, Broadcast: 218.96.234.219\n" +
                "    Protocol iso, MTU: 1497\n" +
                "      Flags: User-MTU\n" +
                "    Protocol mpls, MTU: 9150, Maximum labels: 5\n" +
                "      Flags: User-MTU\n" +
                "    Protocol multiservice, MTU: Unlimited\n" +
                "\n" +
                "  Logical interface ge-11/0/8.32767 (Index 403) (SNMP ifIndex 636)\n" +
                "    Flags: Up SNMP-Traps 0x4004000 VLAN-Tag [ 0x0000.0 ]  Encapsulation: ENET2\n" +
                "    Input packets : 0\n" +
                "    Output packets: 0\n" +
                "    Protocol multiservice, MTU: Unlimited\n" +
                "      Flags: None\n" +
                "\n" +
                "----------------------------------------------------------------------------------------------------\n" +
                "CNGUZYUJ1001C>show interfaces diagnostics optics ge-11/0/8 | match power\n" +
                "    Laser output power                        :  0.2820 mW / -5.50 dBm\n" +
                "    Receiver signal average optical power     :  0.2776 mW / -5.57 dBm\n" +
                "    Laser output power high alarm             :  Off\n" +
                "    Laser output power low alarm              :  Off\n" +
                "    Laser output power high warning           :  Off\n" +
                "    Laser output power low warning            :  Off\n" +
                "    Laser rx power high alarm                 :  Off\n" +
                "    Laser rx power low alarm                  :  Off\n" +
                "    Laser rx power high warning               :  Off\n" +
                "    Laser rx power low warning                :  Off\n" +
                "    Laser output power high alarm threshold   :  1.1000 mW / 0.41 dBm\n" +
                "    Laser output power low alarm threshold    :  0.0600 mW / -12.22 dBm\n" +
                "    Laser output power high warning threshold :  1.0000 mW / 0.00 dBm\n" +
                "    Laser output power low warning threshold  :  0.0850 mW / -10.71 dBm\n" +
                "    Laser rx power high alarm threshold       :  1.8000 mW / 2.55 dBm\n" +
                "    Laser rx power low alarm threshold        :  0.0000 mW / - Inf dBm\n" +
                "    Laser rx power high warning threshold     :  1.0000 mW / 0.00 dBm\n" +
                "    Laser rx power low warning threshold      :  0.0200 mW / -16.99 dBm\n" +
                "\n" +
                "----------------------------------------------------------------------------------------------------\n" +
                "CNGUZYUJ1001C> show interfaces queue ge-11/0/8\n" +
                "Physical interface: ge-11/0/8, Enabled, Physical link is Up\n" +
                "  Interface index: 163, SNMP ifIndex: 564\n" +
                "Forwarding classes: 16 supported, 7 in use\n" +
                "Egress queues: 8 supported, 7 in use\n" +
                "Queue: 0, Forwarding classes: BE\n" +
                "  Queued:\n" +
                "    Packets              :            4534387495                    88 pps\n" +
                "    Bytes                :         2110107616713                204416 bps\n" +
                "  Transmitted:\n" +
                "    Packets              :            4534387495                    88 pps\n" +
                "    Bytes                :         2110107616713                204416 bps\n" +
                "    Tail-dropped packets :                     0                     0 pps\n" +
                "    RL-dropped packets   :                     0                     0 pps\n" +
                "    RL-dropped bytes     :                     0                     0 bps\n" +
                "    RED-dropped packets  :                     0                     0 pps\n" +
                "     Low                 :                     0                     0 pps\n" +
                "     Medium-low          :                     0                     0 pps\n" +
                "     Medium-high         :                     0                     0 pps\n" +
                "     High                :                     0                     0 pps\n" +
                "    RED-dropped bytes    :                     0                     0 bps\n" +
                "     Low                 :                     0                     0 bps\n" +
                "     Medium-low          :                     0                     0 bps\n" +
                "     Medium-high         :                     0                     0 bps\n" +
                "     High                :                     0                     0 bps\n" +
                "  Queue-depth bytes      : \n" +
                "    Average              :                     0\n" +
                "    Current              :                     0\n" +
                "    Peak                 :                     0\n" +
                "    Maximum              :               3768320\n" +
                "Queue: 1, Forwarding classes: CS1\n" +
                "  Queued:\n" +
                "    Packets              :             929083932                     5 pps\n" +
                "    Bytes                :          472218105620                 22144 bps\n" +
                "  Transmitted:\n" +
                "    Packets              :             929083932                     5 pps\n" +
                "    Bytes                :          472218105620                 22144 bps\n" +
                "    Tail-dropped packets :                     0                     0 pps\n" +
                "    RL-dropped packets   :                     0                     0 pps\n" +
                "    RL-dropped bytes     :                     0                     0 bps\n" +
                "    RED-dropped packets  :                     0                     0 pps\n" +
                "     Low                 :                     0                     0 pps\n" +
                "     Medium-low          :                     0                     0 pps\n" +
                "     Medium-high         :                     0                     0 pps\n" +
                "     High                :                     0                     0 pps\n" +
                "    RED-dropped bytes    :                     0                     0 bps\n" +
                "     Low                 :                     0                     0 bps\n" +
                "     Medium-low          :                     0                     0 bps\n" +
                "     Medium-high         :                     0                     0 bps\n" +
                "     High                :                     0                     0 bps\n" +
                "  Queue-depth bytes      : \n" +
                "    Average              :                     0\n" +
                "    Current              :                     0\n" +
                "    Peak                 :                     0\n" +
                "    Maximum              :               3768320\n" +
                "Queue: 2, Forwarding classes: CS3\n" +
                "  Queued:\n" +
                "    Packets              :              64587698                     0 pps\n" +
                "    Bytes                :           20245367528                     0 bps\n" +
                "  Transmitted:\n" +
                "    Packets              :              64587698                     0 pps\n" +
                "    Bytes                :           20245367528                     0 bps\n" +
                "    Tail-dropped packets :                     0                     0 pps\n" +
                "    RL-dropped packets   :                     0                     0 pps\n" +
                "    RL-dropped bytes     :                     0                     0 bps\n" +
                "    RED-dropped packets  :                     0                     0 pps\n" +
                "     Low                 :                     0                     0 pps\n" +
                "     Medium-low          :                     0                     0 pps\n" +
                "     Medium-high         :                     0                     0 pps\n" +
                "     High                :                     0                     0 pps\n" +
                "    RED-dropped bytes    :                     0                     0 bps\n" +
                "     Low                 :                     0                     0 bps\n" +
                "     Medium-low          :                     0                     0 bps\n" +
                "     Medium-high         :                     0                     0 bps\n" +
                "     High                :                     0                     0 bps\n" +
                "  Queue-depth bytes      : \n" +
                "    Average              :                     0\n" +
                "    Current              :                     0\n" +
                "    Peak                 :                     0\n" +
                "    Maximum              :               1277952\n" +
                "Queue: 3, Forwarding classes: EF\n" +
                "  Queued:\n" +
                "    Packets              :             671998426                     5 pps\n" +
                "    Bytes                :          189161490949                  4736 bps\n" +
                "  Transmitted:\n" +
                "    Packets              :             671998426                     5 pps\n" +
                "    Bytes                :          189161490949                  4736 bps\n" +
                "    Tail-dropped packets :                     0                     0 pps\n" +
                "    RL-dropped packets   :                     0                     0 pps\n" +
                "    RL-dropped bytes     :                     0                     0 bps\n" +
                "    RED-dropped packets  :                     0                     0 pps\n" +
                "     Low                 :                     0                     0 pps\n" +
                "     Medium-low          :                     0                     0 pps\n" +
                "     Medium-high         :                     0                     0 pps\n" +
                "     High                :                     0                     0 pps\n" +
                "    RED-dropped bytes    :                     0                     0 bps\n" +
                "     Low                 :                     0                     0 bps\n" +
                "     Medium-low          :                     0                     0 bps\n" +
                "     Medium-high         :                     0                     0 bps\n" +
                "     High                :                     0                     0 bps\n" +
                "  Queue-depth bytes      : \n" +
                "    Average              :                     0\n" +
                "    Current              :                     0\n" +
                "    Peak                 :                     0\n" +
                "    Maximum              :                638976\n" +
                "Queue: 4, Forwarding classes: CS2\n" +
                "  Queued:\n" +
                "    Packets              :               1016725                     0 pps\n" +
                "    Bytes                :             320151397                     0 bps\n" +
                "  Transmitted:\n" +
                "    Packets              :               1016725                     0 pps\n" +
                "    Bytes                :             320151397                     0 bps\n" +
                "    Tail-dropped packets :                     0                     0 pps\n" +
                "    RL-dropped packets   :                     0                     0 pps\n" +
                "    RL-dropped bytes     :                     0                     0 bps\n" +
                "    RED-dropped packets  :                     0                     0 pps\n" +
                "     Low                 :                     0                     0 pps\n" +
                "     Medium-low          :                     0                     0 pps\n" +
                "     Medium-high         :                     0                     0 pps\n" +
                "     High                :                     0                     0 pps\n" +
                "    RED-dropped bytes    :                     0                     0 bps\n" +
                "     Low                 :                     0                     0 bps\n" +
                "     Medium-low          :                     0                     0 bps\n" +
                "     Medium-high         :                     0                     0 bps\n" +
                "     High                :                     0                     0 bps\n" +
                "  Queue-depth bytes      : \n" +
                "    Average              :                     0\n" +
                "    Current              :                     0\n" +
                "    Peak                 :                     0\n" +
                "    Maximum              :               1277952\n" +
                "Queue: 5, Forwarding classes: CS4\n" +
                "  Queued:\n" +
                "    Packets              :                820307                     0 pps\n" +
                "    Bytes                :             295040300                     0 bps\n" +
                "  Transmitted:\n" +
                "    Packets              :                820307                     0 pps\n" +
                "    Bytes                :             295040300                     0 bps\n" +
                "    Tail-dropped packets :                     0                     0 pps\n" +
                "    RL-dropped packets   :                     0                     0 pps\n" +
                "    RL-dropped bytes     :                     0                     0 bps\n" +
                "    RED-dropped packets  :                     0                     0 pps\n" +
                "     Low                 :                     0                     0 pps\n" +
                "     Medium-low          :                     0                     0 pps\n" +
                "     Medium-high         :                     0                     0 pps\n" +
                "     High                :                     0                     0 pps\n" +
                "    RED-dropped bytes    :                     0                     0 bps\n" +
                "     Low                 :                     0                     0 bps\n" +
                "     Medium-low          :                     0                     0 bps\n" +
                "     Medium-high         :                     0                     0 bps\n" +
                "     High                :                     0                     0 bps\n" +
                "  Queue-depth bytes      : \n" +
                "    Average              :                     0\n" +
                "    Current              :                     0\n" +
                "    Peak                 :                     0\n" +
                "    Maximum              :               1277952\n" +
                "Queue: 6, Forwarding classes: NC\n" +
                "  Queued:\n" +
                "    Packets              :                   596                     0 pps\n" +
                "    Bytes                :                 57516                     0 bps\n" +
                "  Transmitted:\n" +
                "    Packets              :                   596                     0 pps\n" +
                "    Bytes                :                 57516                     0 bps\n" +
                "    Tail-dropped packets :                     0                     0 pps\n" +
                "    RL-dropped packets   :                     0                     0 pps\n" +
                "    RL-dropped bytes     :                     0                     0 bps\n" +
                "    RED-dropped packets  :                     0                     0 pps\n" +
                "     Low                 :                     0                     0 pps\n" +
                "     Medium-low          :                     0                     0 pps\n" +
                "     Medium-high         :                     0                     0 pps\n" +
                "     High                :                     0                     0 pps\n" +
                "    RED-dropped bytes    :                     0                     0 bps\n" +
                "     Low                 :                     0                     0 bps\n" +
                "     Medium-low          :                     0                     0 bps\n" +
                "     Medium-high         :                     0                     0 bps\n" +
                "     High                :                     0                     0 bps\n" +
                "  Queue-depth bytes      : \n" +
                "    Average              :                     0\n" +
                "    Current              :                     0\n" +
                "    Peak                 :                     0\n" +
                "    Maximum              :                638976\n" +
                "\n" +
                "----------------------------------------------------------------------------------------------------\n" +
                "CNGUZYUJ1004E>show interfaces ge-0/0/0\n" +
                "Physical interface: ge-0/0/0, Enabled, Physical link is Up\n" +
                "  Interface index: 129, SNMP ifIndex: 523\n" +
                "  Link-level type: Flexible-Ethernet, MTU: 9192, MRU: 0, Speed: 1000mbps, BPDU Error: None, MAC-REWRITE Error: None, Loopback: Disabled, Source filtering: Disabled, Flow control: Enabled, Auto-negotiation: Enabled, Remote fault: Online\n" +
                "  Device flags   : Present Running\n" +
                "  Interface flags: SNMP-Traps Internal: 0x4000\n" +
                "  Link flags     : None\n" +
                "  CoS queues     : 8 supported, 8 maximum usable queues\n" +
                "  Current address: 00:22:83:15:44:00, Hardware address: 00:22:83:15:44:00\n" +
                "  Last flapped   : 2017-04-21 18:08:01 HKT (147w6d 17:31 ago)\n" +
                "  Input rate     : 86464 bps (66 pps)\n" +
                "  Output rate    : 102368 bps (71 pps)\n" +
                "  Active alarms  : None\n" +
                "  Active defects : None\n" +
                "  Interface transmit statistics: Disabled\n" +
                "\n" +
                "  Logical interface ge-0/0/0.50 (Index 77) (SNMP ifIndex 530)\n" +
                "    Description: Trunk 1000M to CNGUZYUJ1001C (ge-11/0/4.50) via Direct (2017/04/21 by PXC)\n" +
                "    Flags: Up SNMP-Traps 0x4000 VLAN-Tag [ 0x8100.50 ]  Encapsulation: ENET2\n" +
                "    Input packets : 3365438913\n" +
                "    Output packets: 13326890725\n" +
                "    Protocol inet, MTU: 9170\n" +
                "      Flags: Sendbcast-pkt-to-re, User-MTU\n" +
                "      Addresses, Flags: Is-Preferred Is-Primary\n" +
                "        Destination: 218.96.234.216/30, Local: 218.96.234.217, Broadcast: 218.96.234.219\n" +
                "    Protocol iso, MTU: 1497\n" +
                "      Flags: Is-Primary, User-MTU\n" +
                "    Protocol mpls, MTU: 9150, Maximum labels: 5\n" +
                "      Flags: Is-Primary, User-MTU\n" +
                "    Protocol multiservice, MTU: Unlimited\n" +
                "\n" +
                "  Logical interface ge-0/0/0.32767 (Index 78) (SNMP ifIndex 531)\n" +
                "    Flags: Up SNMP-Traps 0x4000 VLAN-Tag [ 0x0000.0 ]  Encapsulation: ENET2\n" +
                "    Input packets : 0\n" +
                "    Output packets: 0\n" +
                "    Protocol multiservice, MTU: Unlimited\n" +
                "      Flags: None\n" +
                "\n" +
                "----------------------------------------------------------------------------------------------------\n" +
                "CNGUZYUJ1004E>show interfaces diagnostics optics ge-0/0/0 | match power\n" +
                "    Laser output power                        :  0.3060 mW / -5.14 dBm\n" +
                "    Receiver signal average optical power     :  0.2778 mW / -5.56 dBm\n" +
                "    Laser output power high alarm             :  Off\n" +
                "    Laser output power low alarm              :  Off\n" +
                "    Laser output power high warning           :  Off\n" +
                "    Laser output power low warning            :  Off\n" +
                "    Laser rx power high alarm                 :  Off\n" +
                "    Laser rx power low alarm                  :  Off\n" +
                "    Laser rx power high warning               :  Off\n" +
                "    Laser rx power low warning                :  Off\n" +
                "    Laser output power high alarm threshold   :  1.0000 mW / 0.00 dBm\n" +
                "    Laser output power low alarm threshold    :  0.0440 mW / -13.57 dBm\n" +
                "    Laser output power high warning threshold :  0.5010 mW / -3.00 dBm\n" +
                "    Laser output power low warning threshold  :  0.1120 mW / -9.51 dBm\n" +
                "    Laser rx power high alarm threshold       :  1.1220 mW / 0.50 dBm\n" +
                "    Laser rx power low alarm threshold        :  0.0079 mW / -21.02 dBm\n" +
                "    Laser rx power high warning threshold     :  0.7943 mW / -1.00 dBm\n" +
                "    Laser rx power low warning threshold      :  0.0200 mW / -16.99 dBm\n" +
                "\n" +
                "----------------------------------------------------------------------------------------------------\n" +
                "CNGUZYUJ1004E> show interfaces queue ge-0/0/0\n" +
                "Physical interface: ge-0/0/0, Enabled, Physical link is Up\n" +
                "  Interface index: 129, SNMP ifIndex: 523\n" +
                "Forwarding classes: 16 supported, 7 in use\n" +
                "Egress queues: 8 supported, 7 in use\n" +
                "Queue: 0, Forwarding classes: BE\n" +
                "  Queued:\n" +
                "    Packets              :           11958801167                    56 pps\n" +
                "    Bytes                :         6653647933082                138400 bps\n" +
                "  Transmitted:\n" +
                "    Packets              :           11958801167                    56 pps\n" +
                "    Bytes                :         6653647933082                138400 bps\n" +
                "    Tail-dropped packets :                     0                     0 pps\n" +
                "    RL-dropped packets   :                     0                     0 pps\n" +
                "    RL-dropped bytes     :                     0                     0 bps\n" +
                "    RED-dropped packets  :                     0                     0 pps\n" +
                "     Low, non-TCP        :                     0                     0 pps\n" +
                "     Low, TCP            :                     0                     0 pps\n" +
                "     High, non-TCP       :                     0                     0 pps\n" +
                "     High, TCP           :                     0                     0 pps\n" +
                "    RED-dropped bytes    :                     0                     0 bps\n" +
                "     Low, non-TCP        :                     0                     0 bps\n" +
                "     Low, TCP            :                     0                     0 bps\n" +
                "     High, non-TCP       :                     0                     0 bps\n" +
                "     High, TCP           :                     0                     0 bps\n" +
                "Queue: 1, Forwarding classes: CS1\n" +
                "  Queued:\n" +
                "    Packets              :                   158                     0 pps\n" +
                "    Bytes                :                 20635                     0 bps\n" +
                "  Transmitted:\n" +
                "    Packets              :                   158                     0 pps\n" +
                "    Bytes                :                 20635                     0 bps\n" +
                "    Tail-dropped packets :                     0                     0 pps\n" +
                "    RL-dropped packets   :                     0                     0 pps\n" +
                "    RL-dropped bytes     :                     0                     0 bps\n" +
                "    RED-dropped packets  :                     0                     0 pps\n" +
                "     Low, non-TCP        :                     0                     0 pps\n" +
                "     Low, TCP            :                     0                     0 pps\n" +
                "     High, non-TCP       :                     0                     0 pps\n" +
                "     High, TCP           :                     0                     0 pps\n" +
                "    RED-dropped bytes    :                     0                     0 bps\n" +
                "     Low, non-TCP        :                     0                     0 bps\n" +
                "     Low, TCP            :                     0                     0 bps\n" +
                "     High, non-TCP       :                     0                     0 bps\n" +
                "     High, TCP           :                     0                     0 bps\n" +
                "Queue: 2, Forwarding classes: CS3\n" +
                "  Queued:\n" +
                "    Packets              :                     0                     0 pps\n" +
                "    Bytes                :                     0                     0 bps\n" +
                "  Transmitted:\n" +
                "    Packets              :                     0                     0 pps\n" +
                "    Bytes                :                     0                     0 bps\n" +
                "    Tail-dropped packets :                     0                     0 pps\n" +
                "    RL-dropped packets   :                     0                     0 pps\n" +
                "    RL-dropped bytes     :                     0                     0 bps\n" +
                "    RED-dropped packets  :                     0                     0 pps\n" +
                "     Low, non-TCP        :                     0                     0 pps\n" +
                "     Low, TCP            :                     0                     0 pps\n" +
                "     High, non-TCP       :                     0                     0 pps\n" +
                "     High, TCP           :                     0                     0 pps\n" +
                "    RED-dropped bytes    :                     0                     0 bps\n" +
                "     Low, non-TCP        :                     0                     0 bps\n" +
                "     Low, TCP            :                     0                     0 bps\n" +
                "     High, non-TCP       :                     0                     0 bps\n" +
                "     High, TCP           :                     0                     0 bps\n" +
                "Queue: 3, Forwarding classes: EF\n" +
                "  Queued:\n" +
                "    Packets              :            1368092500                    59 pps\n" +
                "    Bytes                :          165890187224                 46280 bps\n" +
                "  Transmitted:\n" +
                "    Packets              :            1368092500                    59 pps\n" +
                "    Bytes                :          165890187224                 46280 bps\n" +
                "    Tail-dropped packets :                     0                     0 pps\n" +
                "    RL-dropped packets   :                     0                     0 pps\n" +
                "    RL-dropped bytes     :                     0                     0 bps\n" +
                "    RED-dropped packets  :                     0                     0 pps\n" +
                "     Low, non-TCP        :                     0                     0 pps\n" +
                "     Low, TCP            :                     0                     0 pps\n" +
                "     High, non-TCP       :                     0                     0 pps\n" +
                "     High, TCP           :                     0                     0 pps\n" +
                "    RED-dropped bytes    :                     0                     0 bps\n" +
                "     Low, non-TCP        :                     0                     0 bps\n" +
                "     Low, TCP            :                     0                     0 bps\n" +
                "     High, non-TCP       :                     0                     0 bps\n" +
                "     High, TCP           :                     0                     0 bps\n" +
                "Queue: 4, Forwarding classes: CS2\n" +
                "  Queued:\n" +
                "    Packets              :                     0                     0 pps\n" +
                "    Bytes                :                     0                     0 bps\n" +
                "  Transmitted:\n" +
                "    Packets              :                     0                     0 pps\n" +
                "    Bytes                :                     0                     0 bps\n" +
                "    Tail-dropped packets :                     0                     0 pps\n" +
                "    RL-dropped packets   :                     0                     0 pps\n" +
                "    RL-dropped bytes     :                     0                     0 bps\n" +
                "    RED-dropped packets  :                     0                     0 pps\n" +
                "     Low, non-TCP        :                     0                     0 pps\n" +
                "     Low, TCP            :                     0                     0 pps\n" +
                "     High, non-TCP       :                     0                     0 pps\n" +
                "     High, TCP           :                     0                     0 pps\n" +
                "    RED-dropped bytes    :                     0                     0 bps\n" +
                "     Low, non-TCP        :                     0                     0 bps\n" +
                "     Low, TCP            :                     0                     0 bps\n" +
                "     High, non-TCP       :                     0                     0 bps\n" +
                "     High, TCP           :                     0                     0 bps\n" +
                "Queue: 5, Forwarding classes: CS4\n" +
                "  Queued:\n" +
                "    Packets              :                     0                     0 pps\n" +
                "    Bytes                :                     0                     0 bps\n" +
                "  Transmitted:\n" +
                "    Packets              :                     0                     0 pps\n" +
                "    Bytes                :                     0                     0 bps\n" +
                "    Tail-dropped packets :                     0                     0 pps\n" +
                "    RL-dropped packets   :                     0                     0 pps\n" +
                "    RL-dropped bytes     :                     0                     0 bps\n" +
                "    RED-dropped packets  :                     0                     0 pps\n" +
                "     Low, non-TCP        :                     0                     0 pps\n" +
                "     Low, TCP            :                     0                     0 pps\n" +
                "     High, non-TCP       :                     0                     0 pps\n" +
                "     High, TCP           :                     0                     0 pps\n" +
                "    RED-dropped bytes    :                     0                     0 bps\n" +
                "     Low, non-TCP        :                     0                     0 bps\n" +
                "     Low, TCP            :                     0                     0 bps\n" +
                "     High, non-TCP       :                     0                     0 bps\n" +
                "     High, TCP           :                     0                     0 bps\n" +
                "Queue: 6, Forwarding classes: NC\n" +
                "  Queued:\n" +
                "    Packets              :                     0                     0 pps\n" +
                "    Bytes                :                     0                     0 bps\n" +
                "  Transmitted:\n" +
                "    Packets              :                     0                     0 pps\n" +
                "    Bytes                :                     0                     0 bps\n" +
                "    Tail-dropped packets :                     0                     0 pps\n" +
                "    RL-dropped packets   :                     0                     0 pps\n" +
                "    RL-dropped bytes     :                     0                     0 bps\n" +
                "    RED-dropped packets  :                     0                     0 pps\n" +
                "     Low, non-TCP        :                     0                     0 pps\n" +
                "     Low, TCP            :                     0                     0 pps\n" +
                "     High, non-TCP       :                     0                     0 pps\n" +
                "     High, TCP           :                     0                     0 pps\n" +
                "    RED-dropped bytes    :                     0                     0 bps\n" +
                "     Low, non-TCP        :                     0                     0 bps\n" +
                "     Low, TCP            :                     0                     0 bps\n" +
                "     High, non-TCP       :                     0                     0 bps\n" +
                "     High, TCP           :                     0                     0 bps\n" +
                "\n" +
                "----------------------------------------------------------------------------------------------------\n" +
                "CNGUZYUJ1001C>>ping interface ge-11/0/8.50 rapid source 218.96.234.218 218.96.234.218\n" +
                "PING 218.96.234.218 (218.96.234.218): 56 data bytes\n" +
                "!!!!!\n" +
                "--- 218.96.234.218 ping statistics ---\n" +
                "5 packets transmitted, 5 packets received, 0% packet loss\n" +
                "round-trip min/avg/max/stddev = 0.025/0.060/0.174/0.058 ms\n" +
                "\n" +
                "----------------------------------------------------------------------------------------------------\n" +
                "CNGUZYUJ1001C>ping interface ge-11/0/8.50 rapid source 218.96.234.218 218.96.234.218 count 10\n" +
                "PING 218.96.234.218 (218.96.234.218): 56 data bytes\n" +
                "!!!!!!!!!!\n" +
                "--- 218.96.234.218 ping statistics ---\n" +
                "10 packets transmitted, 10 packets received, 0% packet loss\n" +
                "round-trip min/avg/max/stddev = 0.101/0.204/0.322/0.061 ms\n" +
                "\n" +
                "----------------------------------------------------------------------------------------------------\n" +
                "CNGUZYUJ1001C>ping interface  ge-11/0/8.50 rapid source 218.96.234.218 218.96.234.218 count 5 size 100 do-not-fragment\n" +
                "PING 218.96.234.218 (218.96.234.218): 100 data bytes\n" +
                "!!!!!\n" +
                "--- 218.96.234.218 ping statistics ---\n" +
                "5 packets transmitted, 5 packets received, 0% packet loss\n" +
                "round-trip min/avg/max/stddev = 0.060/0.183/0.282/0.073 ms\n" +
                "\n" +
                "----------------------------------------------------------------------------------------------------\n" +
                "CNGUZYUJ1001C>show log messages | match ge-11/0/8.50 | last 10 | no-more\n" +
                "\n" +
                "                                        \n" +
                "----------------------------------------------------------------------------------------------------\n" +
                "CNGUZYUJ1001C>show isis adjacency CNGUZYUJ1004E extensive\n" +
                "CNGUZYUJ1004E\n" +
                "  Interface: ge-11/0/8.50, Level: 2, State: Up, Expires in 21 secs\n" +
                "  Priority: 0, Up/Down transitions: 1, Last transition: 117w6d 04:59:58 ago\n" +
                "  Circuit type: 2, Speaks: IP\n" +
                "  Topologies: Unicast\n" +
                "  Restart capable: Yes, Adjacency advertisement: Advertise\n" +
                "  IP addresses: 218.96.234.217\n" +
                "  Transition log:\n" +
                "  When                  State        Event           Down reason\n" +
                "  Sat Nov 18 06:40:05   Up           Seenself        \n";
        return tempParam;
    }

    /**
     * ��ȡ����ļ�Ҫ��Ϣ
     *
     * @param results
     * @return
     */
    public static String resultsGetSummary(String results) {
        if (StrUtil.isBlank(results)) {
            return "����Ϊ��";
        } else {
            String summary = null;
            if (results.indexOf("====================================================================================================") == -1) {
                summary = "�Զ���ѯ ��·״̬ ggwAPI ���ز����쳣��������ϵ������Ա������";
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