package tbcloud.user.job.impl;

import com.aliyuncs.dysmsapi.model.v20170525.SendSmsRequest;
import com.aliyuncs.dysmsapi.model.v20170525.SendSmsResponse;
import com.aliyuncs.http.MethodType;
import jframe.aliyun.service.SMSService;
import jframe.core.msg.Msg;
import jframe.core.plugin.annotation.InjectService;
import jframe.core.plugin.annotation.Injector;
import tbcloud.lib.api.ApiConst;
import tbcloud.lib.api.msg.MobileVCode;
import tbcloud.lib.api.msg.MsgType;
import tbcloud.lib.api.util.GsonUtil;
import tbcloud.user.job.UserJob;

/**
 * https://help.aliyun.com/document_detail/55284.html?spm=a2c4g.11186623.6.566.5d1b4175GzAYkw
 *
 * @author dzh
 * @date 2018-11-19 17:55
 */
@Injector
public class MobileVCodeJob extends UserJob {

    @InjectService(id = "jframe.service.aliyun.sms")
    static SMSService SmsService;

    @Override
    public int msgType() {
        return MsgType.MOBILE_VCODE;
    }

    @Override
    protected void doJob(Msg<?> msg) throws Exception {
        Object val = msg.getValue();
        if (val == null) {
            return;
        }

        MobileVCode vcodeSend = null;
        if (val instanceof String) { // maybe from mq in the future
            vcodeSend = GsonUtil.fromJson((String) val, MobileVCode.class);
        } else if (val instanceof MobileVCode) {
            vcodeSend = (MobileVCode) val;
        } else {
            return;
        }

        if (vcodeSend != null) {
            // TODO check mobile

            switch (vcodeSend.getType()) {
                case ApiConst
                        .MOBILE_VCODE_USER_REG:
                    regUser(vcodeSend);
                    break;
                case ApiConst.MOBILE_VCODE_PASSWD_RESET:
                    resetPasswd(vcodeSend);
                    break;
                case ApiConst.MOBILE_VCODE_MOBILE_RESET:
                    resetMobile(vcodeSend);
                    break;
                default:
                    LOG.error("error MobileVCode type:" + vcodeSend.getType());
            }
        }
    }

    private void resetMobile(MobileVCode vcodeSend) {
        SendSmsRequest request = new SendSmsRequest();
        request.setMethod(MethodType.POST);
        request.setPhoneNumbers(vcodeSend.getMobile());
        request.setSignName("树熊云");
        request.setTemplateCode("SMS_150480348");
        request.setTemplateParam("{\"code\":\"" + vcodeSend.getCode() + "\"}");

        doSend(ApiConst.ALISMS_ID_TBC, request);
    }

    private void resetPasswd(MobileVCode vcodeSend) {
        SendSmsRequest request = new SendSmsRequest();
        request.setMethod(MethodType.POST);
        request.setPhoneNumbers(vcodeSend.getMobile());
        request.setSignName("树熊云");
        request.setTemplateCode("SMS_150480349");
        request.setTemplateParam("{\"code\":\"" + vcodeSend.getCode() + "\"}");

        doSend(ApiConst.ALISMS_ID_TBC, request);
    }

    private void regUser(MobileVCode vcodeSend) {
        SendSmsRequest request = new SendSmsRequest();
        request.setMethod(MethodType.POST);
        request.setPhoneNumbers(vcodeSend.getMobile());
        request.setSignName("树熊云");
        request.setTemplateCode("SMS_150480350");
        request.setTemplateParam("{\"code\":\"" + vcodeSend.getCode() + "\"}");

        doSend(ApiConst.ALISMS_ID_TBC, request);
    }

    void doSend(String id, SendSmsRequest request) {
        LOG.info("send sms {} {}", id, GsonUtil.toJson(request));
        try {
            SendSmsResponse rsp = SmsService.send(id, request);
            if (rsp != null && !"OK".equalsIgnoreCase(rsp.getCode())) {
                LOG.warn("send error {}", GsonUtil.toJson(rsp));
            }
        } catch (Exception e) {  //TODO
            LOG.error(e.getMessage(), e);
        }
    }

    @Override
    protected String id() {
        return "MobileVCodeJob";
    }
}
