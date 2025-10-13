package com.example.demo.repository;

import com.example.demo.model.Email;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmailRepository extends JpaRepository<Email, Long> {
    List<Email> findByUserEmail(String email);
}
