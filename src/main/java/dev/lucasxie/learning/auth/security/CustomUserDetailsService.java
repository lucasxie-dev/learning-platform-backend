package dev.lucasxie.learning.auth.security;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.lucasxie.learning.permission.Permission;
import dev.lucasxie.learning.role.Role;
import dev.lucasxie.learning.user.UserAccount;
import dev.lucasxie.learning.user.UserAccountRepository;

@Service
public class CustomUserDetailsService implements UserDetailsService {

	private final UserAccountRepository userAccountRepository;

	public CustomUserDetailsService(UserAccountRepository userAccountRepository) {
		this.userAccountRepository = userAccountRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public UserDetails loadUserByUsername(String username) {
		return userAccountRepository.findByEmailIgnoreCaseOrUsernameIgnoreCase(username, username)
			.map(this::toAuthenticatedUser)
			.orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
	}

	@Transactional(readOnly = true)
	public AuthenticatedUser loadUserById(Long userId) {
		return userAccountRepository.findWithRolesById(userId)
			.map(this::toAuthenticatedUser)
			.orElseThrow(() -> new UsernameNotFoundException("User not found: " + userId));
	}

	public AuthenticatedUser toAuthenticatedUser(UserAccount userAccount) {
		return new AuthenticatedUser(
			userAccount.getId(),
			userAccount.getEmail(),
			userAccount.getUsername(),
			userAccount.getPasswordHash(),
			userAccount.getStatus(),
			toAuthorities(userAccount.getRoles())
		);
	}

	private Collection<? extends GrantedAuthority> toAuthorities(Collection<Role> roles) {
		Set<GrantedAuthority> authorities = new LinkedHashSet<>();

		for (Role role : roles) {
			authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getCode().name()));

			for (Permission permission : role.getPermissions()) {
				authorities.add(new SimpleGrantedAuthority(permission.getCode()));
			}
		}

		return authorities;
	}
}
