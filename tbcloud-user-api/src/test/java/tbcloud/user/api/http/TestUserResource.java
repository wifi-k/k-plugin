package tbcloud.user.api.http;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tbcloud.lib.api.ApiConst;
import tbcloud.lib.api.util.StringUtil;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

/**
 * @author dzh
 * @date 2018-11-14 19:19
 */
public class TestUserResource {

    static Logger LOG = LoggerFactory.getLogger(TestUserResource.class);

    @Test
    public void bindBatchNodeTest() {
        String file = "a\rb\n\rc";
        String[] list = file.split("[\r\n]+");
        System.out.println(Arrays.asList(list));
        System.out.println(list.length);
    }

    @Test
    public void innerImgCodeIdTest() throws InterruptedException {
        String imgCodeId = "1672f1504e248ebdaa22325b68aefd";

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHH");
        imgCodeId += sdf.format(new Date());
        LOG.info("an hour {}", imgCodeId);
        int innerId = Math.abs(imgCodeId.hashCode() % ApiConst.IMGCODE_MAX_ID) + 1;
        LOG.info("innerId {}", innerId);

        Thread.sleep(2000L);

        imgCodeId = "abcd";
        sdf = new SimpleDateFormat("yyyyMMddHH");
        imgCodeId += sdf.format(new Date());
        LOG.info("an hour {}", imgCodeId);
        innerId = Math.abs(imgCodeId.hashCode() % ApiConst.IMGCODE_MAX_ID) + 1;
        LOG.info("innerId {}", innerId);
    }

    @Test
    public void genPasswdTest() {
        String passwd = "daizhong";
        LOG.info("{}", StringUtil.MD5Encode(StringUtil.MD5Encode(passwd) + ApiConst.USER_PASSWD_SALT_1));
    }

}
