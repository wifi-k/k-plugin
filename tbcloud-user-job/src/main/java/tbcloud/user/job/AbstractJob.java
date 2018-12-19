package tbcloud.user.job;

/**
 * @author dzh
 * @date 2018-11-19 17:07
 */
abstract class AbstractJob implements AutoCloseable {

    abstract protected String id();

}
