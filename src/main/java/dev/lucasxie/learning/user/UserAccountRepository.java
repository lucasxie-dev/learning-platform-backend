package dev.lucasxie.learning.user;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

	@EntityGraph(attributePaths = {"roles", "roles.permissions"})
	Optional<UserAccount> findWithRolesById(Long id);

	@EntityGraph(attributePaths = {"roles", "roles.permissions"})
	Optional<UserAccount> findByEmailIgnoreCaseOrUsernameIgnoreCase(String email, String username);

	Optional<UserAccount> findByEmailIgnoreCase(String email);

	Optional<UserAccount> findByUsernameIgnoreCase(String username);

	boolean existsByEmailIgnoreCase(String email);

	boolean existsByUsernameIgnoreCase(String username);
}
