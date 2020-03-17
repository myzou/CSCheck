package com.cter.AutoCheck;

import cn.hutool.core.date.DateUtil;
import com.cter.util.BaseLog;

import java.util.HashMap;

public class FlappingManage extends Thread {
    BaseLog log=new BaseLog("CSCheckLog");

    private CaseView caseView;

    public CaseView getCaseView() {
        return caseView;
    }

    public void setCaseView(CaseView caseView) {
        this.caseView = caseView;
    }

    public FlappingManage(CaseView caseView) {
        this.caseView = caseView;
    }

    @Override
    public void run() {
        try {
            int connTotal=1;
            String caseId=caseView.getCaseId();
            HashMap<String, String> paramMap = new HashMap<>();
            String interfaceName =caseView.getInterfaceName();
            String pe = caseView.getPeName();
            String vrf = caseView.getVrf();
            paramMap.put("interfaceName", interfaceName);
            paramMap.put("pe", pe);
            paramMap.put("vrf", vrf);
            log.info(DateUtil.now() +"\t"+"case:"+caseView.getCaseId()+"\ttype:иа╤о \t"+ GetResults.flappingDispose(paramMap,caseId,connTotal));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
