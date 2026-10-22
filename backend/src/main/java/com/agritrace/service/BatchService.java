package com.agritrace.service;

import com.agritrace.common.BatchStatus;
import com.agritrace.common.BusinessException;
import com.agritrace.common.NoGenerator;
import com.agritrace.common.PageResult;
import com.agritrace.dto.BatchDetail;
import com.agritrace.entity.Batch;
import com.agritrace.entity.FarmBase;
import com.agritrace.entity.FarmingRecord;
import com.agritrace.entity.Inspection;
import com.agritrace.entity.Logistics;
import com.agritrace.entity.Product;
import com.agritrace.entity.TraceCode;
import com.agritrace.entity.TraceScanLog;
import com.agritrace.mapper.BatchMapper;
import com.agritrace.mapper.FarmBaseMapper;
import com.agritrace.mapper.FarmingRecordMapper;
import com.agritrace.mapper.InspectionMapper;
import com.agritrace.mapper.LogisticsMapper;
import com.agritrace.mapper.ProductMapper;
import com.agritrace.mapper.TraceCodeMapper;
import com.agritrace.mapper.TraceScanLogMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BatchService extends ServiceImpl<BatchMapper, Batch> {
    private final ProductMapper productMapper;
    private final FarmBaseMapper baseMapper;
    private final FarmingRecordMapper farmingMapper;
    private final InspectionMapper inspectionMapper;
    private final LogisticsMapper logisticsMapper;
    private final TraceCodeMapper traceCodeMapper;
    private final TraceScanLogMapper scanLogMapper;

    public PageResult<Batch> page(int page, int size, String keyword, String status, Long productId, Long baseId) {
        LambdaQueryWrapper<Batch> q = new LambdaQueryWrapper<Batch>()
                .eq(StringUtils.hasText(status), Batch::getStatus, status)
                .eq(productId != null, Batch::getProductId, productId)
                .eq(baseId != null, Batch::getBaseId, baseId)
                .orderByDesc(Batch::getId);
        if (StringUtils.hasText(keyword)) {
            List<Long> pids = productMapper.selectList(new LambdaQueryWrapper<Product>().like(Product::getName, keyword))
                    .stream().map(Product::getId).toList();
            q.and(w -> w.like(Batch::getBatchNo, keyword).or().like(Batch::getRemark, keyword)
                    .or(!pids.isEmpty(), x -> x.in(Batch::getProductId, pids)));
        }
        Page<Batch> p = page(new Page<>(page, size), q);
        fillNames(p.getRecords());
        return PageResult.of(p);
    }

    /** 下拉框用：可按状态过滤，例如质检只列已采收的批次 */
    public List<Batch> options(Collection<String> statuses) {
        List<Batch> list = list(new LambdaQueryWrapper<Batch>()
                .in(statuses != null && !statuses.isEmpty(), Batch::getStatus, statuses)
                .orderByDesc(Batch::getId));
        fillNames(list);
        return list;
    }

    public Batch mustGet(Long id) {
        Batch b = id == null ? null : getById(id);
        if (b == null) {
            throw new BusinessException("批次不存在");
        }
        return b;
    }

    public Batch create(Batch b, Long userId) {
        checkRefs(b);
        b.setId(null);
        b.setCreatedBy(userId);
        b.setStatus(b.getHarvestDate() != null ? BatchStatus.HARVESTED : BatchStatus.GROWING);
        if (!StringUtils.hasText(b.getBatchNo())) {
            b.setBatchNo(NoGenerator.next("B", "-", b.getPlantDate(), 3,
                    no -> exists(new LambdaQueryWrapper<Batch>().eq(Batch::getBatchNo, no))));
        }
        save(b);
        return b;
    }

    /** 普通修改不改状态，状态由农事/质检/物流自动推进 */
    public void update(Long id, Batch b) {
        Batch old = mustGet(id);
        checkRefs(b);
        if (!old.getProductId().equals(b.getProductId()) && BatchStatus.PASSED.contains(old.getStatus())) {
            throw new BusinessException("批次已质检合格，不能再更换产品");
        }
        b.setId(id);
        // 种植中的批次补填了采收日期，视为已采收；其它状态不在这里改
        b.setStatus(BatchStatus.GROWING.equals(old.getStatus()) && b.getHarvestDate() != null ? BatchStatus.HARVESTED : null);
        if (!StringUtils.hasText(b.getBatchNo())) {
            b.setBatchNo(null);
        }
        updateById(b);
    }

    /** 管理员手动纠正状态 */
    public void changeStatus(Long id, String status) {
        mustGet(id);
        if (!BatchStatus.ALL.contains(status)) {
            throw new BusinessException("状态不正确");
        }
        setStatus(id, status);
    }

    void setStatus(Long id, String status) {
        Batch upd = new Batch();
        upd.setId(id);
        upd.setStatus(status);
        updateById(upd);
    }

    /** 删除批次连同它的全部溯源记录 */
    @Transactional
    public void delete(Long id) {
        mustGet(id);
        farmingMapper.delete(new LambdaQueryWrapper<FarmingRecord>().eq(FarmingRecord::getBatchId, id));
        inspectionMapper.delete(new LambdaQueryWrapper<Inspection>().eq(Inspection::getBatchId, id));
        logisticsMapper.delete(new LambdaQueryWrapper<Logistics>().eq(Logistics::getBatchId, id));
        traceCodeMapper.delete(new LambdaQueryWrapper<TraceCode>().eq(TraceCode::getBatchId, id));
        scanLogMapper.delete(new LambdaQueryWrapper<TraceScanLog>().eq(TraceScanLog::getBatchId, id));
        removeById(id);
    }

    public BatchDetail detail(Long id) {
        Batch b = mustGet(id);
        fillNames(List.of(b));
        Product product = productMapper.selectById(b.getProductId());
        FarmBase base = baseMapper.selectById(b.getBaseId());
        List<FarmingRecord> records = farmingMapper.selectList(new LambdaQueryWrapper<FarmingRecord>()
                .eq(FarmingRecord::getBatchId, id).orderByAsc(FarmingRecord::getOpDate).orderByAsc(FarmingRecord::getId));
        List<Inspection> inspections = inspectionMapper.selectList(new LambdaQueryWrapper<Inspection>()
                .eq(Inspection::getBatchId, id).orderByAsc(Inspection::getInspectDate).orderByAsc(Inspection::getId));
        List<Logistics> logistics = logisticsMapper.selectList(new LambdaQueryWrapper<Logistics>()
                .eq(Logistics::getBatchId, id).orderByAsc(Logistics::getDepartTime));
        TraceCode code = traceCodeMapper.selectOne(new LambdaQueryWrapper<TraceCode>().eq(TraceCode::getBatchId, id));
        return new BatchDetail(b, product, base, records, inspections, logistics, code);
    }

    /** 给批次填上产品名、基地名、溯源码 */
    public void fillNames(List<Batch> list) {
        if (list.isEmpty()) {
            return;
        }
        Set<Long> pids = list.stream().map(Batch::getProductId).collect(Collectors.toSet());
        Set<Long> bids = list.stream().map(Batch::getBaseId).collect(Collectors.toSet());
        Set<Long> ids = list.stream().map(Batch::getId).collect(Collectors.toSet());
        Map<Long, Product> products = productMapper.selectBatchIds(pids).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
        Map<Long, String> bases = baseMapper.selectBatchIds(bids).stream()
                .collect(Collectors.toMap(FarmBase::getId, FarmBase::getName));
        Map<Long, String> codes = traceCodeMapper.selectList(new LambdaQueryWrapper<TraceCode>().in(TraceCode::getBatchId, ids))
                .stream().collect(Collectors.toMap(TraceCode::getBatchId, TraceCode::getCode));
        for (Batch b : list) {
            Product p = products.get(b.getProductId());
            if (p != null) {
                b.setProductName(p.getName());
                b.setCategory(p.getCategory());
                b.setUnit(p.getUnit());
            }
            b.setBaseName(bases.get(b.getBaseId()));
            b.setTraceCode(codes.get(b.getId()));
        }
    }

    /**
     * 给农事/质检/物流等记录填上批次号、产品名。
     * getBatchId 取记录的批次 id，setter 把（批次号, 产品名）写回记录。
     */
    public <T> void fillBatchInfo(List<T> rows, Function<T, Long> getBatchId, BiConsumer<T, Batch> setter) {
        if (rows.isEmpty()) {
            return;
        }
        Set<Long> ids = rows.stream().map(getBatchId).collect(Collectors.toSet());
        List<Batch> batches = listByIds(ids);
        fillNames(batches);
        Map<Long, Batch> map = batches.stream().collect(Collectors.toMap(Batch::getId, Function.identity()));
        rows.forEach(r -> {
            Batch b = map.get(getBatchId.apply(r));
            if (b != null) {
                setter.accept(r, b);
            }
        });
    }

    private void checkRefs(Batch b) {
        if (productMapper.selectById(b.getProductId()) == null) {
            throw new BusinessException("产品不存在");
        }
        FarmBase base = baseMapper.selectById(b.getBaseId());
        if (base == null) {
            throw new BusinessException("基地不存在");
        }
        if (b.getPlantDate() != null && b.getHarvestDate() != null && b.getHarvestDate().isBefore(b.getPlantDate())) {
            throw new BusinessException("采收日期不能早于种植日期");
        }
    }
}
