package tbcloud.user.api.http.req;

/**
 * @author dzh
 * @date 2019-03-14 15:19
 */
public class NodeWifiReq extends NodeReq {

    private Integer freq;

    private String ssid;

    private String passwd;

    private Integer rssi;

    public Integer getFreq() {
        return freq;
    }

    public void setFreq(Integer freq) {
        this.freq = freq;
    }

    public String getSsid() {
        return ssid;
    }

    public void setSsid(String ssid) {
        this.ssid = ssid;
    }

    public String getPasswd() {
        return passwd;
    }

    public void setPasswd(String passwd) {
        this.passwd = passwd;
    }

    public Integer getRssi() {
        return rssi;
    }

    public void setRssi(Integer rssi) {
        this.rssi = rssi;
    }
}
