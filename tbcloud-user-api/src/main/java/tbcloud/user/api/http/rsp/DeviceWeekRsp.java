package tbcloud.user.api.http.rsp;

import tbcloud.node.model.NodeDeviceWeek;

/**
 * @author dzh
 * @date 2019-04-04 11:52
 */
public class DeviceWeekRsp extends NodeDeviceWeek {

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
}
