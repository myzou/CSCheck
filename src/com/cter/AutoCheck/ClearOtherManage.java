package com.cter.AutoCheck;

import cn.hutool.core.date.DateUtil;
import com.cter.util.BaseLog;

import java.sql.Connection;

public class ClearOtherManage extends Thread {
    BaseLog log=new BaseLog("CSCheckLog");

    private CaseView caseView;

    public CaseView getCaseView() {
        return caseView;
    }

    public void setCaseView(CaseView caseView) {
        this.caseView = caseView;
    }

    public ClearOtherManage(CaseView caseView) {
        this.caseView = caseView;
    }

    @Override
    public void run() {
        try {
            int connTotal=1;
            String caseId=caseView.getCaseId();
            Connection conn = OracleDbUtil.getRemedyProConnection();
            String results="Work Log";
            GetResults.insertCaseLog(caseView);
            int i = GetResults.insertWorkLog(results, caseId, conn);
            OracleDbUtil.closeConnection(conn);
            GetResults.caseIdMap.remove(caseId);

            log.info(  "case:"+caseView.getCaseId()+"\ttype:其他类型 \t"+ i );

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
