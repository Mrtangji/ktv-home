package com.homektv.repo;

import com.homektv.domain.Wish;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WishRepository extends JpaRepository<Wish, Long> {

    List<Wish> findAllByOrderByCreatedAtDesc();
}
