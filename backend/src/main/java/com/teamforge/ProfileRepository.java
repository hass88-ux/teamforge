package com.teamforge;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
interface ProfileRepository extends JpaRepository<StoredProfile, UUID> {
 org.springframework.data.domain.Page<StoredProfile> findByDiscoverableTrueAndAccountIdNot(UUID accountId, org.springframework.data.domain.Pageable page);
}
