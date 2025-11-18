package com.sensei.app.service;

import com.sensei.app.dtos.CreateUserDTO;
import com.sensei.app.dtos.ResponseUserDTO;
import com.sensei.app.dtos.UpdateUserDTO;
import com.sensei.app.exceptions.EmailAlreadyExistsException;
import com.sensei.app.exceptions.InvalidUserRoleException;
import com.sensei.app.exceptions.NoChangesMadeException;
import com.sensei.app.exceptions.UserNotFoundException;
import com.sensei.app.model.User;
import com.sensei.app.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<User> index() {
        try {
            return this.userRepository.findAll();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            throw e;
        }
    }

    public ResponseUserDTO createUser(CreateUserDTO userDTO) throws EmailAlreadyExistsException {
        try {
            User user = userDTO.toEntity();

            this.emailRegistered(user.getEmail());

            this.userRepository.save(user);

            return new ResponseUserDTO(
                    user.getId(),
                    user.getName(),
                    user.getRole(),
                    user.getEmail()
            );
        } catch (Exception e) {
            System.out.println(e.getMessage());
            throw e;
        }
    }

    public ResponseUserDTO updateUser(String id, UpdateUserDTO updateDTO) throws UserNotFoundException, InvalidUserRoleException, NoChangesMadeException {
        try {
            ArrayList<String> fields = new ArrayList<>();
            fields.add("name");
            fields.add("email");
            fields.add("role");

            boolean noChanges =
                    (updateDTO.getName() == null || updateDTO.getName().isBlank()) &&
                            updateDTO.getEmail() == null || updateDTO.getEmail().isBlank() &&
                            updateDTO.getRole() == null;

            if (noChanges) {
                throw new NoChangesMadeException("No fields were filled for update. Fields: " + fields);
            }

            User user = this.userRepository.findById(id)
                    .orElseThrow(() -> new UserNotFoundException("User with id " + id + " not found"));

            if (updateDTO.getName() != null && !updateDTO.getName().isBlank()) {
                user.setName(updateDTO.getName());
            }

            if (updateDTO.getEmail() != null && !updateDTO.getEmail().isBlank()) {
                if (!(user.getEmail().equals(updateDTO.getEmail()))) {
                    this.emailRegistered(updateDTO.getEmail());

                    user.setEmail(updateDTO.getEmail());
                }
            }

            if (updateDTO.getRole() != null) {
                user.setRole(updateDTO.getRole());
            }

            this.userRepository.save(user);

            return new ResponseUserDTO(
                    user.getId(),
                    user.getName(),
                    user.getRole(),
                    user.getEmail()
            );
        } catch (Exception e) {
            System.out.println(e.getMessage());
            throw e;
        }
    }

    public void deleteUser(String id) {
        try {
            this.userRepository.deleteById(id);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            throw e;
        }
    }

    //  UTILS

    private void emailRegistered(String email) {
        Optional<User> user = this.userRepository.findByEmail(email);

        if (user.isPresent()) {
            throw new EmailAlreadyExistsException("The email " + email + " is already registered");
        }
    }
}
