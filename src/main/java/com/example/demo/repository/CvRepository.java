package com.example.demo.repository;

import com.example.demo.model.Cv;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CvRepository extends JpaRepository<Cv, Long> {
    List<Cv> findByUserEmail(String userEmail);
}
