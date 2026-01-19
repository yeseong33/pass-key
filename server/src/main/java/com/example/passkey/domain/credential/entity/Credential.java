package com.example.passkey.domain.credential.entity;

import com.example.passkey.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "credentials")
@Getter
@Setter
@NoArgsConstructor
public class Credential {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String credentialId;  // Base64 encoded

    @Lob
    @Column(nullable = false)
    private byte[] publicKey;  // COSE 형식 공개키

    @Column(nullable = false)
    private long signCount;  // 서명 카운터 (리플레이 공격 방지)

    @Column
    private String aaguid;  // 인증자 식별자

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime lastUsedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public Credential(String credentialId, byte[] publicKey, long signCount, String aaguid) {
        this.credentialId = credentialId;
        this.publicKey = publicKey;
        this.signCount = signCount;
        this.aaguid = aaguid;
        this.createdAt = LocalDateTime.now();
    }

    public void updateSignCount(long newSignCount) {
        this.signCount = newSignCount;
        this.lastUsedAt = LocalDateTime.now();
    }
}
