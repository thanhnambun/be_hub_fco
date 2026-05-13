package com.ra.base_spring_boot.repository;

import com.ra.base_spring_boot.model.SyncErrorLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SyncErrorLogRepository extends JpaRepository<SyncErrorLog, Long> {
}
