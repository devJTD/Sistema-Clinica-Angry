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
public class obtenerCitasPendientesPorPacienteTest {

    @Mock
    private CitaRepositorio citaRepositorio;

    @Mock
    private PacienteRepositorio pacienteRepositorio;

    @InjectMocks
    private CitaServicio citaServicio;

    @Test
    void obtenerCitasPendientesPorPaciente() {
        System.out.println("****** Iniciando test obtenerCitasPendientesPorPaciente ******");

        // PASO 1: Preparar datos simulados
        System.out.println("\nPASO 1: Preparar datos simulados");
        Long idPaciente = 1L;

        Paciente paciente = new Paciente();
        paciente.setId(idPaciente);
        paciente.setNombre("Ana");
        paciente.setApellido("Torres");
        paciente.setCorreo("ana.torres@example.com");
        System.out.println("Paciente simulado creado con ID 1");

        Cita cita1 = new Cita();
        cita1.setId(10L);
        cita1.setEstado("Pendiente");
        System.out.println("Cita 1 creada con estado 'Pendiente'");

        Cita cita2 = new Cita();
        cita2.setId(11L);
        cita2.setEstado("Finalizada");
        System.out.println("Cita 2 creada con estado 'Finalizada'");

        // Simulamos la base de datos con ambas citas
        List<Cita> baseDeDatosSimulada = Arrays.asList(cita1, cita2);
        System.out.println("Ambas citas agregadas a la base de datos simulada");

        // Simulamos el filtrado que haría el repositorio
        List<Cita> citasFiltradas = baseDeDatosSimulada.stream()
            .filter(c -> "Pendiente".equals(c.getEstado()))
            .collect(Collectors.toList());
        System.out.println("Se filtraron las citas con estado 'Pendiente' (esperamos 1)");

        // PASO 2: Configurar mocks
        System.out.println("\nPASO 2: Configurar mocks");
        when(pacienteRepositorio.findById(idPaciente)).thenReturn(Optional.of(paciente));
        when(citaRepositorio.findByPacienteAndEstado(paciente, "Pendiente")).thenReturn(citasFiltradas);
        System.out.println("Mocks configurados correctamente");

        // PASO 3: Ejecutar método
        System.out.println("\nPASO 3: Ejecutar método obtenerCitasPendientesPorPaciente()");
        List<Cita> resultado = citaServicio.obtenerCitasPendientesPorPaciente(idPaciente);

        // PASO 4: Verificar resultados
        System.out.println("\nPASO 4: Verificar resultados con AssertJ");
        assertThat(resultado).isNotNull();
        System.out.println("Resultado no es null");

        assertThat(resultado).hasSize(1);
        System.out.println("Se retornó exactamente 1 cita con estado 'Pendiente'");

        assertThat(resultado.get(0).getEstado()).isEqualTo("Pendiente");
        System.out.println("Estado de la cita retornada es 'Pendiente'");

        verify(pacienteRepositorio).findById(idPaciente);
        verify(citaRepositorio).findByPacienteAndEstado(paciente, "Pendiente");
        System.out.println("Verificaciones de acceso a los repositorios realizadas correctamente");

        System.out.println("\n****** Test obtenerCitasPendientesPorPaciente ejecutado exitosamente ******");
    }
}
