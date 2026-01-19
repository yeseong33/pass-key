package com.example.passkey.domain.user.entity;

import com.example.passkey.domain.credential.entity.Credential;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String displayName;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Credential> credentials = new ArrayList<>();

    public User(String username, String displayName) {
        this.username = username;
        this.displayName = displayName;
    }

    public void addCredential(Credential credential) {
        credentials.add(credential);
        credential.setUser(this);
    }
}
