package org.example.nongye.mapper;

import org.apache.ibatis.annotations.*;
import org.example.nongye.entity.FarmingLog;

import java.util.List;

@Mapper
public interface FarmingLogMapper {

    @Select("SELECT * FROM farming_log WHERE batch_id = #{batchId} ORDER BY log_date ASC, id ASC")
    List<FarmingLog> selectByBatchId(Long batchId);

    /**
     * 某农户全部批次的农事记录
     */
    @Select("SELECT l.*, b.batch_no AS batch_no, b.crop_name AS crop_name " +
            "FROM farming_log l " +
            "LEFT JOIN crop_batch b ON l.batch_id = b.id " +
            "WHERE b.farmer_id = #{farmerId} ORDER BY l.log_date DESC, l.id DESC")
    List<FarmingLog> selectByFarmer(Long farmerId);

    /**
     * 全平台农事记录（管理员监管）
     */
    @Select("SELECT l.*, b.batch_no AS batch_no, b.crop_name AS crop_name, u.real_name AS farmer_name " +
            "FROM farming_log l " +
            "LEFT JOIN crop_batch b ON l.batch_id = b.id " +
            "LEFT JOIN sys_user u ON b.farmer_id = u.id " +
            "ORDER BY l.log_date DESC, l.id DESC")
    List<FarmingLog> selectAllWithBatch();

    @Insert("INSERT INTO farming_log (batch_id, log_type, log_date, material, dosage, content, photo_path) " +
            "VALUES (#{batchId}, #{logType}, #{logDate}, #{material}, #{dosage}, #{content}, #{photoPath})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(FarmingLog log);
}
