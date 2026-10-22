package com.agritrace.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** 首页统计用的聚合查询 */
@Mapper
public interface DashboardMapper {

    @Select("SELECT status AS name, COUNT(*) AS value FROM batch GROUP BY status")
    List<Map<String, Object>> batchStatusCount();

    @Select("SELECT p.category AS name, COUNT(*) AS value FROM batch b JOIN product p ON p.id = b.product_id " +
            "GROUP BY p.category ORDER BY value DESC")
    List<Map<String, Object>> batchCategoryCount();

    @Select("SELECT DATE_FORMAT(scanned_at, '%Y-%m-%d') AS day, COUNT(*) AS value FROM trace_scan_log " +
            "WHERE scanned_at >= #{since} GROUP BY DATE_FORMAT(scanned_at, '%Y-%m-%d')")
    List<Map<String, Object>> scanTrend(@Param("since") LocalDateTime since);

    @Select("SELECT p.name AS name, SUM(t.scan_count) AS value FROM trace_code t " +
            "JOIN batch b ON b.id = t.batch_id JOIN product p ON p.id = b.product_id " +
            "GROUP BY p.name ORDER BY value DESC LIMIT 6")
    List<Map<String, Object>> topScannedProducts();

    @Select("SELECT result AS name, COUNT(*) AS value FROM inspection GROUP BY result")
    List<Map<String, Object>> inspectionResultCount();

    @Select("SELECT COALESCE(SUM(scan_count), 0) FROM trace_code")
    Long totalScans();

    @Select("SELECT COUNT(*) FROM trace_scan_log WHERE scanned_at >= #{since}")
    Long scansSince(@Param("since") LocalDateTime since);
}
