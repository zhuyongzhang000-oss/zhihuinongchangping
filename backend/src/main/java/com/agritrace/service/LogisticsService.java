package com.agritrace.service;

import com.agritrace.common.BatchStatus;
import com.agritrace.common.BusinessException;
import com.agritrace.common.NoGenerator;
import com.agritrace.common.PageResult;
import com.agritrace.entity.Batch;
import com.agritrace.entity.Logistics;
import com.agritrace.mapper.LogisticsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class LogisticsService extends ServiceImpl<LogisticsMapper, Logistics> {
    public static final String IN_TRANSIT = "IN_TRANSIT";
    public static final String ARRIVED = "ARRIVED";

    private final BatchService batchService;

    public PageResult<Logistics> page(int page, int size, Long batchId, String status, String keyword) {
        LambdaQueryWrapper<Logistics> q = new LambdaQueryWrapper<Logistics>()
                .eq(batchId != null, Logistics::getBatchId, batchId)
                .eq(StringUtils.hasText(status), Logistics::getStatus, status)
                .and(StringUtils.hasText(keyword), w -> w.like(Logistics::getWaybillNo, keyword)
                        .or().like(Logistics::getCarrier, keyword).or().like(Logistics::getVehicleNo, keyword)
                        .or().like(Logistics::getToAddr, keyword))
                .orderByDesc(Logistics::getDepartTime).orderByDesc(Logistics::getId);
        Page<Logistics> p = page(new Page<>(page, size), q);
        batchService.fillBatchInfo(p.getRecords(), Logistics::getBatchId, (r, b) -> {
            r.setBatchNo(b.getBatchNo());
            r.setProductName(b.getProductName());
        });
        return PageResult.of(p);
    }

    @Transactional
    public Logistics create(Logistics r, Long userId) {
        Batch b = batchService.mustGet(r.getBatchId());
        if (!BatchStatus.PASSED.contains(b.getStatus())) {
            throw new BusinessException("批次 " + b.getBatchNo() + " 还没有质检合格，不能发运");
        }
        checkTime(r);
        r.setId(null);
        r.setCreatedBy(userId);
        r.setStatus(r.getArriveTime() == null ? IN_TRANSIT : ARRIVED);
        if (!StringUtils.hasText(r.getWaybillNo())) {
            r.setWaybillNo(NoGenerator.next("WL", "", r.getDepartTime().toLocalDate(), 2,
                    no -> exists(new LambdaQueryWrapper<Logistics>().eq(Logistics::getWaybillNo, no))));
        }
        save(r);
        refreshBatchStatus(b.getId());
        return r;
    }

    @Transactional
    public void update(Long id, Logistics r) {
        Logistics old = mustGet(id);
        checkTime(r);
        r.setId(id);
        r.setBatchId(old.getBatchId());
        r.setStatus(r.getArriveTime() == null ? IN_TRANSIT : ARRIVED);
        if (!StringUtils.hasText(r.getWaybillNo())) {
            r.setWaybillNo(null);
        }
        updateById(r);
        if (r.getArriveTime() == null && old.getArriveTime() != null) {
            // updateById 会跳过 null 字段，撤销到达要单独清空
            update(new LambdaUpdateWrapper<Logistics>()
                    .eq(Logistics::getId, id).set(Logistics::getArriveTime, null));
        }
        refreshBatchStatus(old.getBatchId());
    }

    /** 确认到达 */
    @Transactional
    public void arrive(Long id, LocalDateTime time) {
        Logistics old = mustGet(id);
        if (ARRIVED.equals(old.getStatus())) {
            throw new BusinessException("该运单已确认到达");
        }
        LocalDateTime at = time == null ? LocalDateTime.now() : time;
        if (at.isBefore(old.getDepartTime())) {
            throw new BusinessException("到达时间不能早于发车时间");
        }
        Logistics upd = new Logistics();
        upd.setId(id);
        upd.setArriveTime(at);
        upd.setStatus(ARRIVED);
        updateById(upd);
        refreshBatchStatus(old.getBatchId());
    }

    @Transactional
    public void delete(Long id) {
        Logistics old = mustGet(id);
        removeById(id);
        refreshBatchStatus(old.getBatchId());
    }

    /** 有在途运单→运输中；全部到达→已上市；没有运单→回到质检合格 */
    private void refreshBatchStatus(Long batchId) {
        Batch b = batchService.mustGet(batchId);
        if (!BatchStatus.PASSED.contains(b.getStatus())) {
            return;
        }
        boolean inTransit = exists(new LambdaQueryWrapper<Logistics>()
                .eq(Logistics::getBatchId, batchId).eq(Logistics::getStatus, IN_TRANSIT));
        boolean any = exists(new LambdaQueryWrapper<Logistics>().eq(Logistics::getBatchId, batchId));
        batchService.setStatus(batchId, inTransit ? BatchStatus.SHIPPING : any ? BatchStatus.ON_SALE : BatchStatus.INSPECTED);
    }

    private void checkTime(Logistics r) {
        if (r.getArriveTime() != null && r.getArriveTime().isBefore(r.getDepartTime())) {
            throw new BusinessException("到达时间不能早于发车时间");
        }
    }

    private Logistics mustGet(Long id) {
        Logistics r = getById(id);
        if (r == null) {
            throw new BusinessException("物流记录不存在");
        }
        return r;
    }
}
