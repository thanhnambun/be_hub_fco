package com.fco.platform.sync.infrastructure.persistence;

import com.fco.platform.sync.domain.DetailSyncErrorLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DetailSyncErrorLogRepository extends JpaRepository<DetailSyncErrorLog, Long> {
}
