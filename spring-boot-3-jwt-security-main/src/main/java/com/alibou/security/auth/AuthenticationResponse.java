package com.alibou.security.auth;
import com.alibou.security.user.Role;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthenticationResponse {

  @JsonProperty("access_token")
  private String accessToken;

  @JsonProperty("refresh_token")
  private String refreshToken;

  // Extra user info returned to Angular frontend
  private String email;
  private String firstname;
  private String lastname;
  private String matricule;
  private String poste;
  private Role role;        // used by Angular to show/hide menu items
  private Integer id;       // added for chat system
}
