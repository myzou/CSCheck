package com.cter.job;

import com.cter.AutoCheck.GetResults;
import com.cter.util.BaseLog;
import com.cter.util.LoadPropertiestUtil;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

/**
 * ���Զ�ʱ��
 * @author op1768
 */
//ʹ��  @Componentע��ָ����bean����
@Component("startWorkJob")
public class StartWorkJob {
    BaseLog log=new BaseLog("CSCaseIDLog");
      static int queryNum=1;

    public void csAutoCheck(){
        log.info(StartWorkJob.queryNum+"============ ��ʼ��ѯ =========");
        GetResults.getAllCaseByView();
        log.info(StartWorkJob.queryNum+"============ ������ѯ =========");
        StartWorkJob.queryNum=queryNum+1;
    }


}