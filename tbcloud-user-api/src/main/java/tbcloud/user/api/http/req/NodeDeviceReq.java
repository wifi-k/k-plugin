package tbcloud.user.api.http.req;

/**
 * @author dzh
 * @date 2019-04-04 14:22
 */
public class NodeDeviceReq extends NodeReq {

    private Integer isRecord;
    private Integer isBlock;

    private String mac;
    private String note;
    private Integer IsOnline;

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Integer getIsOnline() {
        return IsOnline;
    }

    public void setIsOnline(Integer isOnline) {
        IsOnline = isOnline;
    }

    public Integer getIsRecord() {
        return isRecord;
    }

    public void setIsRecord(Integer isRecord) {
        this.isRecord = isRecord;
    }

    public Integer getIsBlock() {
        return isBlock;
    }

    public void setIsBlock(Integer isBlock) {
        this.isBlock = isBlock;
    }
}
