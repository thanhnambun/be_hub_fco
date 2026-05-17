package com.fco.platform.sync.infrastructure.persistence;

import com.fco.platform.sync.domain.SyncErrorLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SyncErrorLogRepository extends JpaRepository<SyncErrorLog, Long> {
}
