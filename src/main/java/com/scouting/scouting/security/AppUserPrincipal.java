package com.scouting.scouting.security;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.scouting.scouting.user.entity.Role;
import com.scouting.scouting.user.entity.User;
import com.scouting.scouting.user.entity.UserStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

/**
 * Principal de seguridad. Se construye a partir de la base de datos y nunca
 * acepta rol, estado ni identidad desde el request.
 */
public record AppUserPrincipal(
        Long id,
        String email,
        @JsonIgnore String passwordHash,
        Role role,
        UserStatus status,
        boolean mustChangePassword,
        Instant temporaryPasswordExpiresAt
) implements UserDetails {

    public static AppUserPrincipal from(User user) {
        return new AppUserPrincipal(
                user.getId(),
                user.getEmail(),
                user.getPasswordHash(),
                user.getRole(),
                user.getStatus(),
                user.isMustChangePassword(),
                user.getTemporaryPasswordExpiresAt()
        );
    }

    public AppUserPrincipal withPasswordChanged() {
        return new AppUserPrincipal(id, email, passwordHash, role, status, false, null);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    @JsonIgnore
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return !isTemporaryPasswordExpired();
    }

    @Override
    public boolean isEnabled() {
        return status == UserStatus.ACTIVE && !isTemporaryPasswordExpired();
    }

    private boolean isTemporaryPasswordExpired() {
        return temporaryPasswordExpiresAt != null && !temporaryPasswordExpiresAt.isAfter(Instant.now());
    }
}
