package dev.lucasxie.learning.course;

import java.time.Instant;

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
	name = "course",
	indexes = {
		@Index(name = "idx_course_owner_id", columnList = "owner_id"),
		@Index(name = "idx_course_status", columnList = "status")
	}
)
public class Course extends BaseEntity {

	@Column(name = "title", nullable = false, length = 200)
	private String title;

	@Column(name = "subtitle", length = 300)
	private String subtitle;

	@Column(name = "description", columnDefinition = "text")
	private String description;

	@Column(name = "cover_file_id")
	private Long coverFileId;

	@Column(name = "owner_id", nullable = false)
	private Long ownerId;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 30)
	private CourseStatus status = CourseStatus.DRAFT;

	@Column(name = "published_at")
	private Instant publishedAt;
}
