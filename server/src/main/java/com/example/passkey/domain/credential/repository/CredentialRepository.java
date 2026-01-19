package com.example.passkey.domain.credential.repository;

import com.example.passkey.domain.credential.entity.Credential;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CredentialRepository extends JpaRepository<Credential, Long> {
    Optional<Credential> findByCredentialId(String credentialId);
    boolean existsByCredentialId(String credentialId);
}
