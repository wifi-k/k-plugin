package tbcloud.user.job.impl;

import jframe.core.msg.Msg;
import tbcloud.lib.api.msg.EmailModify;
import tbcloud.lib.api.msg.MsgType;
import tbcloud.lib.api.util.GsonUtil;
import tbcloud.user.job.UserJob;

/**
 * https://help.aliyun.com/document_detail/29459.html?spm=a2c4g.11186623.6.601.744e4098hN2uMh
 *
 * @author dzh
 * @date 2018-11-20 13:55
 */
public class EmailModifyJob extends UserJob {

    @Override
    public int msgType() {
        return MsgType.EMAIL_MODIFY;
    }

    @Override
    protected void doJob(Msg<?> msg) throws Exception {
        Object val = msg.getValue();
        if (val == null) {
            return;
        }

        EmailModify emailModify = null;
        if (val instanceof String) { // maybe from mq in the future
            emailModify = GsonUtil.fromJson((String) val, EmailModify.class);
        } else if (val instanceof EmailModify) {
            emailModify = (EmailModify) val;
        }

        if (emailModify != null) {
            // send email
            String from = ""; //TODO
            String to = emailModify.getEmail();
            String subject = "欢迎您使用树熊云！立即激活您的邮箱";
            String content = ""; //TODO html

        }
    }

    @Override
    protected String id() {
        return "emailmodify";
    }
}
