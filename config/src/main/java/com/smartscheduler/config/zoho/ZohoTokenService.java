package com.smartscheduler.config.zoho;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Service
public class ZohoTokenService {

    private final RedisTemplate<String, ZohoToken> redisTemplate;
    private final RestClient zohoRestClient;
    private final RedissonClient redissonClient; // Injected for distributed locking

    private final String zohoTokenKey = "zoho:access_token";
    private final String zohoLockKey = "lock:zoho:token:refresh"; // New key for the lock
    private static final int PROACTIVE_REFRESH_MINUTES = 5; // Window before expiry

    // Credentials injected from application.yml
    @Value("${zoho.client-id}")
    private String clientId;
    @Value("${zoho.client-secret}")
    private String clientSecret;
    @Value("${zoho.refresh-token}")
    private String refreshToken;

    public ZohoTokenService(
            @Qualifier("zohoTokenRedisTemplate") RedisTemplate<String, ZohoToken> redisTemplate,
            RestClient zohoRestClient,
            RedissonClient redissonClient) { // Inject RedissonClient
        this.redisTemplate = redisTemplate;
        this.zohoRestClient = zohoRestClient;
        this.redissonClient = redissonClient;
    }

    // --- 1. Scheduled Check for Proactive Refresh ---
    @Scheduled(fixedRate = 60000) // Runs every 60 seconds (1 minute)
    public void scheduledTokenRefreshCheck() {
        System.out.println("Running scheduled Zoho token refresh check...");

        ZohoToken currentToken = redisTemplate.opsForValue().get(zohoTokenKey);

        // Check if token is null or expires within the proactive window
        if (currentToken == null || currentToken.getExpiryTime().isBefore(Instant.now().plus(PROACTIVE_REFRESH_MINUTES, TimeUnit.MINUTES.toChronoUnit()))) {
            System.out.println("Token is missing or nearing expiry. Forcing refresh.");
            forceRefreshAndStore();
        } else {
            System.out.println("Token is valid. Expires at: " + currentToken.getExpiryTime());
        }
    }

    private String forceRefreshAndStore() {
        RLock lock = redissonClient.getLock(zohoLockKey);

        try {
            if (lock.tryLock(3, 10, TimeUnit.SECONDS)) {
                ZohoToken currentToken = null;
                try {
                    currentToken = redisTemplate.opsForValue().get(zohoTokenKey);
                    if (currentToken != null && currentToken.getExpiryTime().isAfter(Instant.now().plus(PROACTIVE_REFRESH_MINUTES, TimeUnit.MINUTES.toChronoUnit()))) {
                        return currentToken.getAccessToken(); // Already refreshed by a peer instance
                    }

                    MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
                    formData.add("client_id", clientId);
                    formData.add("client_secret", clientSecret);
                    formData.add("refresh_token", refreshToken);
                    formData.add("grant_type", "refresh_token");

                    ZohoRefreshResponse response = zohoRestClient.post()
                            .body(formData)
                            .retrieve()
                            .body(ZohoRefreshResponse.class);

                    String newAccessToken = response.getAccess_token();
                    long expiresInSeconds = response.getExpires_in();

                    ZohoToken newToken = new ZohoToken();
                    newToken.setAccessToken(newAccessToken);
                    newToken.setRefreshToken(this.refreshToken);
                    newToken.setExpiryTime(Instant.now().plusSeconds(expiresInSeconds).minus(PROACTIVE_REFRESH_MINUTES, TimeUnit.MINUTES.toChronoUnit()));

                    redisTemplate.opsForValue().set(zohoTokenKey, newToken);

                    System.out.println("Zoho token successfully refreshed and stored by THIS INSTANCE.");
                    return newAccessToken;

                } catch (Exception e) {
                    System.err.println("Zoho Token Refresh Failed! Error: " + e.getMessage());
                    // Fallback: return existing token if it exists (may be expired, but better than nothing)
                    return (currentToken != null) ? currentToken.getAccessToken() : null;
                } finally {
                    lock.unlock();
                }
            } else {
                // Lock acquisition failed (another instance is refreshing). Wait and read updated token.
                System.out.println("Lock held by peer. Re-reading token from Redis.");

                // Allow a small delay for the peer to write the token
                TimeUnit.SECONDS.sleep(1);

                // Read the token again (it should be the new, valid one)
                ZohoToken updatedToken = redisTemplate.opsForValue().get(zohoTokenKey);
                return (updatedToken != null) ? updatedToken.getAccessToken() : null;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }
}