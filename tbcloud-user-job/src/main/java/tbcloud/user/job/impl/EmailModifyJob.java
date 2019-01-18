package tbcloud.user.job.impl;

import com.aliyuncs.dm.model.v20151123.SingleSendMailRequest;
import com.aliyuncs.exceptions.ClientException;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateExceptionHandler;
import jframe.core.msg.Msg;
import jframe.core.plugin.annotation.InjectService;
import jframe.core.plugin.annotation.Injector;
import tbcloud.lib.api.ApiConst;
import tbcloud.lib.api.msg.EmailModify;
import tbcloud.lib.api.msg.MsgType;
import tbcloud.lib.api.util.GsonUtil;
import tbcloud.user.job.UserJob;

import java.io.*;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

/**
 * https://help.aliyun.com/document_detail/29459.html?spm=a2c4g.11186623.6.601.744e4098hN2uMh
 *
 * @author dzh
 * @date 2018-11-20 13:55
 */
@Injector
public class EmailModifyJob extends UserJob {


    @InjectService(id = "jframe.service.aliyun.dm")
    static jframe.aliyun.service.DMService DMService;

    private Configuration cfg;

    @Override
    public int msgType() {
        return MsgType.EMAIL_MODIFY;
    }

    @Override
    public void start() {
        super.start();

        initFreemarker();
    }

    private void initFreemarker() {
        String ftlDir = plugin().getConfig(ApiConst.FTL_DIR);
        cfg = new Configuration(Configuration.VERSION_2_3_27);
        try {
            cfg.setDirectoryForTemplateLoading(new File(ftlDir));
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
            return;
        }
        cfg.setDefaultEncoding(ApiConst.UTF8.name());
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        cfg.setLogTemplateExceptions(false);
        cfg.setWrapUncheckedExceptions(true);
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
        } else {
            return;
        }

        if (emailModify != null) { // send email
            String from = plugin().getConfig(ApiConst.USER_EMAIL_MODIFY_FROM);
            String to = emailModify.getEmail();
            String subject = "欢迎您使用树熊云！立即激活您的邮箱";
            // content
            String name = emailModify.getName();
            String link = plugin().getConfig(ApiConst.USER_EMAIL_MODIFY_LINK) + URLEncoder.encode("token="
                    + emailModify.getToken() + "&email=" + emailModify.getEmail(), ApiConst.UTF8);
            String content = emailContent(name, link);

            SingleSendMailRequest request = new SingleSendMailRequest();
            try {
                //request.setVersion("2017-06-22");
                request.setVersion("2015-11-23");//hz
                request.setAccountName(from);
                request.setFromAlias("树熊云用户平台");
                request.setAddressType(1);
                request.setTagName("userEmailModify");
                request.setReplyToAddress(false);
                request.setToAddress(to);
                //可以给多个收件人发送邮件，收件人之间用逗号分开，批量发信建议使用BatchSendMailRequest方式
                //request.setToAddress("邮箱1,邮箱2");
                request.setSubject(subject);
                request.setHtmlBody(content);

                LOG.info("send dm {} {}", ApiConst.ALIDM_ID_TBC, GsonUtil.toJson(request));
                DMService.singleSend(ApiConst.ALIDM_ID_TBC, request);
            } catch (ClientException e) {
                LOG.error(e.getMessage(), e);
                //TODO retry
            }

        }
    }

    private String emailContent(String name, String link) {
        Map<String, Object> root = new HashMap<>();
        root.put("tpl.name", name);
        root.put("tpl.link", link);
        try {
            Template temp = cfg.getTemplate("emailverify.ftlh");
            ByteArrayOutputStream baos = new ByteArrayOutputStream(2048);
            Writer out = new OutputStreamWriter(baos);
            temp.process(root, out);
            return new String(baos.toString(ApiConst.UTF8));
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }


        return "<h2>确认您的电子邮件地址</h2><p>尊敬的" + name + "，</p><p>电子邮箱确认请点击链接并输入登录密码确认</p> <p><a href=\"" + link + "\">点击此处激活邮箱</a></p><p>此致 树熊云</p>";
    }

    @Override
    protected String id() {
        return "EmailModifyJob";
    }
}
