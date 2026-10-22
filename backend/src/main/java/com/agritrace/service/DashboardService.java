package com.agritrace.service;

import com.agritrace.common.BatchStatus;
import com.agritrace.dto.Activity;
import com.agritrace.entity.Batch;
import com.agritrace.entity.FarmingRecord;
import com.agritrace.entity.Inspection;
import com.agritrace.entity.Logistics;
import com.agritrace.mapper.BatchMapper;
import com.agritrace.mapper.DashboardMapper;
import com.agritrace.mapper.FarmBaseMapper;
import com.agritrace.mapper.FarmingRecordMapper;
import com.agritrace.mapper.InspectionMapper;
import com.agritrace.mapper.LogisticsMapper;
import com.agritrace.mapper.ProductMapper;
import com.agritrace.mapper.TraceCodeMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {
    private static final Map<String, String> FARMING_TYPES = Map.of("SOW", "播种/定植", "FERTILIZE", "施肥",
            "IRRIGATE", "灌溉", "PEST", "病虫害防治", "WEED", "除草", "HARVEST", "采收", "OTHER", "农事");

    private final DashboardMapper dashboardMapper;
    private final BatchMapper batchMapper;
    private final ProductMapper productMapper;
    private final FarmBaseMapper baseMapper;
    private final FarmingRecordMapper farmingMapper;
    private final InspectionMapper inspectionMapper;
    private final LogisticsMapper logisticsMapper;
    private final TraceCodeMapper traceCodeMapper;

    public Map<String, Object> overview() {
        Map<String, Object> m = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();

        Map<String, Object> cards = new LinkedHashMap<>();
        cards.put("baseCount", baseMapper.selectCount(null));
        cards.put("productCount", productMapper.selectCount(null));
        cards.put("batchCount", batchMapper.selectCount(null));
        cards.put("growingCount", batchMapper.selectCount(new LambdaQueryWrapper<Batch>().eq(Batch::getStatus, BatchStatus.GROWING)));
        cards.put("traceCodeCount", traceCodeMapper.selectCount(null));
        cards.put("totalScans", dashboardMapper.totalScans());
        cards.put("todayScans", dashboardMapper.scansSince(today.atStartOfDay()));
        cards.put("inTransit", logisticsMapper.selectCount(new LambdaQueryWrapper<Logistics>().eq(Logistics::getStatus, LogisticsService.IN_TRANSIT)));
        m.put("cards", cards);

        m.put("batchStatus", dashboardMapper.batchStatusCount());
        m.put("category", dashboardMapper.batchCategoryCount());
        m.put("inspectionResult", dashboardMapper.inspectionResultCount());
        m.put("topProducts", dashboardMapper.topScannedProducts());

        // 近 30 天扫码趋势，没有扫码的日子补 0
        LocalDate start = today.minusDays(29);
        Map<String, Object> byDay = dashboardMapper.scanTrend(start.atStartOfDay()).stream()
                .collect(Collectors.toMap(r -> String.valueOf(r.get("day")), r -> r.get("value")));
        List<Map<String, Object>> trend = new ArrayList<>();
        for (LocalDate d = start; !d.isAfter(today); d = d.plusDays(1)) {
            trend.add(Map.of("day", d.toString(), "value", byDay.getOrDefault(d.toString(), 0)));
        }
        m.put("scanTrend", trend);
        m.put("activities", activities());
        return m;
    }

    /** 最新动态：农事、质检、物流各取最近几条，按时间合并 */
    private List<Activity> activities() {
        List<FarmingRecord> fr = farmingMapper.selectList(new LambdaQueryWrapper<FarmingRecord>()
                .orderByDesc(FarmingRecord::getOpDate).orderByDesc(FarmingRecord::getId).last("LIMIT 6"));
        List<Inspection> ins = inspectionMapper.selectList(new LambdaQueryWrapper<Inspection>()
                .orderByDesc(Inspection::getInspectDate).orderByDesc(Inspection::getId).last("LIMIT 6"));
        List<Logistics> lg = logisticsMapper.selectList(new LambdaQueryWrapper<Logistics>()
                .orderByDesc(Logistics::getDepartTime).last("LIMIT 6"));

        List<Long> ids = new ArrayList<>();
        fr.forEach(r -> ids.add(r.getBatchId()));
        ins.forEach(r -> ids.add(r.getBatchId()));
        lg.forEach(r -> ids.add(r.getBatchId()));
        Map<Long, String> batchNo = ids.isEmpty() ? Map.of() : batchMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(Batch::getId, Batch::getBatchNo, (a, b) -> a));
        Function<Long, String> no = id -> batchNo.getOrDefault(id, "-");

        List<Activity> list = new ArrayList<>();
        fr.forEach(r -> list.add(new Activity("farming", FARMING_TYPES.getOrDefault(r.getType(), "农事")
                + (r.getMaterial() == null ? "" : "：" + r.getMaterial()), no.apply(r.getBatchId()), r.getOpDate().atStartOfDay())));
        ins.forEach(r -> list.add(new Activity("PASS".equals(r.getResult()) ? "pass" : "fail",
                "质检" + ("PASS".equals(r.getResult()) ? "合格" : "不合格") + "（" + r.getReportNo() + "）",
                no.apply(r.getBatchId()), r.getInspectDate().atStartOfDay())));
        lg.forEach(r -> {
            boolean arrived = r.getArriveTime() != null;
            LocalDateTime t = arrived ? r.getArriveTime() : r.getDepartTime();
            list.add(new Activity("logistics", (arrived ? "已送达 " : "发往 ") + r.getToAddr(), no.apply(r.getBatchId()), t));
        });
        list.sort(Comparator.comparing(Activity::time).reversed());
        return list.subList(0, Math.min(7, list.size()));
    }
}
