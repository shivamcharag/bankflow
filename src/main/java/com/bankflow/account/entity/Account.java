package com.bankflow.account.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity                          // Marks this as a DB table
@Table(name = "accounts")        // Table name in PostgreSQL
@Getter                          // Lombok - generates all getters
@Setter                          // Lombok - generates all setters
@NoArgsConstructor               // Lombok - generates empty constructor
@AllArgsConstructor              // Lombok - generates full constructor
@Builder
@EntityListeners(AuditingEntityListener.class)// Lombok - enables builder pattern
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "account_number", unique = true, nullable = false, length = 20)
    private String accountNumber;

    @Column(name = "account_holder_name", nullable = false, length = 100)
    private String accountHolderName;

    @Column(name = "email", unique = true, nullable = false, length = 100)
    private String email;

    @Column(name = "phone_number", nullable = false, length = 15)
    private String phoneNumber;

    @Column(name = "balance", nullable = false, precision = 15, scale = 2)
    private BigDecimal balance;

    @Enumerated(EnumType.STRING)  // Saves enum as String in DB, not number
    @Column(name = "account_type", nullable = false)
    private AccountType accountType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AccountStatus status;

    @Version                      // Optimistic locking - prevents lost updates!
    @Column(name = "version")
    private Long version;

    @CreationTimestamp            // Auto set when record created
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp              // Auto updated on every save
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ─── Enums defined INSIDE entity (they belong to Account) ────────────────

    public enum AccountType {
        SAVINGS,
        CURRENT,
        FIXED_DEPOSIT
    }

    public enum AccountStatus {
        ACTIVE,
        INACTIVE,
        SUSPENDED,
        CLOSED
    }
}