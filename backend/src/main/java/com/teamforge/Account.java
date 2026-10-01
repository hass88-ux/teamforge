package com.teamforge;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="accounts")
class Account {
 @Id UUID id;
 @Column(nullable=false, unique=true, length=254) String email;
 @Column(name="password_hash", nullable=false, length=100) String passwordHash;
 @Column(name="account_type", nullable=false, length=10) String accountType;
 @Column(name="created_at", nullable=false) Instant createdAt;
 protected Account() {}
 Account(String email, String hash) { this.id=UUID.randomUUID(); this.email=email; this.passwordHash=hash; this.accountType="REAL"; this.createdAt=Instant.now(); }
}
