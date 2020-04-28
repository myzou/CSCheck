package com.cter.AutoCheck;

/**
 * remedy 提供的对应case 的View实体类
 */
public class CaseView {

    private String caseId;
    private String peName;
    private String interfaceName;
    private String vrf;
    private String caseSummary;
    private String ceWanIp;
    private String peWanIp;
    private String siteId;
    private String status;
    private String destinationIp;//对端ip
    private String sourceIp;//资源ip,jason说留着备用
    private String trunkName;
    private String siteNum;//编辑是site1 还是 site2
    private String webService;//页面显示的service
    private String webType;//界面显示的类型
    private String webItem;//界面显示的Item



    public String getCaseId() {
        return caseId;
    }

    public void setCaseId(String caseId) {
        this.caseId = caseId;
    }

    public String getPeName() {
        return peName;
    }

    public void setPeName(String peName) {
        this.peName = peName;
    }

    public String getInterfaceName() {
        return interfaceName;
    }

    public void setInterfaceName(String interfaceName) {
        this.interfaceName = interfaceName;
    }

    public String getVrf() {
        return vrf;
    }

    public void setVrf(String vrf) {
        this.vrf = vrf;
    }

    public String getCaseSummary() {
        return caseSummary;
    }

    public void setCaseSummary(String caseSummary) {
        this.caseSummary = caseSummary;
    }

    public String getCeWanIp() {
        return ceWanIp;
    }

    public void setCeWanIp(String ceWanIp) {
        this.ceWanIp = ceWanIp;
    }

    public String getPeWanIp() {
        return peWanIp;
    }

    public void setPeWanIp(String peWanIp) {
        this.peWanIp = peWanIp;
    }

    public String getSiteId() {
        return siteId;
    }

    public void setSiteId(String siteId) {
        this.siteId = siteId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDestinationIp() {
        return destinationIp;
    }

    public void setDestinationIp(String destinationIp) {
        this.destinationIp = destinationIp;
    }

    public String getSourceIp() {
        return sourceIp;
    }

    public void setSourceIp(String sourceIp) {
        this.sourceIp = sourceIp;
    }

    public String getTrunkName() {
        return trunkName;
    }

    public void setTrunkName(String trunkName) {
        this.trunkName = trunkName;
    }

    public String getSiteNum() {
        return siteNum;
    }

    public void setSiteNum(String siteNum) {
        this.siteNum = siteNum;
    }

    public String getWebService() {
        return webService;
    }

    public void setWebService(String webService) {
        this.webService = webService;
    }

    public String getWebType() {
        return webType;
    }

    public void setWebType(String webType) {
        this.webType = webType;
    }

    public String getWebItem() {
        return webItem;
    }

    public void setWebItem(String webItem) {
        this.webItem = webItem;
    }
}
