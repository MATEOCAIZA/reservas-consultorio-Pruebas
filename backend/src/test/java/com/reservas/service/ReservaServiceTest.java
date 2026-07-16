package com.reservas.service;

import com.reservas.dto.ReservaRequest;
import com.reservas.dto.ReservaResponse;
import com.reservas.entity.Reserva;
import com.reservas.entity.Servicio;
import com.reservas.entity.Usuario;
import com.reservas.repository.ReservaRepository;
import com.reservas.repository.ServicioRepository;
import com.reservas.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservaServiceTest {

    @Mock
    private ReservaRepository reservaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private ServicioRepository servicioRepository;

    @InjectMocks
    private ReservaService reservaService;

    private ReservaRequest crearRequest(String email) {
        ReservaRequest request = new ReservaRequest();
        request.setNombre("Juan Perez");
        request.setTelefono("0999999999");
        request.setEmail(email);
        request.setIdServicio(10L);
        request.setFecha(LocalDate.of(2026, 8, 1));
        request.setHora(LocalTime.of(10, 30));
        request.setObservaciones("Primera visita");
        return request;
    }

    @Test
    void crearReserva_conEmailNuevo_creaUsuarioClienteYReservaPendiente() {
        ReservaRequest request = crearRequest("juan@example.com");
        Servicio servicio = new Servicio("Consulta General", 25, "Consulta médica general");
        servicio.setIdServicio(10L);

        when(usuarioRepository.findByEmail("juan@example.com")).thenReturn(Optional.empty());
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(servicioRepository.findById(10L)).thenReturn(Optional.of(servicio));
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReservaResponse response = reservaService.crearReserva(request);

        ArgumentCaptor<Usuario> usuarioCaptor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(usuarioCaptor.capture());
        Usuario usuarioCreado = usuarioCaptor.getValue();

        assertThat(usuarioCreado.getEmail()).isEqualTo("juan@example.com");
        assertThat(usuarioCreado.getRol()).isEqualTo(Usuario.Rol.CLIENTE);
        assertThat(response.getEstado()).isEqualTo("Pendiente");
        assertThat(response.getNombreServicio()).isEqualTo("Consulta General");
    }

    @Test
    void crearReserva_conEmailExistente_reutilizaElUsuarioSinCrearOtro() {
        ReservaRequest request = crearRequest("maria@example.com");
        Usuario usuarioExistente = new Usuario("Maria Lopez", "0988888888", "maria@example.com", Usuario.Rol.CLIENTE);
        usuarioExistente.setIdUsuario(5L);
        Servicio servicio = new Servicio("Limpieza Dental", 40, "Limpieza y revisión dental");
        servicio.setIdServicio(10L);

        when(usuarioRepository.findByEmail("maria@example.com")).thenReturn(Optional.of(usuarioExistente));
        when(servicioRepository.findById(10L)).thenReturn(Optional.of(servicio));
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReservaResponse response = reservaService.crearReserva(request);

        verify(usuarioRepository, never()).save(any(Usuario.class));
        assertThat(response.getNombreCliente()).isEqualTo("Maria Lopez");
    }

    @Test
    void crearReserva_conServicioInexistente_lanzaExcepcionYNoGuardaReserva() {
        ReservaRequest request = crearRequest("juan@example.com");
        when(usuarioRepository.findByEmail("juan@example.com"))
                .thenReturn(Optional.of(new Usuario("Juan Perez", "0999999999", "juan@example.com", Usuario.Rol.CLIENTE)));
        when(servicioRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reservaService.crearReserva(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Servicio no encontrado");

        verify(reservaRepository, never()).save(any(Reserva.class));
    }

    @Test
    void confirmarReserva_conIdExistente_cambiaEstadoAConfirmada() {
        Usuario cliente = new Usuario("Juan Perez", "0999999999", "juan@example.com", Usuario.Rol.CLIENTE);
        Servicio servicio = new Servicio("Consulta General", 25, "Consulta médica general");
        Reserva reservaPendiente = new Reserva(cliente, servicio, LocalDate.of(2026, 8, 1), LocalTime.of(10, 30));
        reservaPendiente.setIdReserva(7L);
        reservaPendiente.setEstado("Pendiente");

        when(reservaRepository.findById(7L)).thenReturn(Optional.of(reservaPendiente));
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReservaResponse response = reservaService.confirmarReserva(7L);

        assertThat(response.getEstado()).isEqualTo("Confirmada");
        verify(reservaRepository).save(reservaPendiente);
    }

    @Test
    void actualizarEstadoReserva_conIdInexistente_lanzaExcepcionYNoGuarda() {
        when(reservaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reservaService.rechazarReserva(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Reserva no encontrada");

        verify(reservaRepository, never()).save(any(Reserva.class));
    }
}
