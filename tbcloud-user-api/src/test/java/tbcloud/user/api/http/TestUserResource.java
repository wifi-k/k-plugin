package tbcloud.user.api.http;

import org.junit.Test;

/**
 * @author dzh
 * @date 2018-11-14 19:19
 */
public class TestUserResource {

    @Test
    public void bindBatchNodeTest() {
        String file = "a\r\nb\n";
        String[] list = file.split("\r");
        System.out.println(file);
        System.out.println(list.length);
    }
}
