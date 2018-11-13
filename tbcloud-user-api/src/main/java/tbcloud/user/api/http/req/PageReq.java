package tbcloud.user.api.http.req;

/**
 * @author dzh
 * @date 2018-11-12 17:56
 */
public class PageReq {

    private Integer pageNo;
    private Integer pageCount;

    public Integer getPageNo() {
        if (pageNo == null || pageNo < 1) return 1;
        return pageNo;
    }

    public void setPageNo(Integer pageNo) {
        this.pageNo = pageNo;
    }

    public Integer getPageCount() {
        if (pageCount == null || pageCount < 0) return 10;
        if (pageCount > 100) return 100;
        return pageCount;
    }

    public void setPageCount(Integer pageCount) {
        this.pageCount = pageCount;
    }
}
