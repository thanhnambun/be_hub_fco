package com.fco.platform.market.infrastructure.persistence;

import com.fco.platform.market.domain.GameAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IGameAccountRepository extends JpaRepository<GameAccount, Long> {
    Optional<GameAccount> findByAccountCode(String accountCode);
}
