package tbcloud.httpproxy.dao.service;

import tbcloud.httpproxy.model.HttpProxyOnline;
import tbcloud.httpproxy.model.HttpProxyOnlineExample;
import tbcloud.httpproxy.model.HttpProxyRecord;
import tbcloud.httpproxy.model.HttpProxyRecordExample;

import java.util.List;

/**
 * @author dzh
 * @date 2018-12-11 12:16
 */
public interface HttpProxyDao {

    int insertHttpProxyRecord(HttpProxyRecord httpProxyRecord);

    List<HttpProxyRecord> selectHttpProxyRecord(HttpProxyRecordExample example);

    HttpProxyRecord selectHttpProxyRecord(String id);

    int updateHttpProxyRecord(HttpProxyRecord httpProxyRecord);


    int insertHttpProxyOnline(HttpProxyOnline httpProxyOnline);

    HttpProxyOnline selectHttpProxyOnline(String nodeId);

    int updateHttpProxyOnline(HttpProxyOnline httpProxyOnline);

    List<HttpProxyOnline> selectHttpProxyOnline(HttpProxyOnlineExample example);


}
