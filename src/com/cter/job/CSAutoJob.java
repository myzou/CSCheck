package com.cter.job;

import com.cter.AutoCheck.GetResults;
import com.cter.util.BaseLog;
import org.springframework.stereotype.Component;

/**
 * 测试定时器
 * @author op1768
 */
//使用  @Component注入指定的bean名称
@Component("CSAutoJob")
public class CSAutoJob {
    BaseLog log=new BaseLog("CSCaseIDLog");
      static int queryNum=1;

    public void csAutoCheck(){
        log.info(CSAutoJob.queryNum+"============ 开始查询 =========");
//        GetResults.getAllCaseByView();
        log.info(CSAutoJob.queryNum+"============ 结束查询 =========");
        CSAutoJob.queryNum=queryNum+1;
    }


}