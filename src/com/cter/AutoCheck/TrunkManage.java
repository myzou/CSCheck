package com.cter.AutoCheck;

import cn.hutool.core.date.DateUtil;
import com.cter.util.BaseLog;

public class TrunkManage extends Thread {

    BaseLog log=new BaseLog("CSCheckLog");

    private CaseView caseView;

    public CaseView getCaseView() {
        return caseView;
    }

    public void setCaseView(CaseView caseView) {
        this.caseView = caseView;
    }

    public TrunkManage(CaseView caseView) {
        this.caseView = caseView;
    }

    @Override
    public void run() {
        try {
            int connTotal=1;
            String caseId=caseView.getCaseId();
            String trunkName=caseView.getTrunkName();
            GetResults.insertCaseLog(caseView);

            log.info(DateUtil.now() +"\t"+"case:"+caseView.getCaseId()+"\ttype:骨干 \t"+ GetResults.trunkDispose(trunkName,caseId,connTotal));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
