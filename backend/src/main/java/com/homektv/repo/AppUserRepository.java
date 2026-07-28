package com.homektv.repo;

import com.homektv.domain.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByClientToken(String clientToken);

    @Modifying
    @Query(value = "INSERT INTO users (client_token, nickname) VALUES (:clientToken, :nickname) " +
            "ON CONFLICT (client_token) DO NOTHING", nativeQuery = true)
    int insertIfAbsent(String clientToken, String nickname);

    @Query(value = "SELECT pg_advisory_xact_lock(hashtext('home-ktv-room-host'))", nativeQuery = true)
    void lockRoomHost();
}
