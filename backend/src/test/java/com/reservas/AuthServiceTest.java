package com.reservas;

import com.reservas.dto.LoginRequest;
import com.reservas.dto.LoginResponse;
import com.reservas.entity.Usuario;
import com.reservas.repository.UsuarioRepository;
import com.reservas.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void loginConCredencialesValidasDebeRetornarToken() {
        Usuario usuario = new Usuario("Ana", "0999999999", "ana@test.com", Usuario.Rol.ADMINISTRADOR);
        usuario.setPassword("hashedPassword");

        when(usuarioRepository.findByEmail("ana@test.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("123456", "hashedPassword")).thenReturn(true);

        LoginResponse response = authService.login(new LoginRequest("ana@test.com", "123456"));

        assertNotNull(response);
        assertEquals("ana@test.com", response.getEmail());
        assertTrue(response.getToken().startsWith("admin-token-"));
    }

    @Test
    void loginConPasswordIncorrectaDebeLanzarExcepcion() {
        Usuario usuario = new Usuario("Ana", "0999999999", "ana@test.com", Usuario.Rol.ADMINISTRADOR);
        usuario.setPassword("hashedPassword");

        when(usuarioRepository.findByEmail("ana@test.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("wrong", "hashedPassword")).thenReturn(false);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> authService.login(new LoginRequest("ana@test.com", "wrong")));

        assertEquals("Contraseña incorrecta", exception.getMessage());
    }
}
