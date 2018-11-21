package tbcloud.user.api.http.req;

/**
 * @author dzh
 * @date 2018-11-21 15:19
 */
public class DatePageReq extends PageReq {

    private Long startTime;
    private Long endTime;

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
