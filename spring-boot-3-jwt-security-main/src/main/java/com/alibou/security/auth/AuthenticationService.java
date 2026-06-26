package com.alibou.security.auth;

import com.alibou.security.config.JwtService;
import com.alibou.security.token.Token;
import com.alibou.security.token.TokenRepository;
import com.alibou.security.token.TokenType;
import com.alibou.security.user.Role;
import com.alibou.security.user.User;
import com.alibou.security.user.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

  private final UserRepository        userRepository;
  private final TokenRepository       tokenRepository;
  private final PasswordEncoder       passwordEncoder;
  private final JwtService            jwtService;
  private final AuthenticationManager authenticationManager;

  // ── REGISTER ─────────────────────────────────────────────────────────────
  public AuthenticationResponse register(RegisterRequest request) {
    // FIX: check for duplicate email before inserting
    if (userRepository.findByEmail(request.getEmail()).isPresent()) {
      throw new IllegalStateException("Email already in use: " + request.getEmail());
    }

    var user = User.builder()
            .firstname(request.getFirstname())
            .lastname(request.getLastname())
            .email(request.getEmail())
            .password(passwordEncoder.encode(request.getPassword()))
            .matricule(request.getMatricule())
            .poste(request.getPoste())
            .role(request.getRole() != null ? request.getRole() : Role.PREPARATEUR)
            .actif(true)
            .build();

    var savedUser    = userRepository.save(user);
    var accessToken  = jwtService.generateToken(savedUser);
    var refreshToken = jwtService.generateRefreshToken(savedUser);
    saveUserToken(savedUser, accessToken);

    return buildResponse(savedUser, accessToken, refreshToken);
  }

  // ── AUTHENTICATE ─────────────────────────────────────────────────────────
  public AuthenticationResponse authenticate(AuthenticationRequest request) {
    authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
    );
    // FIX: use findByEmail which returns Optional — safe after unique constraint
    var user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new RuntimeException("User not found"));
    var accessToken  = jwtService.generateToken(user);
    var refreshToken = jwtService.generateRefreshToken(user);
    revokeAllUserTokens(user);
    saveUserToken(user, accessToken);

    return buildResponse(user, accessToken, refreshToken);
  }

  // ── REFRESH TOKEN ─────────────────────────────────────────────────────────
  public void refreshToken(HttpServletRequest request, HttpServletResponse response)
          throws IOException {

    final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (authHeader == null || !authHeader.startsWith("Bearer ")) return;

    final String refreshToken = authHeader.substring(7);
    final String userEmail    = jwtService.extractUsername(refreshToken);

    if (userEmail != null) {
      var user = userRepository.findByEmail(userEmail).orElseThrow();
      if (jwtService.isTokenValid(refreshToken, user)) {
        var accessToken = jwtService.generateToken(user);
        revokeAllUserTokens(user);
        saveUserToken(user, accessToken);
        var authResponse = buildResponse(user, accessToken, refreshToken);
        new ObjectMapper().writeValue(response.getOutputStream(), authResponse);
      }
    }
  }

  // ── HELPERS ───────────────────────────────────────────────────────────────
  private void saveUserToken(User user, String jwtToken) {
    var token = Token.builder()
            .user(user)
            .token(jwtToken)
            .tokenType(TokenType.BEARER)
            .expired(false)
            .revoked(false)
            .build();
    tokenRepository.save(token);
  }

  private void revokeAllUserTokens(User user) {
    var validTokens = tokenRepository.findAllValidTokenByUser(user.getId());
    if (validTokens.isEmpty()) return;
    validTokens.forEach(t -> { t.setExpired(true); t.setRevoked(true); });
    tokenRepository.saveAll(validTokens);
  }

  private AuthenticationResponse buildResponse(User user, String access, String refresh) {
    return AuthenticationResponse.builder()
            .accessToken(access)
            .refreshToken(refresh)
            .email(user.getEmail())
            .firstname(user.getFirstname())
            .lastname(user.getLastname())
            .matricule(user.getMatricule())
            .poste(user.getPoste())
            .role(user.getRole())
            .id(user.getId())
            .build();
  }
}
