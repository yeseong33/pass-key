package com.example.passkey.domain.auth.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Challenge를 임시 저장하는 서비스
 * 실제 운영환경에서는 Redis 등을 사용해야 함
 */
@Service
public class ChallengeService {

    // username -> challenge (Base64 encoded)
    private final Map<String, byte[]> challengeStore = new ConcurrentHashMap<>();

    public void storeChallenge(String key, byte[] challenge) {
        challengeStore.put(key, challenge);
    }

    public byte[] getChallenge(String key) {
        return challengeStore.get(key);
    }

    public byte[] getAndRemoveChallenge(String key) {
        return challengeStore.remove(key);
    }

    public boolean hasChallenge(String key) {
        return challengeStore.containsKey(key);
    }
}
