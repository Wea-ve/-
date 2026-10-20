package org.example.nongye.entity;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class FarmingLog {
    private Long id;
    private Long batchId;
    private String logType;   // SOWING/FERTILIZE/PESTICIDE/IRRIGATE/WEED
    private LocalDate logDate;
    private String material;
    private String dosage;
    private String content;
    private String photoPath;
    private LocalDateTime createTime;

    // 字典翻译
    private String logTypeName;

    // 联表
    private String batchNo;
    private String cropName;
    private String farmerName;
}
