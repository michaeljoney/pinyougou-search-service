package com.pinyougou.search.service;

import java.util.List;
import java.util.Map;

public interface ItemSearchService {
    /**
     * 搜索
     * @param keywords
     * @return
     */
    Map<String, Object> search(Map searchMap);

    //批量化导入数据到solr；
    void importList(List list);

    void deletByGoodsId(List list);
}
