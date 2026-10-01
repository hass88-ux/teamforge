package com.teamforge;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
interface ProfileRepository extends JpaRepository<StoredProfile, UUID> {}
