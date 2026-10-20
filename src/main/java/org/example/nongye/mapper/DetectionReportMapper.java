package org.example.nongye.mapper;

import org.apache.ibatis.annotations.*;
import org.example.nongye.entity.DetectionReport;

import java.util.List;

@Mapper
public interface DetectionReportMapper {

    @Select("SELECT * FROM detection_report WHERE batch_id = #{batchId} ORDER BY report_date DESC")
    List<DetectionReport> selectByBatchId(Long batchId);

    /**
     * 某农户所有批次的检测报告
     */
    @Select("SELECT r.*, b.batch_no AS batch_no, b.crop_name AS crop_name " +
            "FROM detection_report r " +
            "LEFT JOIN crop_batch b ON r.batch_id = b.id " +
            "WHERE b.farmer_id = #{farmerId} ORDER BY r.report_date DESC")
    List<DetectionReport> selectByFarmer(Long farmerId);

    /**
     * 全平台检测报告（管理员）
     */
    @Select("SELECT r.*, b.batch_no AS batch_no, b.crop_name AS crop_name, u.real_name AS farmer_name " +
            "FROM detection_report r " +
            "LEFT JOIN crop_batch b ON r.batch_id = b.id " +
            "LEFT JOIN sys_user u ON b.farmer_id = u.id " +
            "ORDER BY r.report_date DESC")
    List<DetectionReport> selectAllWithBatch();

    @Insert("INSERT INTO detection_report (batch_id, report_no, institution, report_url, report_date, result) " +
            "VALUES (#{batchId}, #{reportNo}, #{institution}, #{reportUrl}, #{reportDate}, #{result})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(DetectionReport report);
}
