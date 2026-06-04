package dev.lucasxie.learning.course;

import java.time.Instant;

import dev.lucasxie.learning.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
	name = "course_enrollment",
	uniqueConstraints = @UniqueConstraint(name = "uk_course_enrollment_user_course", columnNames = {"user_id", "course_id"}),
	indexes = {
		@Index(name = "idx_course_enrollment_user_id", columnList = "user_id"),
		@Index(name = "idx_course_enrollment_course_id", columnList = "course_id")
	}
)
public class CourseEnrollment extends BaseEntity {

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "course_id", nullable = false)
	private Long courseId;

	@Column(name = "enrolled_at", nullable = false)
	private Instant enrolledAt;
}
