package dev.lucasxie.learning.course;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseEnrollmentRepository extends JpaRepository<CourseEnrollment, Long> {

	boolean existsByUserIdAndCourseId(Long userId, Long courseId);

	Optional<CourseEnrollment> findByUserIdAndCourseId(Long userId, Long courseId);

	Page<CourseEnrollment> findByUserId(Long userId, Pageable pageable);

	Page<CourseEnrollment> findByCourseId(Long courseId, Pageable pageable);

	Optional<CourseEnrollment> findByCourseIdAndUserId(Long courseId, Long userId);

	long countByCourseId(Long courseId);
}
