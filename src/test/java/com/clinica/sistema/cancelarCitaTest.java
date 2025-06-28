package com.clinica.sistema;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.clinica.sistema.Modelo.Cita;
import com.clinica.sistema.Modelo.Horario;
import com.clinica.sistema.Modelo.Medico;
import com.clinica.sistema.Repositorio.CitaRepositorio;
import com.clinica.sistema.Repositorio.HorarioRepositorio;
import com.clinica.sistema.Servicio.CitaServicio;

@ExtendWith(MockitoExtension.class)
public class cancelarCitaTest {

    @Mock
    private CitaRepositorio citaRepositorio;

    @Mock
    private HorarioRepositorio horarioRepositorio;

    @InjectMocks
    private CitaServicio citaServicio;

    @Test
    void cancelarCita() {
        System.out.println("****** Iniciando test cancelarCita() ******");

        // PASO 1: Preparar datos simulados
        System.out.println("PASO 1: Preparar datos simulados");
        Long idCita = 10L;

        Medico medico = new Medico();
        medico.setId(2L);
        System.out.println("Medico simulado creado con ID 2");

        LocalDate fecha = LocalDate.of(2025, 7, 15);
        LocalTime hora = LocalTime.of(9, 30);

        Cita cita = new Cita();
        cita.setId(idCita);
        cita.setEstado("Pendiente");
        cita.setFecha(fecha);
        cita.setHora(hora);
        cita.setMedico(medico);
        System.out.println("Cita simulada creada con estado 'Pendiente' y fecha/hora/médico asociados");

        Horario horario = new Horario();
        horario.setId(5L);
        horario.setFecha(fecha);
        horario.setHora(hora);
        horario.setMedico(medico);
        horario.setDisponible(false);
        System.out.println("Horario simulado asociado a la cita, inicialmente marcado como no disponible");

        // PASO 2: Configurar mocks
        System.out.println("");
        System.out.println("PASO 2: Configuración de mocks");
        when(citaRepositorio.findById(idCita)).thenReturn(Optional.of(cita));
        when(horarioRepositorio.findByFechaAndHoraAndMedico(fecha, hora, medico)).thenReturn(Optional.of(horario));
        System.out.println("Mocks configurados correctamente: cita y horario encontrados");

        // PASO 3: Ejecutar método
        System.out.println("");
        System.out.println("PASO 3: Ejecutando citaServicio.cancelarCita(" + idCita + ")");
        boolean resultado = citaServicio.cancelarCita(idCita);

        // PASO 4: Verificar resultados
        System.out.println("");
        System.out.println("PASO 4: Verificando resultados con AssertJ");
        assertThat(resultado).isTrue();
        System.out.println("El método retornó true, indicando que la cancelación fue exitosa");

        assertThat(cita.getEstado()).isEqualTo("Cancelada");
        System.out.println("El estado de la cita cambió a 'Cancelada' correctamente");

        assertThat(horario.isDisponible()).isTrue();
        System.out.println("El horario fue marcado como disponible tras la cancelación");

        verify(citaRepositorio).save(cita);
        System.out.println("La cita fue guardada con estado actualizado");

        verify(horarioRepositorio).save(horario);
        System.out.println("El horario fue actualizado y guardado como disponible");

        System.out.println("");
        System.out.println("****** Test cancelarCita() ejecutado exitosamente ******");
    }
}
