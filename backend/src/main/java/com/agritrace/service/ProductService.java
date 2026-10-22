package com.agritrace.service;

import com.agritrace.common.BusinessException;
import com.agritrace.common.PageResult;
import com.agritrace.entity.Batch;
import com.agritrace.entity.Product;
import com.agritrace.mapper.BatchMapper;
import com.agritrace.mapper.ProductMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ProductService extends ServiceImpl<ProductMapper, Product> {
    private final BatchMapper batchMapper;

    public PageResult<Product> page(int page, int size, String keyword, String category) {
        LambdaQueryWrapper<Product> q = new LambdaQueryWrapper<Product>()
                .eq(StringUtils.hasText(category), Product::getCategory, category)
                .and(StringUtils.hasText(keyword), w -> w.like(Product::getName, keyword)
                        .or().like(Product::getVariety, keyword))
                .orderByAsc(Product::getId);
        return PageResult.of(page(new Page<>(page, size), q));
    }

    public Product create(Product p) {
        p.setId(null);
        if (!StringUtils.hasText(p.getUnit())) {
            p.setUnit("kg");
        }
        save(p);
        return p;
    }

    public void update(Long id, Product p) {
        p.setId(id);
        if (!updateById(p)) {
            throw new BusinessException("产品不存在");
        }
    }

    public void delete(Long id) {
        if (batchMapper.exists(new LambdaQueryWrapper<Batch>().eq(Batch::getProductId, id))) {
            throw new BusinessException("该产品已有生产批次，不能删除");
        }
        removeById(id);
    }
}
