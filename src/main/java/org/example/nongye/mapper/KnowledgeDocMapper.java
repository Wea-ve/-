package org.example.nongye.mapper;

import org.apache.ibatis.annotations.*;
import org.example.nongye.entity.KnowledgeDoc;

import java.util.List;

@Mapper
public interface KnowledgeDocMapper {

    @Select("SELECT * FROM knowledge_doc ORDER BY id ASC")
    List<KnowledgeDoc> selectAll();

    @Select("SELECT * FROM knowledge_doc WHERE id = #{id}")
    KnowledgeDoc selectById(Long id);

    /** 回写向量（JSON 数组字符串），可重复执行不报错 */
    @Update("UPDATE knowledge_doc SET embedding = #{embedding} WHERE id = #{id}")
    int updateEmbedding(@Param("id") Long id, @Param("embedding") String embedding);

    @Select("SELECT COUNT(*) FROM knowledge_doc")
    int countAll();
}
