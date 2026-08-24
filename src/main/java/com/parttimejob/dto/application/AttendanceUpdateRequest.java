package com.parttimejob.dto.application;

import com.parttimejob.enums.AttendanceStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceUpdateRequest {

    @NotNull(message = "Attendance status is required")
    private AttendanceStatus attendanceStatus;

    private String workCompletionStatus; // e.g. "COMPLETED", "PARTIAL", "NO_SHOW"
}
