package com.sensei.app.service;

import com.sensei.app.dtos.CreateUserDTO;
import com.sensei.app.dtos.ResponseUserDTO;
import com.sensei.app.dtos.UpdateUserDTO;
import com.sensei.app.exceptions.EmailAlreadyExistsException;
import com.sensei.app.exceptions.InvalidPasswordException;
import com.sensei.app.exceptions.NoChangesMadeException;
import com.sensei.app.exceptions.UnsupportedUpdateFieldException;
import com.sensei.app.exceptions.UserDeletionConflictException;
import com.sensei.app.exceptions.UserNotFoundException;
import com.sensei.app.model.User;
import com.sensei.app.repository.ClassRepository;
import com.sensei.app.repository.MaterialRepository;
import com.sensei.app.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    private static final int BCRYPT_MAX_PASSWORD_BYTES = 72;

    private final UserRepository userRepository;
    private final ClassRepository classRepository;
    private final MaterialRepository materialRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       ClassRepository classRepository,
                       MaterialRepository materialRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.classRepository = classRepository;
        this.materialRepository = materialRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<ResponseUserDTO> index() {
        return this.userRepository.findAll()
                .stream()
                .map(ResponseUserDTO::from)
                .toList();
    }

    public ResponseUserDTO getUser(String id) {
        return ResponseUserDTO.from(this.findUser(id));
    }

    /**
     * Cadastro público: o papel (TEACHER ou STUDENT) é definido somente aqui.
     */
    public ResponseUserDTO createUser(CreateUserDTO userDTO) throws EmailAlreadyExistsException {
        if (userDTO.getPassword().getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_PASSWORD_BYTES) {
            throw new InvalidPasswordException("Password must not exceed " + BCRYPT_MAX_PASSWORD_BYTES + " bytes.");
        }

        User user = userDTO.toEntity();

        this.emailRegistered(user.getEmail());

        user.setPassword(passwordEncoder.encode(user.getPassword()));

        this.userRepository.save(user);

        return ResponseUserDTO.from(user);
    }

    /**
     * Atualiza somente a conta do próprio usuário autenticado.
     * Apenas name e email são atualizáveis; o papel é definido no cadastro e não muda.
     */
    public ResponseUserDTO updateUser(String targetUserId, String authenticatedUserId, UpdateUserDTO updateDTO) {
        this.assertOwnAccount(targetUserId, authenticatedUserId);

        if (!updateDTO.getUnsupportedFields().isEmpty()) {
            throw new UnsupportedUpdateFieldException(
                    "Only 'name' and 'email' can be updated. Fields not allowed: " + updateDTO.getUnsupportedFields());
        }

        boolean hasName = hasText(updateDTO.getName());
        boolean hasEmail = hasText(updateDTO.getEmail());

        if (!hasName && !hasEmail) {
            throw new NoChangesMadeException("No fields were filled for update. Fields: [name, email]");
        }

        User user = this.findUser(targetUserId);

        if (hasName) {
            user.setName(updateDTO.getName());
        }

        if (hasEmail && !updateDTO.getEmail().equals(user.getEmail())) {
            this.emailRegistered(updateDTO.getEmail());
            user.setEmail(updateDTO.getEmail());
        }

        this.userRepository.save(user);

        return ResponseUserDTO.from(user);
    }

    /**
     * Exclui somente a conta do próprio usuário autenticado.
     * Contas referenciadas por turmas ou materiais não são excluídas (sem cascata),
     * para não deixar referências órfãs em outros documentos.
     */
    public void deleteUser(String targetUserId, String authenticatedUserId) {
        this.assertOwnAccount(targetUserId, authenticatedUserId);

        User user = this.findUser(targetUserId);

        boolean referenced = this.classRepository.existsByTeacherId(user.getId())
                || this.classRepository.existsByStudentIds(user.getId())
                || this.materialRepository.existsByTeacherId(user.getId());

        if (referenced) {
            throw new UserDeletionConflictException(
                    "The account cannot be deleted while it is referenced by classes or materials.");
        }

        this.userRepository.deleteById(user.getId());
    }

    //  UTILS

    void emailRegistered(String email) {
        Optional<User> user = this.userRepository.findByEmail(email);

        if (user.isPresent()) {
            throw new EmailAlreadyExistsException("The email " + email + " is already registered");
        }
    }

    private User findUser(String id) {
        return this.userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found."));
    }

    /**
     * Compara a identidade autenticada (ID persistido, vindo do principal) com o usuário-alvo.
     * A resposta é 403 independentemente de o alvo existir, para não revelar contas de terceiros.
     */
    private void assertOwnAccount(String targetUserId, String authenticatedUserId) {
        if (authenticatedUserId == null || !authenticatedUserId.equals(targetUserId)) {
            throw new AccessDeniedException("Users can only modify their own account.");
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
