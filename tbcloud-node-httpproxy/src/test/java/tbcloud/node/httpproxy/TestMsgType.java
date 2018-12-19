package tbcloud.node.httpproxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author dzh
 * @date 2018-12-14 10:46
 */
public class TestMsgType {

    static Logger LOG = LoggerFactory.getLogger(TestMsgType.class);

    public void msgTypeTest() {


    }

    public static class A<T> {
        private T data;

        public T getData() {
            return data;
        }

        public void setData(T data) {
            this.data = data;
        }
    }
}
