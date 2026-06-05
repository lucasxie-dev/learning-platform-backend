package dev.lucasxie.learning.lesson;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface LessonProgressRepository extends JpaRepository<LessonProgress, Long> {

	Optional<LessonProgress> findByUserIdAndLessonId(Long userId, Long lessonId);

	List<LessonProgress> findByUserIdAndCourseId(Long userId, Long courseId);

	long countByUserIdAndCourseIdAndCompletedTrue(Long userId, Long courseId);

	Optional<LessonProgress> findTopByUserIdAndCourseIdOrderByUpdatedAtDesc(Long userId, Long courseId);

	List<LessonProgress> findByCourseIdAndUserId(Long courseId, Long userId);

	long countByCourseIdInAndCompletedTrue(Collection<Long> courseIds);

	long countByUserIdAndCourseIdInAndCompletedTrue(Long userId, Collection<Long> courseIds);

	@Query("select progress from LessonProgress progress where progress.courseId in :courseIds order by progress.updatedAt desc")
	List<LessonProgress> findRecentByCourseIds(Collection<Long> courseIds, Pageable pageable);

	@Query("select progress from LessonProgress progress where progress.userId = :userId order by progress.updatedAt desc")
	List<LessonProgress> findRecentByUserId(Long userId, Pageable pageable);

	@Query("""
		select count(progress)
		from LessonProgress progress
		join Lesson lesson on lesson.id = progress.lessonId
		where progress.userId = :userId
		  and progress.courseId = :courseId
		  and progress.completed = true
		  and lesson.status = :lessonStatus
		""")
	long countCompletedLessonsByUserIdAndCourseIdAndLessonStatus(
		Long userId,
		Long courseId,
		LessonStatus lessonStatus
	);
}
