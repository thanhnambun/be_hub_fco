package com.ra.base_spring_boot.repository;

import com.ra.base_spring_boot.model.DetailSyncErrorLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DetailSyncErrorLogRepository extends JpaRepository<DetailSyncErrorLog, Long> {
}
