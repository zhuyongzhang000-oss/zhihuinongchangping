package com.agritrace.service;

import com.agritrace.common.BatchStatus;
import com.agritrace.common.BusinessException;
import com.agritrace.common.PageResult;
import com.agritrace.entity.Batch;
import com.agritrace.entity.FarmingRecord;
import com.agritrace.mapper.FarmingRecordMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class FarmingRecordService extends ServiceImpl<FarmingRecordMapper, FarmingRecord> {
    public static final Set<String> TYPES = Set.of("SOW", "FERTILIZE", "IRRIGATE", "PEST", "WEED", "HARVEST", "OTHER");
    /** 只有种植中、已采收的批次能增改农事记录；进入质检后记录锁定，保证溯源数据不被改动 */
    private static final Set<String> EDITABLE = Set.of(BatchStatus.GROWING, BatchStatus.HARVESTED);

    private final BatchService batchService;

    public PageResult<FarmingRecord> page(int page, int size, Long batchId, String type) {
        LambdaQueryWrapper<FarmingRecord> q = new LambdaQueryWrapper<FarmingRecord>()
                .eq(batchId != null, FarmingRecord::getBatchId, batchId)
                .eq(StringUtils.hasText(type), FarmingRecord::getType, type)
                .orderByDesc(FarmingRecord::getOpDate).orderByDesc(FarmingRecord::getId);
        Page<FarmingRecord> p = page(new Page<>(page, size), q);
        batchService.fillBatchInfo(p.getRecords(), FarmingRecord::getBatchId, (r, b) -> {
            r.setBatchNo(b.getBatchNo());
            r.setProductName(b.getProductName());
        });
        return PageResult.of(p);
    }

    @Transactional
    public FarmingRecord create(FarmingRecord r, Long userId) {
        Batch batch = editableBatch(r.getBatchId());
        checkType(r.getType());
        r.setId(null);
        r.setCreatedBy(userId);
        save(r);
        afterHarvest(batch, r);
        return r;
    }

    @Transactional
    public void update(Long id, FarmingRecord r) {
        FarmingRecord old = mustGet(id);
        editableBatch(old.getBatchId());
        Batch batch = editableBatch(r.getBatchId());
        checkType(r.getType());
        r.setId(id);
        updateById(r);
        afterHarvest(batch, r);
    }

    public void delete(Long id) {
        FarmingRecord old = mustGet(id);
        editableBatch(old.getBatchId());
        removeById(id);
    }

    /** 记一次「采收」，种植中的批次自动变成已采收，并写入采收日期 */
    private void afterHarvest(Batch batch, FarmingRecord r) {
        if ("HARVEST".equals(r.getType()) && BatchStatus.GROWING.equals(batch.getStatus())) {
            Batch upd = new Batch();
            upd.setId(batch.getId());
            upd.setStatus(BatchStatus.HARVESTED);
            upd.setHarvestDate(r.getOpDate());
            batchService.updateById(upd);
        }
    }

    private Batch editableBatch(Long batchId) {
        Batch b = batchService.mustGet(batchId);
        if (!EDITABLE.contains(b.getStatus())) {
            throw new BusinessException("批次 " + b.getBatchNo() + " 已进入质检/流通环节，农事记录已锁定");
        }
        return b;
    }

    private FarmingRecord mustGet(Long id) {
        FarmingRecord r = getById(id);
        if (r == null) {
            throw new BusinessException("记录不存在");
        }
        return r;
    }

    private void checkType(String type) {
        if (!TYPES.contains(type)) {
            throw new BusinessException("农事类型不正确");
        }
    }
}
