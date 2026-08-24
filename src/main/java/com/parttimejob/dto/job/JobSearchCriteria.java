package com.parttimejob.dto.job;

import com.parttimejob.enums.PaymentType;
import com.parttimejob.enums.WorkType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobSearchCriteria {
    private String area;
    private String location;
    private WorkType workType;
    
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate jobDate;
    
    private BigDecimal minPayment;
    private BigDecimal maxPayment;
    
    private PaymentType paymentType;
    
    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalTime startTime;
    
    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalTime endTime;
}
