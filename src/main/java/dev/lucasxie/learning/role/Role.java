package dev.lucasxie.learning.role;

import java.util.HashSet;
import java.util.Set;

import dev.lucasxie.learning.common.model.BaseEntity;
import dev.lucasxie.learning.permission.Permission;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
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
	name = "role",
	uniqueConstraints = @UniqueConstraint(name = "uk_role_code", columnNames = "code")
)
public class Role extends BaseEntity {

	@Enumerated(EnumType.STRING)
	@Column(name = "code", nullable = false, length = 50)
	private RoleCode code;

	@Column(name = "name", nullable = false, length = 100)
	private String name;

	@Column(name = "description", length = 500)
	private String description;

	@ManyToMany
	@JoinTable(
		name = "role_permissions",
		joinColumns = @JoinColumn(name = "role_id"),
		inverseJoinColumns = @JoinColumn(name = "permission_id"),
		uniqueConstraints = @UniqueConstraint(name = "uk_role_permissions_role_permission", columnNames = {"role_id", "permission_id"}),
		indexes = {
			@Index(name = "idx_role_permissions_role_id", columnList = "role_id"),
			@Index(name = "idx_role_permissions_permission_id", columnList = "permission_id")
		}
	)
	private Set<Permission> permissions = new HashSet<>();
}
