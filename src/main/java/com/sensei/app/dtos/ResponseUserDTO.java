package com.sensei.app.dtos;

import com.sensei.app.enums.UserRole;
import com.sensei.app.model.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Representação pública de um usuário. Nunca inclui senha ou hash.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResponseUserDTO {
    private String id;

    private String name;

    private UserRole role;

    private String email;

    public static ResponseUserDTO from(User user) {
        return new ResponseUserDTO(user.getId(), user.getName(), user.getRole(), user.getEmail());
    }
}
