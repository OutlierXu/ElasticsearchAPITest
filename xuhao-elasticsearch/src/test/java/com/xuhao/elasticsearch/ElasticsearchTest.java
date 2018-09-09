package com.xuhao.elasticsearch;

import com.xuhao.elasticsearch.pojo.Item;
import com.xuhao.elasticsearch.repository.ItemRepository;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms;
import org.elasticsearch.search.aggregations.metrics.avg.InternalAvg;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.query.FetchSourceFilter;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @author XuHao
 * @Title: indexTest
 * @ProjectName xuhao-demo
 * @Description: Elasticsearch API Test
 * @date 2018/9/98:53
 */
@SpringBootTest
@RunWith(SpringRunner.class)
public class ElasticsearchTest {

    @Autowired
    private ElasticsearchTemplate elasticsearchTemplate;

    @Autowired
    private ItemRepository itemRepository;

    /**
     * 创建索引库
     */
    @Test
    public void creatIndex() {

        elasticsearchTemplate.createIndex(Item.class);
        elasticsearchTemplate.putMapping(Item.class);

    }

    /**
     * 删除索引库
     */
    @Test
    public void deleteIndex() {
        boolean b = elasticsearchTemplate.deleteIndex(Item.class);
        if (b) {

            System.out.println("删除item索引库成功！");
        }
    }

    /**
     * 新增文档
     */
    @Test
    public void addDocument() {
        Item item = new Item(1L, "小米手机7", " 手机",
                "小米", 3499.00, "http://image.leyou.com/13123.jpg");
        this.itemRepository.save(item);
    }

    /**
     * 批量新增
     */
    @Test
    public void addDocuments() {
        List<Item> list = new ArrayList<>();
        list.add(new Item(5L, "小米手机7", "手机", "小米", 3299.00, "http://image.leyou.com/13123.jpg"));
        list.add(new Item(6L, "坚果手机R1", "手机", "锤子", 3699.00, "http://image.leyou.com/13123.jpg"));
        list.add(new Item(7L, "华为META10", "手机", "华为", 4499.00, "http://image.leyou.com/13123.jpg"));
        list.add(new Item(4L, "小米Mix2S", "手机", "小米", 4299.00, "http://image.leyou.com/13123.jpg"));
        list.add(new Item(5L, "荣耀V10", "手机", "华为", 2799.00, "http://image.leyou.com/13123.jpg"));
        this.itemRepository.saveAll(list);
    }

    /**
     * 查询所有
     */
    @Test
    public void findAll() {
        Iterable<Item> itemList = itemRepository.findAll();

        itemList.forEach(System.out::println);
    }

    /**
     * 按id查询
     */
    @Test
    public void findById() {

        Optional<Item> optional = this.itemRepository.findById(2L);
        Item item = optional.get();
        System.out.println(item);

    }

    /**
     * 自定义查询
     */
    @Test
    public void findByPriceBetween() {
        List<Item> itemList = this.itemRepository.findByPriceBetween(3000, 5000);
        itemList.forEach(System.out::println);
    }


    /**
     * 词条查询
     */
    @Test
    public void queryBuilder() {
        //词条查询
        MatchQueryBuilder queryBuilder = QueryBuilders.matchQuery("title", "小米手机");
        //执行查询
        Iterable<Item> itemsList = this.itemRepository.search(queryBuilder);
        itemsList.forEach(System.out::println);

    }

    /**
     * 复杂查询，查询条件的构建
     * 创建构建器：SearchQueryBuilder = new NativeSearchQueryBuilder()；
     * 1.查询条件：withQuery(QueryBuilders.matchQuery("title", "小米手机"));
     * 2.分页条件：withPageable(PageRequest.of(page, size));
     * 3.排序条件：withSort(SortBuilders.fieldSort("price").order(SortOrder.DESC));
     */
    @Test
    public void testNativeQuery() {
        //创建查询条件构建器
        NativeSearchQueryBuilder SearchQueryBuilder = new NativeSearchQueryBuilder();

        //添加查询条件
        SearchQueryBuilder.withQuery(QueryBuilders.matchQuery("title", "小米手机"));

        //设置分页条件
        int page = 0;
        int size = 10;
        SearchQueryBuilder.withPageable(PageRequest.of(page, size));

        //添加排序条件
        SearchQueryBuilder.withSort(SortBuilders.fieldSort("price").order(SortOrder.DESC));

        //执行查询，返回分页结果对象
        Page<Item> itemPage = this.itemRepository.search(SearchQueryBuilder.build());

        System.out.println("匹配文档数：" + itemPage.getTotalElements());
        System.out.println("匹配总页数：" + itemPage.getTotalPages());

        itemPage.forEach(item -> System.out.println(item));
    }

    @Test
    public void testAggregation() {
        NativeSearchQueryBuilder searchQueryBuilder = new NativeSearchQueryBuilder();

        //1.查询结果过滤，不查询任何结果
        searchQueryBuilder.withSourceFilter(new FetchSourceFilter(new String[]{""}, null));

        //2.添加一个聚合，指定聚合方式，聚合名称，聚合字段
        searchQueryBuilder.addAggregation(AggregationBuilders.terms("brand_populate").field("brand"));

        //3.执行查询，将结果强转为AggregationPage<Item>类型
        AggregatedPage<Item> aggPage = (AggregatedPage<Item>) this.itemRepository.search(searchQueryBuilder.build());

        //4.解析结果,取出名为“brand_populate”的桶
        //4.1
        StringTerms agg = (StringTerms) aggPage.getAggregation("brand_populate");

        //4.2获取桶
        List<StringTerms.Bucket> buckets = agg.getBuckets();

        //4.3遍历桶
        buckets.forEach(bucket -> {
            System.out.println(bucket.getKeyAsString());
            System.out.println(bucket.getDocCount());
        });

    }

    @Test
    public void testSubAggregation() {
        NativeSearchQueryBuilder searchQueryBuilder = new NativeSearchQueryBuilder();
        searchQueryBuilder.withSourceFilter(new FetchSourceFilter(new String[]{""}, null));

        //1.添加terms类型的名称为brand_populate的聚合和avg类型名称为price_avg的子聚合，求平均值
        searchQueryBuilder.addAggregation(AggregationBuilders.terms("brand_populate").field("brand")
                .subAggregation(AggregationBuilders.avg("price_avg").field("price")));

        //2.执行查询
         AggregatedPage aggPage = (AggregatedPage) this.itemRepository.search(searchQueryBuilder.build());

         //3.解析结果：
        StringTerms agg = (StringTerms) aggPage.getAggregation("brand_populate");

        List<StringTerms.Bucket> buckets = agg.getBuckets();

        //3.1遍历桶
        buckets.forEach(bucket -> {
            System.out.println(bucket.getKeyAsString() + ":" + bucket.getDocCount());

            //3.2获取子聚合的结果
            InternalAvg price_avg = (InternalAvg) bucket.getAggregations().asMap().get("price_avg");

            System.out.println("平均售价:" + price_avg.getValue());
        });


    }


}
