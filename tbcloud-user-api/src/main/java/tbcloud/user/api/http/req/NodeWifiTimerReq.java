package tbcloud.user.api.http.req;

/**
 * @author dzh
 * @date 2019-03-27 11:54
 */
public class NodeWifiTimerReq extends NodeReq {

    private Integer op;
    private String wifi;

    public Integer getOp() {
        return op;
    }

    public void setOp(Integer op) {
        this.op = op;
    }

    public String getWifi() {
        return wifi;
    }

    public void setWifi(String wifi) {
        this.wifi = wifi;
    }
}
