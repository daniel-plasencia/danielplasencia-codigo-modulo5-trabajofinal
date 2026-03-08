package com.tecsup.app.micro.user.application.usecase;

import com.tecsup.app.micro.user.domain.exception.DuplicateEmailException;
import com.tecsup.app.micro.user.domain.exception.InvalidUserDataException;
import com.tecsup.app.micro.user.domain.model.User;
import com.tecsup.app.micro.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CreateUserUseCase {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    public User execute(User user) {
        if (!user.isValid()) {
            throw new InvalidUserDataException("Invalid user data");
        }
        
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new DuplicateEmailException(user.getEmail());
        }
        
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        if (user.getRole() == null) {
            user.setRole("USER");
        }
        
        return userRepository.save(user);
    }
}
