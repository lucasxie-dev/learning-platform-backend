package dev.lucasxie.learning.lesson;

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
	name = "lesson_progress",
	uniqueConstraints = @UniqueConstraint(name = "uk_lesson_progress_user_lesson", columnNames = {"user_id", "lesson_id"}),
	indexes = {
		@Index(name = "idx_lesson_progress_user_id", columnList = "user_id"),
		@Index(name = "idx_lesson_progress_course_id", columnList = "course_id"),
		@Index(name = "idx_lesson_progress_lesson_id", columnList = "lesson_id")
	}
)
public class LessonProgress extends BaseEntity {

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "course_id", nullable = false)
	private Long courseId;

	@Column(name = "lesson_id", nullable = false)
	private Long lessonId;

	@Column(name = "progress_seconds", nullable = false)
	private Integer progressSeconds = 0;

	@Column(name = "completed", nullable = false)
	private Boolean completed = false;

	@Column(name = "completed_at")
	private Instant completedAt;
}
