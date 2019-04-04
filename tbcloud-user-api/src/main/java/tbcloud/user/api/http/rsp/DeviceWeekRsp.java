package tbcloud.user.api.http.rsp;

import tbcloud.node.model.NodeDeviceWeek;

/**
 * @author dzh
 * @date 2019-04-04 11:52
 */
public class DeviceWeekRsp {

    private Integer year;
    private Integer week;
    private String nodeId;
    private String mac;
    private Long totalTime;
    private String macNote;
    private String hostName;
    private String macVendor;
    private String macIcon;

    private String remark;

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public static final DeviceWeekRsp from(NodeDeviceWeek week) {
        DeviceWeekRsp rsp = new DeviceWeekRsp();
        rsp.setYear(week.getYear());
        rsp.setWeek(week.getWeek());
        rsp.setNodeId(week.getNodeId());
        rsp.setMac(week.getMac());
        rsp.setTotalTime(week.getTotalTime());
        rsp.setMacNote(week.getMacNote());
        rsp.setHostName(week.getHostName());
        rsp.setMacVendor(week.getMacVendor());
        rsp.setMacIcon(week.getMacIcon());

        rsp.initRemark();
        return rsp;
    }

    public void initRemark() {
        this.remark = "上周平均每日活跃时长: " + totalTime / 3600000 + " 小时";
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public Integer getWeek() {
        return week;
    }

    public void setWeek(Integer week) {
        this.week = week;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public Long getTotalTime() {
        return totalTime;
    }

    public void setTotalTime(Long totalTime) {
        this.totalTime = totalTime;
    }

    public String getMacNote() {
        return macNote;
    }

    public void setMacNote(String macNote) {
        this.macNote = macNote;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public String getMacVendor() {
        return macVendor;
    }

    public void setMacVendor(String macVendor) {
        this.macVendor = macVendor;
    }

    public String getMacIcon() {
        return macIcon;
    }

    public void setMacIcon(String macIcon) {
        this.macIcon = macIcon;
    }
}
