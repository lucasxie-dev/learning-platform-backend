package dev.lucasxie.learning.permission;

import dev.lucasxie.learning.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
	name = "permission",
	uniqueConstraints = @UniqueConstraint(name = "uk_permission_code", columnNames = "code")
)
public class Permission extends BaseEntity {

	@Column(name = "code", nullable = false, length = 100)
	private String code;

	@Column(name = "name", nullable = false, length = 100)
	private String name;

	@Column(name = "description", length = 500)
	private String description;
}
