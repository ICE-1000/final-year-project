package com.inventory.dto;

import com.inventory.model.RequestStatus;

import javax.validation.constraints.NotNull;

public class UpdateRequestStatusDTO {
    @NotNull
    private RequestStatus status;

    private String rejectionReason;

    public RequestStatus getStatus() { return status; }
    public void setStatus(RequestStatus status) { this.status = status; }
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
}
