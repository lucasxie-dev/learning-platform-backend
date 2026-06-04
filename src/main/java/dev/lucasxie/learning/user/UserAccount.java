package dev.lucasxie.learning.user;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import dev.lucasxie.learning.common.model.BaseEntity;
import dev.lucasxie.learning.role.Role;
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
	name = "user_account",
	uniqueConstraints = {
		@UniqueConstraint(name = "uk_user_account_email", columnNames = "email"),
		@UniqueConstraint(name = "uk_user_account_username", columnNames = "username")
	},
	indexes = {
		@Index(name = "idx_user_account_status", columnList = "status")
	}
)
public class UserAccount extends BaseEntity {

	@Column(name = "email", nullable = false, length = 255)
	private String email;

	@Column(name = "username", nullable = false, length = 100)
	private String username;

	@Column(name = "password_hash", nullable = false, length = 255)
	private String passwordHash;

	@Column(name = "display_name", length = 100)
	private String displayName;

	@Column(name = "avatar_url", length = 1024)
	private String avatarUrl;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 30)
	private UserStatus status = UserStatus.ACTIVE;

	@Column(name = "last_login_at")
	private Instant lastLoginAt;

	@ManyToMany
	@JoinTable(
		name = "user_roles",
		joinColumns = @JoinColumn(name = "user_id"),
		inverseJoinColumns = @JoinColumn(name = "role_id"),
		uniqueConstraints = @UniqueConstraint(name = "uk_user_roles_user_role", columnNames = {"user_id", "role_id"}),
		indexes = {
			@Index(name = "idx_user_roles_user_id", columnList = "user_id"),
			@Index(name = "idx_user_roles_role_id", columnList = "role_id")
		}
	)
	private Set<Role> roles = new HashSet<>();
}
