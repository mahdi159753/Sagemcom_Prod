package com.alibou.security.chat;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserContactDto {
    private Integer id;
    private String firstname;
    private String lastname;
    private String email;
    private LocalDateTime lastActive;
}
