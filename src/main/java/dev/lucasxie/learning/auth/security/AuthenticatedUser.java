package dev.lucasxie.learning.auth.security;

import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import dev.lucasxie.learning.user.UserStatus;

public class AuthenticatedUser implements UserDetails {

	private final Long id;

	private final String email;

	private final String username;

	private final String password;

	private final UserStatus status;

	private final Collection<? extends GrantedAuthority> authorities;

	public AuthenticatedUser(
		Long id,
		String email,
		String username,
		String password,
		UserStatus status,
		Collection<? extends GrantedAuthority> authorities
	) {
		this.id = id;
		this.email = email;
		this.username = username;
		this.password = password;
		this.status = status;
		this.authorities = authorities;
	}

	public Long getId() {
		return id;
	}

	public String getEmail() {
		return email;
	}

	@Override
	public String getUsername() {
		return username;
	}

	@Override
	public String getPassword() {
		return password;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return authorities;
	}

	@Override
	public boolean isAccountNonLocked() {
		return status != UserStatus.LOCKED;
	}

	@Override
	public boolean isEnabled() {
		return status == UserStatus.ACTIVE;
	}
}
