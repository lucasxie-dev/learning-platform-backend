package dev.lucasxie.learning.course;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface CourseRepository extends JpaRepository<Course, Long>, JpaSpecificationExecutor<Course> {

	boolean existsByCoverFileId(Long coverFileId);

	long countByOwnerId(Long ownerId);

	long countByStatus(CourseStatus status);

	long countByOwnerIdAndStatus(Long ownerId, CourseStatus status);

	@Query("select course.id from Course course order by course.createdAt desc")
	List<Long> findRecentCourseIds(Pageable pageable);

	@Query("select course.id from Course course where course.ownerId = :ownerId order by course.createdAt desc")
	List<Long> findRecentCourseIdsByOwnerId(Long ownerId, Pageable pageable);

	@Query("select course.id from Course course")
	List<Long> findAllCourseIds();

	@Query("select course.id from Course course where course.ownerId = :ownerId")
	List<Long> findCourseIdsByOwnerId(Long ownerId);

	@Query("select course from Course course where course.id in :courseIds order by course.createdAt desc")
	List<Course> findByIdInOrderByCreatedAtDesc(List<Long> courseIds);

	@Query("select course from Course course where course.status = :status order by course.createdAt desc")
	List<Course> findRecentByStatus(CourseStatus status, Pageable pageable);
}
