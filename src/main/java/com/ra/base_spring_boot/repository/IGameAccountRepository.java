package com.ra.base_spring_boot.repository;

import com.ra.base_spring_boot.model.GameAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IGameAccountRepository extends JpaRepository<GameAccount, Long> {
    Optional<GameAccount> findByAccountCode(String accountCode);
}
