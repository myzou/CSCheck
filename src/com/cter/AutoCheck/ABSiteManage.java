package com.cter.AutoCheck;

import cn.hutool.core.date.DateUtil;
import com.cter.util.BaseLog;

import java.util.ArrayList;
import java.util.HashMap;

public class ABSiteManage extends  Thread{

    BaseLog log=new BaseLog("CSCheckLog");

    private ArrayList<CaseView> caseViews;

    public ABSiteManage(ArrayList<CaseView> caseViews) {
        this.caseViews = caseViews;
    }

    public ArrayList<CaseView> getCaseViews() {
        return caseViews;
    }

    public void setCaseViews(ArrayList<CaseView> caseViews) {
        this.caseViews = caseViews;
    }

    @Override
    public void run() {
        try {
            String caseId=caseViews.get(0).getCaseId();
            int connTotal=1;
            HashMap<String, String> paramMap = new HashMap<>();
            String interfaceName1 =caseViews.get(0).getInterfaceName();
            String pe1 = caseViews.get(0).getPeName();
            String vrf1 = caseViews.get(0).getVrf();
            String pewan1 = caseViews.get(0).getPeWanIp();
            String interfaceName2 =caseViews.get(1).getInterfaceName();
            String pe2 = caseViews.get(1).getPeName();
            String vrf2 = caseViews.get(1).getVrf();
            String pewan2 = caseViews.get(1).getPeWanIp();
            paramMap.put("interfaceName1", interfaceName1);
            paramMap.put("pe1", pe1);
            paramMap.put("vrf1", vrf1);
            paramMap.put("peWan1", pewan1);
            paramMap.put("interfaceName2", interfaceName2);
            paramMap.put("pe2", pe2);
            paramMap.put("vrf2", vrf2);
            paramMap.put("peWan2", pewan2);
            System.out.print(DateUtil.now() +"\t");
            log.info(DateUtil.now() +"\t"+"case:"+caseId+"\ttype:ab丢包"+"\t"+ GetResults.abPacketLossDispose(paramMap,caseId,connTotal));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {


    }
}
