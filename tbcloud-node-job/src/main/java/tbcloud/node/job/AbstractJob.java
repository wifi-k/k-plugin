package tbcloud.node.job;

/**
 * @author dzh
 * @date 2018-11-19 17:07
 */
public abstract class AbstractJob implements AutoCloseable {

    abstract protected String id();

}
