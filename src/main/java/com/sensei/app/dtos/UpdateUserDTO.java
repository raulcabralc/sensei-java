package com.sensei.app.dtos;

import com.sensei.app.enums.UserRole;
import com.sensei.app.model.User;
import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserDTO {
    private String name;

    @Email(message = "Invalid email.")
    private String email;

    private UserRole role;

    public User toEntity() {
        User user = new User();

        user.setName(this.name);
        user.setEmail(this.email);

        return user;
    }
}
