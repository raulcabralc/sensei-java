package com.sensei.app.dtos;

import com.sensei.app.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResponseUserDTO {
    private String id;

    private String name;

    private UserRole role;

    private String email;
}