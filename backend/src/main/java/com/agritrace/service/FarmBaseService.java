package com.agritrace.service;

import com.agritrace.common.BusinessException;
import com.agritrace.common.PageResult;
import com.agritrace.entity.Batch;
import com.agritrace.entity.FarmBase;
import com.agritrace.mapper.BatchMapper;
import com.agritrace.mapper.FarmBaseMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class FarmBaseService extends ServiceImpl<FarmBaseMapper, FarmBase> {
    private final BatchMapper batchMapper;

    public PageResult<FarmBase> page(int page, int size, String keyword, Integer status) {
        LambdaQueryWrapper<FarmBase> q = new LambdaQueryWrapper<FarmBase>()
                .eq(status != null, FarmBase::getStatus, status)
                .and(StringUtils.hasText(keyword), w -> w.like(FarmBase::getName, keyword)
                        .or().like(FarmBase::getCode, keyword).or().like(FarmBase::getAddress, keyword))
                .orderByAsc(FarmBase::getId);
        return PageResult.of(page(new Page<>(page, size), q));
    }

    public FarmBase create(FarmBase b) {
        b.setId(null);
        if (b.getStatus() == null) {
            b.setStatus(1);
        }
        save(b);
        return b;
    }

    public void update(Long id, FarmBase b) {
        b.setId(id);
        if (!updateById(b)) {
            throw new BusinessException("基地不存在");
        }
    }

    public void delete(Long id) {
        if (batchMapper.exists(new LambdaQueryWrapper<Batch>().eq(Batch::getBaseId, id))) {
            throw new BusinessException("该基地下还有生产批次，不能删除，可以改为停用");
        }
        removeById(id);
    }
}
