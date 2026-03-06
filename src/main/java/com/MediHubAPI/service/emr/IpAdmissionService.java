package com.MediHubAPI.service.emr;

import com.MediHubAPI.dto.emr.IpAdmissionFetchResponse;
import com.MediHubAPI.dto.emr.IpAdmissionSaveRequest;
import com.MediHubAPI.dto.emr.IpAdmissionSaveResponse;

public interface IpAdmissionService {
    IpAdmissionFetchResponse getIpAdmission(Long appointmentId);
    IpAdmissionSaveResponse saveIpAdmission(Long appointmentId, IpAdmissionSaveRequest request);
}
