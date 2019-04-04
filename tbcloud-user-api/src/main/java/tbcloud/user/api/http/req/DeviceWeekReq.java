package tbcloud.user.api.http.req;

/**
 * @author dzh
 * @date 2019-04-04 11:51
 */
public class DeviceWeekReq extends NodeReq {

    private Integer year;

    private Integer week;

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
}
