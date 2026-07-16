package com.reservas.service;

import com.reservas.dto.LoginRequest;
import com.reservas.dto.LoginResponse;
import com.reservas.entity.Usuario;
import com.reservas.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    private Usuario administrador;

    @BeforeEach
    void setUp() {
        administrador = new Usuario();
        administrador.setIdUsuario(1L);
        administrador.setNombre("Admin");
        administrador.setEmail("admin@reservas.com");
        administrador.setPassword("hash-de-la-contrasena");
        administrador.setRol(Usuario.Rol.ADMINISTRADOR);
    }

    @Test
    void login_conCredencialesValidas_retornaTokenYDatosDelUsuario() {
        LoginRequest request = new LoginRequest("admin@reservas.com", "admin123");
        when(usuarioRepository.findByEmail("admin@reservas.com")).thenReturn(Optional.of(administrador));
        when(passwordEncoder.matches("admin123", "hash-de-la-contrasena")).thenReturn(true);

        LoginResponse response = authService.login(request);

        assertThat(response.getToken()).startsWith("admin-token-");
        assertThat(response.getNombre()).isEqualTo("Admin");
        assertThat(response.getEmail()).isEqualTo("admin@reservas.com");
        assertThat(response.getRol()).isEqualTo("Administrador");
    }

    @Test
    void login_conPasswordIncorrecta_lanzaExcepcion() {
        LoginRequest request = new LoginRequest("admin@reservas.com", "password-equivocada");
        when(usuarioRepository.findByEmail("admin@reservas.com")).thenReturn(Optional.of(administrador));
        when(passwordEncoder.matches("password-equivocada", "hash-de-la-contrasena")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Contraseña incorrecta");
    }

    @Test
    void login_conEmailNoRegistrado_lanzaExcepcion() {
        LoginRequest request = new LoginRequest("desconocido@reservas.com", "cualquiera");
        when(usuarioRepository.findByEmail("desconocido@reservas.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Usuario no encontrado");
    }

    @Test
    void login_conRolCliente_lanzaExcepcionPorFaltaDePermisos() {
        Usuario cliente = new Usuario();
        cliente.setEmail("cliente@reservas.com");
        cliente.setPassword("hash-cliente");
        cliente.setRol(Usuario.Rol.CLIENTE);

        LoginRequest request = new LoginRequest("cliente@reservas.com", "cliente123");
        when(usuarioRepository.findByEmail("cliente@reservas.com")).thenReturn(Optional.of(cliente));
        when(passwordEncoder.matches("cliente123", "hash-cliente")).thenReturn(true);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("No tienes permisos de administrador");
    }
}
