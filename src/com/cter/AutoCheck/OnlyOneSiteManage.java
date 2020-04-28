package com.cter.AutoCheck;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.cter.util.BaseLog;

import java.util.HashMap;

public class OnlyOneSiteManage extends  Thread{

    BaseLog log=new BaseLog("CSCheckLog");

    private CaseView caseView;

    public CaseView getCaseView() {
        return caseView;
    }

    public void setCaseView(CaseView caseView) {
        this.caseView = caseView;
    }


    public OnlyOneSiteManage(CaseView caseView) {
        this.caseView = caseView;
    }

    @Override
    public void run() {
        try {
            String caseId=caseView.getCaseId();
            int connTotal=1;
            HashMap<String, String> paramMap = new HashMap<>();
            String interfaceName =caseView.getInterfaceName();
            String pe = caseView.getPeName();
            String vrf = caseView.getVrf();
            String peWanIp = caseView.getPeWanIp();
            String ceWanIp = caseView.getCeWanIp();

            String dstIP = caseView.getDestinationIp();
            paramMap.put("interfaceName", interfaceName);
            paramMap.put("pe", pe);
            paramMap.put("vrf", vrf);
            paramMap.put("peWanIp",peWanIp);
            paramMap.put("ceWanIp",ceWanIp);
            paramMap.put("dstIP",dstIP);
            if(StrUtil.isBlank(caseView.getDestinationIp())&&caseView.getDestinationIp().length()<6){//没有对端ip
                log.info(DateUtil.now() +"\t"+"case:"+caseView.getCaseId()+"\ttype:单线丢包"+"\t"+ GetResults.packetLossDispose(paramMap,caseId,connTotal));
            }else{//有对端ip
                //CIA ISP =INTERNET
                //TCP TCE==VPN
                log.info(DateUtil.now() +"\t"+"case:"+caseView.getCaseId()+"\ttype:单线丢包 有对端线路 互联网"+"\t"+ GetResults.packetLossDestinationDispose(paramMap,caseId,connTotal));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {


    }
}
