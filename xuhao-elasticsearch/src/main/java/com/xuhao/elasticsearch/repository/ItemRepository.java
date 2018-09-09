package com.xuhao.elasticsearch.repository;

import com.xuhao.elasticsearch.pojo.Item;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

/**
 * @author XuHao
 * @Title: ItemRepository
 * @ProjectName xuhao-demo
 * @Description: TODO
 * @date 2018/9/99:16
 */
public interface ItemRepository extends ElasticsearchRepository<Item,Long> {

    /**
     * 按照价格区间查询
     * @param start
     * @param end
     * @return
     */
    List<Item> findByPriceBetween(double start,double end);

}
