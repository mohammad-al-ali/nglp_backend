package com.NGLP.backend.v1.repo;

import com.NGLP.backend.v1.entity.UserAiPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserAiPreferenceRepo extends JpaRepository<UserAiPreference, Long> {
    Optional<UserAiPreference> findByUserId(Long userId);
}