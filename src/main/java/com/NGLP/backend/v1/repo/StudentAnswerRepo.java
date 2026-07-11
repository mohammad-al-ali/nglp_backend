package com.NGLP.backend.v1.repo;

import com.NGLP.backend.v1.entity.StudentAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudentAnswerRepo extends JpaRepository<StudentAnswer, Long> {

    // لجلب إجابات مستخدم معين
    List<StudentAnswer> findByUserId(Long userId);

    // لجلب إجابات سؤال معين
    List<StudentAnswer> findByQuestionId(Long questionId);

}
