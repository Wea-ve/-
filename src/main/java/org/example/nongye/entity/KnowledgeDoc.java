package org.example.nongye.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 农事知识库文档（AI RAG 检索用）
 * embedding 由应用首次检索时自动向量化回写
 */
@Data
public class KnowledgeDoc {
    private Long id;
    private String title;
    private String category;
    private String content;
    /** 向量 JSON 数组字符串 */
    private String embedding;
    private LocalDateTime createTime;
}
