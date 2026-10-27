package com.mallmini.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mallmini.product.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface ProductMapper extends BaseMapper<Product> {

    @org.apache.ibatis.annotations.Update("""
            UPDATE product
               SET sales_count = sales_count + #{quantity},
                   updated_at = NOW(3)
             WHERE id = #{productId}
            """)
    int incrementSales(@org.apache.ibatis.annotations.Param("productId") Long productId,
                       @org.apache.ibatis.annotations.Param("quantity") int quantity);

    IPage<Product> selectOnSalePage(Page<Product> page,
                                    @Param("categoryIds") List<Long> categoryIds,
                                    @Param("keyword") String keyword,
                                    @Param("minPrice") BigDecimal minPrice,
                                    @Param("maxPrice") BigDecimal maxPrice,
                                    @Param("sort") String sort);
}
