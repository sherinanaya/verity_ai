package com.verityai.assessment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssessmentRepository extends JpaRepository<Assessment, Long> {
    List<Assessment> findAllByOrderByCreatedAtDesc();
}
