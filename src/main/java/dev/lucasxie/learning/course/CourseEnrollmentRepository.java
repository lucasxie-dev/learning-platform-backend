package dev.lucasxie.learning.course;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CourseEnrollmentRepository extends JpaRepository<CourseEnrollment, Long> {

	boolean existsByUserIdAndCourseId(Long userId, Long courseId);

	Optional<CourseEnrollment> findByUserIdAndCourseId(Long userId, Long courseId);

	Page<CourseEnrollment> findByUserId(Long userId, Pageable pageable);

	Page<CourseEnrollment> findByCourseId(Long courseId, Pageable pageable);

	Optional<CourseEnrollment> findByCourseIdAndUserId(Long courseId, Long userId);

	long countByCourseId(Long courseId);

	long countByCourseIdIn(Collection<Long> courseIds);

	@Query("select enrollment from CourseEnrollment enrollment where enrollment.courseId in :courseIds order by enrollment.enrolledAt desc")
	List<CourseEnrollment> findRecentByCourseIds(Collection<Long> courseIds, Pageable pageable);

	@Query("select enrollment from CourseEnrollment enrollment where enrollment.userId = :userId order by enrollment.enrolledAt desc")
	List<CourseEnrollment> findRecentByUserId(Long userId, Pageable pageable);

	@Query("select enrollment.courseId from CourseEnrollment enrollment where enrollment.userId = :userId")
	List<Long> findCourseIdsByUserId(Long userId);
}
