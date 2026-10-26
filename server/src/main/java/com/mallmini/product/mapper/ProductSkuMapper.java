package com.mallmini.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mallmini.product.entity.ProductSku;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ProductSkuMapper extends BaseMapper<ProductSku> {

    @Update("""
            UPDATE product_sku
               SET stock = stock - #{quantity}, updated_at = NOW(3)
             WHERE id = #{skuId}
               AND status = 'ENABLED'
               AND stock >= #{quantity}
            """)
    int deductStock(@Param("skuId") Long skuId, @Param("quantity") int quantity);

    @Update("""
            UPDATE product_sku
               SET stock = stock + #{quantity}, updated_at = NOW(3)
             WHERE id = #{skuId}
            """)
    int restoreStock(@Param("skuId") Long skuId, @Param("quantity") int quantity);
}
