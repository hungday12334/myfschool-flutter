package com.example.myfschool.repository;

import com.example.myfschool.entity.TuitionFee;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TuitionFeeRepository extends JpaRepository<TuitionFee, Long> {

    boolean existsByStudentId(Long studentId);

    List<TuitionFee> findByStudentIdOrderByDueDateDescIdDesc(Long studentId);
}
