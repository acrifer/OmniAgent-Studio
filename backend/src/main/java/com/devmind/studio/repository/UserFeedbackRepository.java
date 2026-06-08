package com.devmind.studio.repository;

import com.devmind.studio.entity.UserFeedback;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserFeedbackRepository extends JpaRepository<UserFeedback, Long> {
}
