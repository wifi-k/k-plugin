package tbcloud.user.api.http.req;

/**
 * @author dzh
 * @date 2018-11-20 15:26
 */
public class NodeListReq extends PageReq {

    private Integer status = -1;

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
