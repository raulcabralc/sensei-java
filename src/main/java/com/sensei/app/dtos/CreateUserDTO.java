package com.sensei.app.dtos;

import com.sensei.app.enums.UserRole;
import com.sensei.app.model.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserDTO {
    @NotBlank(message = "Name is required.")
    private String name;

    @NotBlank(message = "Email is required.")
    @Email(message = "Invalid email.")
    private String email;

    @NotNull(message = "Role is required.")
    private UserRole role;

    // BCrypt processa no máximo 72 bytes; o UserService também valida o tamanho em bytes.
    @NotBlank(message = "Password is required.")
    @Size(min = 8, max = 72, message = "Password must have between 8 and 72 characters.")
    private String password;

    public User toEntity() {
        User user = new User();

        user.setName(this.name);
        user.setEmail(this.email);
        user.setRole(this.role);
        user.setPassword(this.password);

        return user;
    }
}
