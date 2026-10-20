package org.example.nongye.entity;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class DetectionReport {
    private Long id;
    private Long batchId;
    private String reportNo;
    private String institution;
    private String reportUrl;
    private LocalDate reportDate;
    private String result;
    private LocalDateTime createTime;

    // 联表
    private String batchNo;
    private String cropName;
    private String farmerName;
}
