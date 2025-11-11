package com.he187383.barber.repo;

import com.he187383.barber.entity.EmailVerification;
import com.he187383.barber.entity.User;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {

    @Query("""
      SELECT ev FROM EmailVerification ev
       WHERE ev.user = :user AND ev.purpose = 'EMAIL_VERIFY'
       ORDER BY ev.createdAt DESC
      """)
    Optional<EmailVerification> findLatest(@Param("user") User user);

    @Modifying
    @Query("DELETE FROM EmailVerification ev WHERE ev.user = :user")
    void deleteByUser(@Param("user") User user);
}
