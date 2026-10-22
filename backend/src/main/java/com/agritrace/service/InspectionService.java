package com.agritrace.service;

import com.agritrace.common.BatchStatus;
import com.agritrace.common.BusinessException;
import com.agritrace.common.NoGenerator;
import com.agritrace.common.PageResult;
import com.agritrace.entity.Batch;
import com.agritrace.entity.Inspection;
import com.agritrace.mapper.InspectionMapper;
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
public class InspectionService extends ServiceImpl<InspectionMapper, Inspection> {
    /** 已采收的批次才能送检；质检过的批次在发运之前可以复检 */
    private static final Set<String> INSPECTABLE = Set.of(BatchStatus.HARVESTED, BatchStatus.INSPECTED, BatchStatus.REJECTED);

    private final BatchService batchService;

    public PageResult<Inspection> page(int page, int size, Long batchId, String result, String keyword) {
        LambdaQueryWrapper<Inspection> q = new LambdaQueryWrapper<Inspection>()
                .eq(batchId != null, Inspection::getBatchId, batchId)
                .eq(StringUtils.hasText(result), Inspection::getResult, result)
                .and(StringUtils.hasText(keyword), w -> w.like(Inspection::getReportNo, keyword)
                        .or().like(Inspection::getAgency, keyword))
                .orderByDesc(Inspection::getInspectDate).orderByDesc(Inspection::getId);
        Page<Inspection> p = page(new Page<>(page, size), q);
        batchService.fillBatchInfo(p.getRecords(), Inspection::getBatchId, (r, b) -> {
            r.setBatchNo(b.getBatchNo());
            r.setProductName(b.getProductName());
        });
        return PageResult.of(p);
    }

    @Transactional
    public Inspection create(Inspection r, Long userId) {
        Batch b = inspectableBatch(r.getBatchId());
        checkResult(r.getResult());
        r.setId(null);
        r.setCreatedBy(userId);
        if (!StringUtils.hasText(r.getReportNo())) {
            r.setReportNo(NoGenerator.next("JC", "", r.getInspectDate(), 2,
                    no -> exists(new LambdaQueryWrapper<Inspection>().eq(Inspection::getReportNo, no))));
        }
        if (b.getHarvestDate() != null && r.getInspectDate().isBefore(b.getHarvestDate())) {
            throw new BusinessException("检测日期不能早于采收日期");
        }
        save(r);
        refreshBatchStatus(b.getId());
        return r;
    }

    @Transactional
    public void update(Long id, Inspection r) {
        Inspection old = mustGet(id);
        inspectableBatch(old.getBatchId());
        checkResult(r.getResult());
        r.setId(id);
        r.setBatchId(old.getBatchId());   // 不允许把报告挪到别的批次
        if (!StringUtils.hasText(r.getReportNo())) {
            r.setReportNo(null);
        }
        updateById(r);
        refreshBatchStatus(old.getBatchId());
    }

    @Transactional
    public void delete(Long id) {
        Inspection old = mustGet(id);
        inspectableBatch(old.getBatchId());
        removeById(id);
        refreshBatchStatus(old.getBatchId());
    }

    /** 以最近一次检测结论为准：合格→INSPECTED，不合格→REJECTED，没有检测→HARVESTED */
    private void refreshBatchStatus(Long batchId) {
        Inspection latest = getOne(new LambdaQueryWrapper<Inspection>().eq(Inspection::getBatchId, batchId)
                .orderByDesc(Inspection::getInspectDate).orderByDesc(Inspection::getId).last("LIMIT 1"));
        String status = latest == null ? BatchStatus.HARVESTED
                : "PASS".equals(latest.getResult()) ? BatchStatus.INSPECTED : BatchStatus.REJECTED;
        batchService.setStatus(batchId, status);
    }

    private Batch inspectableBatch(Long batchId) {
        Batch b = batchService.mustGet(batchId);
        if (BatchStatus.GROWING.equals(b.getStatus())) {
            throw new BusinessException("批次 " + b.getBatchNo() + " 还未采收，不能送检");
        }
        if (!INSPECTABLE.contains(b.getStatus())) {
            throw new BusinessException("批次 " + b.getBatchNo() + " 已发运，质检记录已锁定");
        }
        return b;
    }

    private Inspection mustGet(Long id) {
        Inspection r = getById(id);
        if (r == null) {
            throw new BusinessException("质检记录不存在");
        }
        return r;
    }

    private void checkResult(String result) {
        if (!"PASS".equals(result) && !"FAIL".equals(result)) {
            throw new BusinessException("检测结论只能是合格或不合格");
        }
    }
}
