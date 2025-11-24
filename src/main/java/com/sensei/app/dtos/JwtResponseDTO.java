package com.sensei.app.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JwtResponseDTO {
    public JwtResponseDTO(String jwt) {
        token = jwt;
    }

    private String token;
    private String type = "Bearer";
}
