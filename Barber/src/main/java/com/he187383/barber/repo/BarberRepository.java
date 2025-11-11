package com.he187383.barber.repo;

import com.he187383.barber.entity.Barber;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BarberRepository extends JpaRepository<Barber, Long> {
    List<Barber> findByIsActiveTrue();

    Optional<Barber> findByUserId(Long userId);

}
