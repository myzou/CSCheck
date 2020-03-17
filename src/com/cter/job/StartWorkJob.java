package com.cter.job;

import com.cter.AutoCheck.GetResults;
import com.cter.util.BaseLog;
import com.cter.util.LoadPropertiestUtil;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

/**
 * 测试定时器
 * @author op1768
 */
//使用  @Component注入指定的bean名称
@Component("startWorkJob")
public class StartWorkJob {
    BaseLog log=new BaseLog("CSCaseIDLog");
      static int queryNum=1;

    public void csAutoCheck(){
        log.info(StartWorkJob.queryNum+"============ 开始查询 =========");
        GetResults.getAllCaseByView();
        log.info(StartWorkJob.queryNum+"============ 结束查询 =========");
        StartWorkJob.queryNum=queryNum+1;
    }


}