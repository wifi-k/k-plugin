package tbcloud.user.job.impl;

import com.qiniu.http.Response;
import com.qiniu.storage.BucketManager;
import jframe.core.msg.Msg;
import jframe.core.plugin.annotation.InjectService;
import jframe.core.plugin.annotation.Injector;
import jframe.qiniu.QiniuConfig;
import jframe.qiniu.service.QiniuService;
import tbcloud.lib.api.msg.DeleteQiniuObject;
import tbcloud.lib.api.msg.MsgType;
import tbcloud.lib.api.util.GsonUtil;
import tbcloud.lib.api.util.StringUtil;
import tbcloud.user.job.UserJob;

import java.util.List;

/**
 * @author dzh
 * @date 2018-12-28 16:54
 */
@Injector
public class QiniuDeleteJob extends UserJob {

    @InjectService(id = "jframe.service.qiniu")
    static QiniuService Qiniu;

    @Override
    public int msgType() {
        return MsgType.DELETE_QINIU_OBJECT;
    }

    @Override
    protected void doJob(Msg<?> msg) throws Exception {
        Object val = msg.getValue();
        if (val == null) {
            return;
        }

        DeleteQiniuObject deleteObject = null;
        if (val instanceof String) { // mq in the future
            deleteObject = GsonUtil.fromJson((String) val, DeleteQiniuObject.class);
        } else if (val instanceof DeleteQiniuObject) {
            deleteObject = (DeleteQiniuObject) val;
        } else {
            return;
        }

        // bucket
        String bucket = Qiniu.config(deleteObject.getId(), QiniuConfig.BUCKET);
        if (StringUtil.isEmpty(bucket)) {
            LOG.error("not found bucket with id {}", deleteObject.getId());
            return;
        }

        // deleted key
        List<String> deletedKey = deleteObject.getKey();
        if (deletedKey == null || deletedKey.isEmpty()) {
            LOG.warn("DeleteQiniuObject key is empty");
            return;
        }

        BucketManager.BatchOperations batchDel = new BucketManager.BatchOperations();
        batchDel.addDeleteOp(bucket, deletedKey.toArray(new String[deletedKey.size()]));

        BucketManager bucketManager = Qiniu.bucketManager();
        Response r = bucketManager.batch(batchDel);
        LOG.info("batchDel {}", r);
    }

    @Override
    protected String id() {
        return "QiniuDeleteJob";
    }
}
