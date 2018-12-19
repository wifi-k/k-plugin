package tbcloud.timer.impl;

import tbcloud.common.model.IpInfo;
import tbcloud.common.model.IpInfoExample;
import tbcloud.lib.api.util.StringUtil;

import java.util.List;

/**
 * 补充ip_info里的地区信息 TODO job
 *
 * @author dzh
 * @date 2018-12-13 19:43
 */
public class IpInfoFind extends MutexTimer {

    @Override
    protected int doTimer() {
        IpInfoExample example = new IpInfoExample();
        example.createCriteria().andCityIdIsNull();
        example.setOrderByClause("create_time limit 100"); //TODO 依据最大QPS限制设置

        List<IpInfo> list = CommonDao.selectIpInfo(example);
        list.forEach(ipInfo -> {
            //TODO search ip info

            // update IpInfo
            if (!StringUtil.isEmpty(ipInfo.getCityId()) && !StringUtil.isEmpty(ipInfo.getIspId()))
                CommonDao.updateIpInfo(ipInfo);
        });

        return list.size();
    }

    @Override
    protected String path() {
        return "/common/ip/find";
    }

    @Override
    protected long delayMs() {
        return 10 * 1000;
    }

    @Override
    protected String name() {
        return "IpInfoFind";
    }
}
