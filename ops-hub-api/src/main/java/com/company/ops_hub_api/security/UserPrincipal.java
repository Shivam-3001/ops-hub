package com.company.ops_hub_api.security;

import com.company.ops_hub_api.domain.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
public class UserPrincipal implements UserDetails {

    private final User user;
    private final Set<String> permissions;
    private final Set<String> roles;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Set<GrantedAuthority> authorities = permissions.stream()
                .map(permission -> new SimpleGrantedAuthority("PERMISSION_" + permission))
                .collect(Collectors.toSet());
        
        roles.forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role)));
        
        return authorities;
    }

    @Override
    public String getPassword() {
        return user.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return user.getEmployeeId();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return user.getActive();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user.getActive();
    }

    public Long getUserId() {
        return user.getId();
    }

    public String getEmployeeId() {
        return user.getEmployeeId();
    }

    public boolean hasPermission(String permission) {
        return permissions.contains(permission);
    }

    public boolean hasAnyPermission(String... permissionCodes) {
        for (String permission : permissionCodes) {
            if (permissions.contains(permission)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasRole(String roleCode) {
        return roles.contains(roleCode);
    }
}
