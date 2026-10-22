package com.agritrace.dto;

import com.agritrace.entity.Batch;
import com.agritrace.entity.FarmBase;
import com.agritrace.entity.FarmingRecord;
import com.agritrace.entity.Inspection;
import com.agritrace.entity.Logistics;
import com.agritrace.entity.Product;
import com.agritrace.entity.TraceCode;

import java.util.List;

/** 批次全链路：后台详情页和消费者溯源页共用 */
public record BatchDetail(Batch batch, Product product, FarmBase base,
                          List<FarmingRecord> farmingRecords, List<Inspection> inspections,
                          List<Logistics> logistics, TraceCode traceCode) {
}
