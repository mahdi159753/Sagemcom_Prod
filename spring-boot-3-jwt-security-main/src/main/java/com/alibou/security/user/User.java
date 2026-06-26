package com.alibou.security.user;

import com.alibou.security.token.Token;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "_user",
        uniqueConstraints = @UniqueConstraint(columnNames = "email")   // FIX: enforce unique email
)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class User implements UserDetails {

  @Id
  @GeneratedValue
  private Integer id;

  private String firstname;
  private String lastname;

  @Column(unique = true, nullable = false)   // FIX: unique + not null at column level
  private String email;

  private String password;

  private String poste;
  private String matricule;

  @Enumerated(EnumType.STRING)
  private Role role;

  private boolean actif = true;

  private java.time.LocalDateTime lastActive;

  @Builder.Default
  @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<Token> tokens = new ArrayList<>();

  @Override public Collection<? extends GrantedAuthority> getAuthorities() { return role.getAuthorities(); }
  @Override public String getPassword()              { return password; }
  @Override public String getUsername()              { return email; }
  @Override public boolean isAccountNonExpired()     { return true; }
  @Override public boolean isAccountNonLocked()      { return true; }
  @Override public boolean isCredentialsNonExpired() { return true; }
  @Override public boolean isEnabled()               { return actif; }
}
