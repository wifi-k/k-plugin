package tbcloud.user.api.http.req;

/**
 * @author dzh
 * @date 2019-04-04 14:22
 */
public class NodeDeviceReq extends NodeReq {

    private Integer isRecord;
    private Integer isBlock;

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
