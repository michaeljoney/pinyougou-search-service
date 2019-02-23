package com.pinyougou.search.service.impl;

import com.alibaba.dubbo.config.annotation.Service;

import com.pinyougou.pojo.TbItem;
import com.pinyougou.search.service.ItemSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.core.query.*;
import org.springframework.data.solr.core.query.result.*;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/********
 * author:Aligan
 * date2018/10/6 14:53
 * description:Aligan
 * version:1.0
 ******/

@Service
public class ItemSearchServiceImpl implements ItemSearchService {

    @Autowired
    private SolrTemplate solrTemplate;

    @Autowired
    private RedisTemplate redisTemplate;
    @Override
    public Map<String, Object> search(Map searchMap) {
        String value = (String) searchMap.get("keywords");

        if(null !=value) {
            System.out.println("搜索的关键字是;"+value);
            searchMap.put("keywords",value.replace(" ",""));
        }

        Map map=new HashMap();
        //高亮显示查询
        map.putAll(searchList(searchMap));
        //分组查询
        List<String> categoryList = searchCategoryList(searchMap);
        map.put("categoryList",categoryList);

        //判断浏览器端是否传过来有商品分类内容,浏览器传过来的优先
        if(!"".equals(searchMap.get("category"))){//若searchMap.get("category")不为空引号；则说名有内容
            String  categoryName = (String) searchMap.get("category");
            Map map1 = searchBrandAndSpecList(categoryName);
            map.putAll(map1);
        }else {
            //查询分类
            if(categoryList.size()>0){
                System.out.println("categoryList内容是："+categoryList);
                Map map1 = searchBrandAndSpecList(categoryList.get(0));
                map.putAll(map1);

            }
        }





        return map;


    }




