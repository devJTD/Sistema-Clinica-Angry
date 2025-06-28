package com.clinica.sistema;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.clinica.sistema.Modelo.Cita;
import com.clinica.sistema.Modelo.Especialidad;
import com.clinica.sistema.Modelo.Horario;
import com.clinica.sistema.Modelo.Medico;
import com.clinica.sistema.Modelo.Paciente;
import com.clinica.sistema.Repositorio.CitaRepositorio;
import com.clinica.sistema.Repositorio.HorarioRepositorio;
import com.clinica.sistema.Repositorio.MedicoRepositorio;
import com.clinica.sistema.Repositorio.PacienteRepositorio;
import com.clinica.sistema.Servicio.CitaServicio;
import com.clinica.sistema.Servicio.NotificacionServicio;

@ExtendWith(MockitoExtension.class)
public class crearCitaTest {

    @Mock
    private CitaRepositorio citaRepositorio;
    @Mock
    private PacienteRepositorio pacienteRepositorio;
    @Mock
    private MedicoRepositorio medicoRepositorio;
    @Mock
    private HorarioRepositorio horarioRepositorio;
    @Mock
    private NotificacionServicio notificacionServicio;

    @InjectMocks
    private CitaServicio citaServicio;

    @Test
    void crearCita() {
        System.out.println("******  Iniciando test crearCita() ********");

        // Datos de prueba
        String fechaStr = "2025-07-10";
        String horaStr = "10:00";
        Long idPaciente = 1L;
        Long idMedico = 2L;
        LocalDate fecha = LocalDate.parse(fechaStr);
        LocalTime hora = LocalTime.parse(horaStr);

        System.out.println("");
        System.out.println("PASO 1: Datos de entrada preparados:");
        System.out.println("");

        // Crear paciente
        Paciente paciente = new Paciente();
        paciente.setId(idPaciente);
        paciente.setNombre("Juan");
        paciente.setApellido("Pérez");
        paciente.setCorreo("juanperez@example.com");
        System.out.println("Paciente creado");

        // Crear especialidad y médico
        Especialidad especialidad = new Especialidad();
        especialidad.setId(1L);
        especialidad.setNombre("Cardiología");

        Medico medico = new Medico();
        medico.setId(idMedico);
        medico.setNombre("Carlos");
        medico.setApellido("Ramírez");
        medico.setEspecialidad(especialidad);
        System.out.println("Medico con Especialidad creado");

        // Crear horario
        Horario horario = new Horario();
        horario.setId(5L);
        horario.setFecha(fecha);
        horario.setHora(hora);
        horario.setDisponible(true);
        horario.setMedico(medico);
        System.out.println("Horario creado");

        // Cita que será devuelta simulando persistencia
        System.out.println("Preparacion basica Cita");
        Cita citaGuardada = new Cita();
        citaGuardada.setId(100L);
        citaGuardada.setEstado("Pendiente");

        

        // Simular comportamiento de los repositorios
        System.out.println("");
        System.out.println("PASO 2: Configuración de mocks terminada");
        System.out.println("");
        when(pacienteRepositorio.findById(idPaciente)).thenReturn(Optional.of(paciente));
        when(medicoRepositorio.findById(idMedico)).thenReturn(Optional.of(medico));
        when(horarioRepositorio.findByFechaAndHoraAndMedico(fecha, hora, medico)).thenReturn(Optional.of(horario));
        when(citaRepositorio.save(any(Cita.class))).thenReturn(citaGuardada);

        // Ejecutar método
        System.out.println("");
        System.out.println("PASO 3: Ejecutando citaServicio.crearCita(...)");
        System.out.println("");
        Cita resultado = citaServicio.crearCita(fechaStr, horaStr, idMedico, idPaciente);

        System.out.println("");
        System.out.println("PASO 4: Verificando resultados con AssertJ");
        System.out.println("");

        assertThat(resultado).isNotNull();
        System.out.println("Cita creada correctamente (no es null)");

        System.out.println("Estado de la cita: " + resultado.getEstado());
        assertThat(resultado.getEstado()).isEqualTo("Pendiente");
        System.out.println("Estado de la cita es 'Pendiente'");

        verify(horarioRepositorio).save(horario);
        System.out.println("El horario fue actualizado y guardado");

        verify(citaRepositorio, times(2)).save(any(Cita.class));
        System.out.println("La cita fue guardada dos veces (una por lógica principal, otra por envío notificación)");

        verify(notificacionServicio).enviarCorreoSimple(
                eq(paciente.getCorreo()),
                contains("Confirmación de Cita Médica"),
                contains("Hola Juan"));
        System.out.println("Se envió la notificación por correo correctamente");

        System.out.println("");
        System.out.println("******** Test crearCita() ejecutado exitosamente ********");
    }

}
