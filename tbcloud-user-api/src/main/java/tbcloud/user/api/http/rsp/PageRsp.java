package tbcloud.user.api.http.rsp;

import java.util.List;

/**
 * @author dzh
 * @date 2018-11-12 17:54
 */
public class PageRsp<T> {

    private long count;
    private List<T> page;

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public List<T> getPage() {
        return page;
    }

    public void setPage(List<T> page) {
        this.page = page;
    }
}
