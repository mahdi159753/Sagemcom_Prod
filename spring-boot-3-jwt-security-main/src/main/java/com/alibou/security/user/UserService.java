package com.alibou.security.user;

import com.alibou.security.token.TokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository  repository;


    // ── CRUD ──────────────────────────────────────────────────────────────────

    public List<User> getAllUsers() {
        return repository.findAll();
    }

    public User getUserById(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found: " + id));
    }

    /**
     * Update an existing user.
     * Password is NOT changed here — use changePassword() for that.
     */
    public User updateUser(Integer id, User updated) {
        User user = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found: " + id));

        user.setFirstname(updated.getFirstname());
        user.setLastname(updated.getLastname());
        user.setEmail(updated.getEmail());
        user.setRole(updated.getRole());
        user.setPoste(updated.getPoste());
        user.setMatricule(updated.getMatricule());
        user.setActif(updated.isActif());

        return repository.save(user);
    }

    public void deleteUser(Integer id) {
        if (!repository.existsById(id)) {
            throw new RuntimeException("User not found: " + id);
        }

        repository.deleteById(id);
    }

    // ── Change own password ───────────────────────────────────────────────────

    public void updateLastActivity(Principal connectedUser) {
        if (connectedUser != null && connectedUser.getName() != null) {
            repository.findByEmail(connectedUser.getName()).ifPresent(user -> {
                user.setLastActive(java.time.LocalDateTime.now());
                repository.save(user);
            });
        }
    }

    public void changePassword(ChangePasswordRequest request, Principal connectedUser) {
        var user = (User) ((UsernamePasswordAuthenticationToken) connectedUser).getPrincipal();

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new IllegalStateException("Wrong password");
        }
        if (!request.getNewPassword().equals(request.getConfirmationPassword())) {
            throw new IllegalStateException("Passwords do not match");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        repository.save(user);
    }
}
