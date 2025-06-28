package com.clinica.sistema;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.clinica.sistema.Modelo.Cita;
import com.clinica.sistema.Modelo.Paciente;
import com.clinica.sistema.Repositorio.CitaRepositorio;
import com.clinica.sistema.Repositorio.PacienteRepositorio;
import com.clinica.sistema.Servicio.CitaServicio;

@ExtendWith(MockitoExtension.class)
public class obtenerHistorialCitasPorPacienteTest {

    @Mock
    private CitaRepositorio citaRepositorio;

    @Mock
    private PacienteRepositorio pacienteRepositorio;

    @InjectMocks
    private CitaServicio citaServicio;

    @Test
    void obtenerHistorialCitasPorPaciente() {
        System.out.println("****** Iniciando test obtenerHistorialCitasPorPaciente() ******");

        // PASO 1: Preparar datos simulados
        System.out.println("PASO 1: Preparar datos simulados");
        Long idPaciente = 1L;

        Paciente paciente = new Paciente();
        paciente.setId(idPaciente);
        paciente.setNombre("Luis");
        paciente.setApellido("García");
        paciente.setCorreo("luis.garcia@example.com");
        System.out.println("Paciente simulado creado con ID 1");

        Cita cita1 = new Cita();
        cita1.setId(20L);
        cita1.setEstado("Cancelada");

        Cita cita2 = new Cita();
        cita2.setId(21L);
        cita2.setEstado("Pendiente");

        List<Cita> todasLasCitas = Arrays.asList(cita1, cita2);

        // PASO 2: Configurar mocks
        System.out.println("PASO 2: Configurar mocks");
        when(pacienteRepositorio.findById(idPaciente)).thenReturn(Optional.of(paciente));

        // Simulamos que en base de datos están ambas, pero el método solo devuelve las no Pendientes
        when(citaRepositorio.findByPacienteAndEstadoNot(paciente, "Pendiente"))
                .thenReturn(todasLasCitas.stream()
                        .filter(c -> !c.getEstado().equals("Pendiente"))
                        .collect(Collectors.toList()));
        System.out.println("Mocks configurados con 2 citas, pero solo se retornará la Cancelada");

        // PASO 3: Ejecutar método
        System.out.println("PASO 3: Ejecutar método obtenerHistorialCitasPorPaciente()");
        List<Cita> resultado = citaServicio.obtenerHistorialCitasPorPaciente(idPaciente);

        // PASO 4: Verificar resultados
        System.out.println("PASO 4: Verificar resultados con AssertJ");
        assertThat(resultado).isNotNull();
        System.out.println("Lista de historial no es null");

        assertThat(resultado).hasSize(1);
        System.out.println("Lista contiene exactamente 1 cita (excluyendo la Pendiente)");

        verify(pacienteRepositorio).findById(idPaciente);
        verify(citaRepositorio).findByPacienteAndEstadoNot(paciente, "Pendiente");
        System.out.println("Verificaciones de llamadas a los repositorios realizadas correctamente");

        System.out.println("****** Test obtenerHistorialCitasPorPaciente() ejecutado exitosamente ******");
    }
}
