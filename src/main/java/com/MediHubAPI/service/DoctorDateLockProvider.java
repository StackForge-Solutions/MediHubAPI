package com.MediHubAPI.service;

import com.MediHubAPI.exception.HospitalAPIException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

@Component
public class DoctorDateLockProvider {

    private final ConcurrentMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    public <T> T executeWithLock(Long doctorId, LocalDate date, Supplier<T> action) {
        Objects.requireNonNull(doctorId, "doctorId cannot be null");
        Objects.requireNonNull(date, "date cannot be null");
        String key = buildKey(doctorId, date);
        ReentrantLock lock = locks.computeIfAbsent(key, k -> new ReentrantLock());
        boolean acquired;
        try {
            acquired = lock.tryLock(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new HospitalAPIException(HttpStatus.CONFLICT, "CONCURRENT_OPERATION", "Interrupted while waiting for appointment lock", e);
        }
        if (!acquired) {
            throw new HospitalAPIException(HttpStatus.CONFLICT, "CONCURRENT_OPERATION", "Another operation is running for this doctor/date");
        }

        try {
            return action.get();
        } finally {
            lock.unlock();
            if (!lock.hasQueuedThreads()) {
                locks.remove(key, lock);
            }
        }
    }

    private String buildKey(Long doctorId, LocalDate date) {
        return doctorId + "|" + date;
    }
}