    private Map searchList(Map searchMap) {
        Map map=new HashMap();
        //高亮显示关键字
        //创建高亮查询对象
        HighlightQuery query=new SimpleHighlightQuery();
        //创建高亮选项对象
        HighlightOptions highlightOptions = new HighlightOptions().addField("item_title");//设置高亮显示的域
        highlightOptions.setSimplePrefix("<em style='color:red'>");//设置高亮显示前缀
        highlightOptions.setSimplePostfix("</em>");//设置高亮显示后缀
        //将高亮选项对象参数放入查询方法
        query.setHighlightOptions(highlightOptions);
        //设置查询条件
        Criteria criteria = new Criteria("item_keywords").is(searchMap.get("keywords"));
        query.addCriteria(criteria);

        //商品类型分组查询

        if(!"".equals(searchMap.get("category"))){
            Criteria filterCriteria = new Criteria("item_category").is(searchMap.get("category"));
            SimpleFilterQuery filterQuery = new SimpleFilterQuery(filterCriteria);
            query.addFilterQuery(filterQuery);
        }
        //商品品牌分组查询!"".equals(searchMap.get("brand"))    equals(searchMap.get("brand"))!=null
        if(!"".equals(searchMap.get("brand"))){
            Criteria filterCriteria=new Criteria("item_brand").is(searchMap.get("brand"));
            SimpleFilterQuery simpleFilterQuery = new SimpleFilterQuery(filterCriteria);
            query.addFilterQuery(simpleFilterQuery);
        }
        //商品规格分组查询
        if(searchMap.get("spec")!=null){
            Map<String,String> spec = (Map)searchMap.get("spec");
            for (String key : spec.keySet()) {
                Criteria filterCriteria = new Criteria("item_spec_" + key).is(spec.get(key));
                SimpleFilterQuery filterQuery = new SimpleFilterQuery(filterCriteria);
                query.addFilterQuery(filterQuery);

            }
        }

        //根据价格分组
        if(!"".equals(searchMap.get("price"))){
            String  price = (String) searchMap.get("price");
            String[] prices = price.split("-");
            //if(prices[0]!="0")){
            if(!prices[0].equals("0")){
                // String[]  lowprice=((String )searchMap.get("price")).split("-");
                Criteria filterCriteria=new Criteria("item_price").greaterThan(prices[0]);
                SimpleFilterQuery filterQuery = new SimpleFilterQuery(filterCriteria);
                query.addFilterQuery(filterQuery);
            }
            if(!prices[1].equals("*")){
                // String[]  lowprice=((String )searchMap.get("price")).split("-");
                Criteria filterCriteria=new Criteria("item_price").lessThan(prices[1]);
                SimpleFilterQuery filterQuery = new SimpleFilterQuery(filterCriteria);
                query.addFilterQuery(filterQuery);
            }

        }

        //分页
        Integer pageNo = (Integer) searchMap.get("pageNo");//获得当前页码
        if(pageNo==null){
            pageNo=1;
        }
        Integer pageSize = (Integer) searchMap.get("pageSize");//获得当前页码
        if(pageSize==null){
            pageSize=20;
        }
        query.setOffset((pageNo-1)*pageSize); //设置起始显示显示页面
        query.setRows(pageSize);//设置显示显示页面的记录数

        //排序
        String fieldName = (String) searchMap.get("sortField");
        String sort = (String) searchMap.get("sort");
        if(sort!=""){
            if(sort.equals("ASC")){
                Sort orders =new Sort(Sort.Direction.ASC,"item_"+fieldName) ;
                query.addSort(orders);
            }
            if(sort.equals("DESC")){
                Sort orders =new Sort(Sort.Direction.DESC,"item_"+fieldName) ;
                query.addSort(orders);
            }
        }




        //根据查询条件进行查询
        HighlightPage<TbItem> page = solrTemplate.queryForHighlightPage(query, TbItem.class);//page里面的内容现在还是没有加高亮的原始查询结果
        for (HighlightEntry<TbItem> entry : page.getHighlighted()) {//获取每个高亮的入口
            TbItem entity = entry.getEntity();//获取原实体类
            if(entry.getHighlights().size()>0&&entry.getHighlights().get(0).getSnipplets().size()>0){
                    //为防止get（0）里面的东西报空指针异常错误，提前进行判断一下；
                entity.setTitle(entry.getHighlights().get(0).getSnipplets().get(0));//设置高亮显示部分。getHighlights()获取一个实体中所有高亮的关键字，
            }
        }
        map.put("totalPage",page.getTotalPages());
        map.put("totalElement",page.getTotalElements());
        map.put("rows",page.getContent());

        return map;
    }
    private List searchCategoryList(Map searchMap) {
        ArrayList<String> list = new ArrayList<>();
        //创建查询对象
        Query query = new SimpleQuery("*:*");
        //根据查询条件进行查询
        Criteria criteria=new Criteria("item_keywords").is(searchMap.get("keywords"));
        query.addCriteria(criteria);
        //设置分组查询的条件为根据item_category字段进行分组
        GroupOptions groupOptions = new GroupOptions().addGroupByField("item_category");
        query.setGroupOptions(groupOptions);//将分组条件加入查询搜索

        //得到一个分组页
        GroupPage<TbItem> page = solrTemplate.queryForGroupPage(query, TbItem.class);
        //得到一个分组结果集
        GroupResult<TbItem> category = page.getGroupResult("item_category");
        //得到一个分组入口页
        Page<GroupEntry<TbItem>> groupEntries = category.getGroupEntries();
      //得到一个分组结果入口集合
        List<GroupEntry<TbItem>> content = groupEntries.getContent();
        for (GroupEntry<TbItem> entry : content) {
            list.add(entry.getGroupValue());
        }

        return list;
    }

    //读取缓存中分类和品牌和规格
    private Map searchBrandAndSpecList(String category) {

        System.out.println("searchBrandAndSpecList方法传入的category值是："+category);

        Map map=new HashMap();
        Long typeId = (Long) redisTemplate.boundHashOps("itemCat").get(category);
        //获取品牌的缓存内容
        List brandList = (List) redisTemplate.boundHashOps("brandList").get(typeId);
        //获取规格的缓存内容
        List specList = (List) redisTemplate.boundHashOps("specList").get(typeId);

        map.put("brandList",brandList);
        map.put("specList",specList);
        return map;
    }
    //批量化导入数据到solr；
    @Override
    public void importList(List list) {
        solrTemplate.saveBeans(list);
        solrTemplate.commit();
    }

    @Override
    public void deletByGoodsId(List list) {

        Query query = new SimpleQuery();
        Criteria criteria = new Criteria("item_goodsId").in(list);
        query.addCriteria(criteria);
        solrTemplate.delete(query);
        solrTemplate.commit();
    }
}
