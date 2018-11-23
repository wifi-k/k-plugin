package tbcloud.user.api.http.req;

/**
 * @author dzh
 * @date 2018-11-12 17:56
 */
public class PageReq {

    private Integer pageNo;
    private Integer pageSize;

    public Integer getPageNo() {
        if (pageNo == null || pageNo < 1) return 1;
        return pageNo;
    }

    public void setPageNo(Integer pageNo) {
        this.pageNo = pageNo;
    }

    public Integer getPageSize() {
        if (pageSize == null || pageSize < 0) return 10;
        if (pageSize > 100) return 100;
        return pageSize;
    }

    public void setPageSize(Integer pageSizef) {
        this.pageSize = pageSize;
    }
}
