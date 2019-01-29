package tbcloud.user.api.http.req;

/**
 * @author dzh
 * @date 2019-01-29 01:58
 */
public class HttpProxyRecordReq extends PageReq {

    private String id; //recordId
    private Long startTime;
    private Long endTime;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Long getStartTime() {
        return startTime;
    }

    public void setStartTime(Long startTime) {
        this.startTime = startTime;
    }

    public Long getEndTime() {
        return endTime;
    }

    public void setEndTime(Long endTime) {
        this.endTime = endTime;
    }
}
