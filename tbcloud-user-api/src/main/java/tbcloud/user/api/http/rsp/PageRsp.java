package tbcloud.user.api.http.rsp;

import java.util.List;

/**
 * @author dzh
 * @date 2018-11-12 17:54
 */
public class PageRsp<T> {

    private long total;
    private List<T> page;

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public List<T> getPage() {
        return page;
    }

    public void setPage(List<T> page) {
        this.page = page;
    }
}
