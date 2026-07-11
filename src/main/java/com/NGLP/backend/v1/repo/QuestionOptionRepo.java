package com.NGLP.backend.v1.repo;

import com.NGLP.backend.v1.entity.QuestionOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuestionOptionRepo extends JpaRepository<QuestionOption, Long> {

    // لجلب خيارات سؤال معين
    List<QuestionOption> findByQuestionId(Long questionId);

}
