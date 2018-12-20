package tbcloud.common.dao.service;

import tbcloud.common.model.*;

import java.util.List;

/**
 * @author dzh
 * @date 2018-12-13 16:14
 */
public interface AreaDao {
    Province selectProvince(String provinceId);

    List<Province> selectProvince(ProvinceExample example);

    long countProvince(ProvinceExample example);

    City selectCity(String cityId);

    List<City> selectCity(CityExample example);

    long countCity(CityExample example);

    Area selectArea(String areaId);

    List<Area> selectArea(AreaExample example);

    long countArea(AreaExample example);
}
