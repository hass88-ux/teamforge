package com.teamforge;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="profiles")
class StoredProfile {
 @Id @Column(name="account_id") UUID accountId;
 @Column(name="profile_json", nullable=false, columnDefinition="text") String profileJson;
 @Column(name="updated_at", nullable=false) Instant updatedAt;
 @Column(name="discoverable", nullable=false) boolean discoverable;
 @Version @Column(name="row_version", nullable=false) long version;
 protected StoredProfile() {}
 StoredProfile(UUID accountId) { this.accountId=accountId; }
}
