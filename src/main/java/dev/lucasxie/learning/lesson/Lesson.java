package dev.lucasxie.learning.lesson;

import dev.lucasxie.learning.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
	name = "lesson",
	indexes = {
		@Index(name = "idx_lesson_course_id", columnList = "course_id"),
		@Index(name = "idx_lesson_status", columnList = "status")
	}
)
public class Lesson extends BaseEntity {

	@Column(name = "course_id", nullable = false)
	private Long courseId;

	@Column(name = "title", nullable = false, length = 200)
	private String title;

	@Column(name = "description", columnDefinition = "text")
	private String description;

	@Column(name = "content_markdown", columnDefinition = "text")
	private String contentMarkdown;

	@Column(name = "sort_order", nullable = false)
	private Integer sortOrder;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 30)
	private LessonStatus status = LessonStatus.DRAFT;

	@Column(name = "audio_file_id")
	private Long audioFileId;

	@Column(name = "video_file_id")
	private Long videoFileId;

	@Column(name = "subtitle_file_id")
	private Long subtitleFileId;

	@Column(name = "duration_seconds")
	private Integer durationSeconds;
}
