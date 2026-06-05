package dev.lucasxie.learning.lesson;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface LessonRepository extends JpaRepository<Lesson, Long>, JpaSpecificationExecutor<Lesson> {

	List<Lesson> findByCourseIdOrderBySortOrderAscIdAsc(Long courseId);

	List<Lesson> findByCourseIdAndStatusOrderBySortOrderAscIdAsc(Long courseId, LessonStatus status);

	List<Lesson> findByCourseIdAndStatusOrderBySortOrderAsc(Long courseId, LessonStatus status);

	List<Lesson> findByCourseIdAndIdIn(Long courseId, Collection<Long> ids);

	Optional<Lesson> findByIdAndCourseId(Long id, Long courseId);

	boolean existsByAudioFileIdOrVideoFileIdOrSubtitleFileId(Long audioFileId, Long videoFileId, Long subtitleFileId);

	boolean existsByContentMarkdownContaining(String fileReference);

	List<Lesson> findByContentMarkdownContaining(String fileReference);

	long countByCourseId(Long courseId);

	long countByCourseIdAndStatus(Long courseId, LessonStatus status);

	long countByCourseIdIn(Collection<Long> courseIds);

	long countByCourseIdInAndStatus(Collection<Long> courseIds, LessonStatus status);

	@Query("select lesson from Lesson lesson where lesson.courseId in :courseIds order by lesson.updatedAt desc")
	List<Lesson> findRecentLessonsByCourseIds(Collection<Long> courseIds, Pageable pageable);

	@Query("select max(lesson.sortOrder) from Lesson lesson where lesson.courseId = :courseId")
	Optional<Integer> findMaxSortOrderByCourseId(Long courseId);
}
