package tbcloud.httpproxy.job.impl;

import jframe.core.msg.Msg;
import tbcloud.httpproxy.job.HttpProxyJob;
import tbcloud.httpproxy.model.HttpProxyRecord;
import tbcloud.lib.api.msg.MsgType;
import tbcloud.lib.api.util.GsonUtil;

/**
 * @author dzh
 * @date 2019-01-28 23:14
 */
public class AddHttpProxyRecordJob extends HttpProxyJob {
    @Override
    public int msgType() {
        return MsgType.HTTPPROXY_RECORD_ADD;
    }

    @Override
    protected void doJob(Msg<?> msg) throws Exception {
        Object val = msg.getValue();
        if (val == null) {
            return;
        }

        HttpProxyRecord record = null;
        if (val instanceof String) { // maybe from mq in the future
            record = GsonUtil.fromJson((String) val, HttpProxyRecord.class);
        } else if (val instanceof HttpProxyRecord) {
            record = (HttpProxyRecord) val;
        }

        if (record == null) { // TODO why
            return;
        }

        plugin().elastic().indexRecord(record);  //TODO batch
    }

    @Override
    protected String id() {
        return "AddHttpProxyRecordJob";
    }
}
