package org.example.nongye.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TraceRecord {
    private Long id;
    private Long batchId;
    private Long fromUser;
    private Long toUser;
    private String type;     // HARVEST/INBOUND/OUTBOUND/SALE
    private BigDecimal quantity;
    private String location;
    private LocalDateTime operateTime;
    private String remark;

    // 联表
    private String fromUserName;
    private String toUserName;
    private String typeName;
    private String batchNo;
    private String cropName;
}
