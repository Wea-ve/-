package org.example.nongye.mapper;

import org.apache.ibatis.annotations.*;
import org.example.nongye.entity.Complaint;

import java.util.List;

@Mapper
public interface ComplaintMapper {

    @Select("SELECT c.*, b.crop_name, b.batch_no FROM complaint c " +
            "LEFT JOIN crop_batch b ON c.batch_id = b.id ORDER BY c.id DESC")
    List<Complaint> selectAll();

    @Select("SELECT * FROM complaint WHERE id = #{id}")
    Complaint selectById(Long id);

    @Insert("INSERT INTO complaint (batch_id, complainant, phone, type, content, images) " +
            "VALUES (#{batchId}, #{complainant}, #{phone}, #{type}, #{content}, #{images})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Complaint complaint);

    @Update("UPDATE complaint SET status=#{status}, reply=#{reply}, reply_time=NOW() WHERE id=#{id}")
    int reply(Complaint complaint);

    /**
     * 按投诉人/手机号/作物名模糊搜索
     */
    @Select("<script>SELECT c.*, b.crop_name, b.batch_no FROM complaint c " +
            "LEFT JOIN crop_batch b ON c.batch_id = b.id " +
            "<where> " +
            "<if test='keyword != null and keyword != \"\"'>" +
            "(c.complainant LIKE CONCAT('%', #{keyword}, '%') OR c.phone LIKE CONCAT('%', #{keyword}, '%') OR b.crop_name LIKE CONCAT('%', #{keyword}, '%'))" +
            "</if> " +
            "</where> ORDER BY c.id DESC</script>")
    List<Complaint> search(@Param("keyword") String keyword);

    /**
     * 投诉状态分布统计（饼图）
     */
    @Select("SELECT status, COUNT(*) AS cnt FROM complaint GROUP BY status")
    List<java.util.Map<String, Object>> countByStatus();

    /**
     * 待处理投诉数量（顶部铃铛角标）
     */
    @Select("SELECT COUNT(*) FROM complaint WHERE status = 'PENDING'")
    int countPending();

    /**
     * 消费者确认回复：提交后直接关闭工单
     */
    @Update("UPDATE complaint SET status='CLOSED', consumer_reply=#{consumerReply}, consumer_reply_time=NOW() WHERE id=#{id}")
    int consumerReply(@Param("id") Long id, @Param("consumerReply") String consumerReply);

    /**
     * 消费者查询自己的投诉：按手机号或查询码（投诉ID）匹配，联表作物信息
     */
    @Select("SELECT c.*, b.crop_name, b.batch_no FROM complaint c " +
            "LEFT JOIN crop_batch b ON c.batch_id = b.id " +
            "WHERE c.phone = #{keyword} OR CAST(c.id AS CHAR) = #{keyword} " +
            "ORDER BY c.id DESC")
    List<Complaint> selectByPhoneOrCode(@Param("keyword") String keyword);
}
