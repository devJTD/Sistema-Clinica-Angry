package com.clinica.sistema;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.clinica.sistema.Modelo.Horario;
import com.clinica.sistema.Modelo.Medico;
import com.clinica.sistema.Repositorio.HorarioRepositorio;
import com.clinica.sistema.Repositorio.MedicoRepositorio;
import com.clinica.sistema.Servicio.CitaServicio;

@ExtendWith(MockitoExtension.class)
public class obtenerHorariosDisponiblesPorMedicoYFechaTest {

    @Mock
    private MedicoRepositorio medicoRepositorio;

    @Mock
    private HorarioRepositorio horarioRepositorio;

    @InjectMocks
    private CitaServicio citaServicio;

    @Test
    void obtenerHorariosDisponiblesPorMedicoYFecha() {
        System.out.println("****** Iniciando test obtenerHorariosDisponiblesPorMedicoYFecha() ******");

        // PASO 1: Preparar datos simulados
        System.out.println("PASO 1: Preparar datos simulados");
        Long idMedico = 2L;
        LocalDate fecha = LocalDate.of(2025, 7, 20);
        System.out.println("Fecha simulada: " + fecha);

        Medico medico = new Medico();
        medico.setId(idMedico);
        medico.setNombre("Laura");
        System.out.println("Médico simulado creado - ID: " + idMedico + ", Nombre: " + medico.getNombre());

        Horario h1 = new Horario();
        h1.setId(101L);
        h1.setFecha(fecha);
        h1.setHora(LocalTime.of(10, 0));
        h1.setDisponible(true);
        h1.setMedico(medico);
        System.out.println("Horario 1 creado - ID: " + h1.getId() + ", Hora: " + h1.getHora());

        Horario h2 = new Horario();
        h2.setId(102L);
        h2.setFecha(fecha);
        h2.setHora(LocalTime.of(11, 0));
        h2.setDisponible(true);
        h2.setMedico(medico);
        System.out.println("Horario 2 creado - ID: " + h2.getId() + ", Hora: " + h2.getHora());

        List<Horario> horariosDisponibles = Arrays.asList(h1, h2);
        System.out.println("Lista de horarios simulada con 2 elementos");

        // PASO 2: Configurar mocks
        System.out.println("PASO 2: Configurar mocks");
        when(medicoRepositorio.findById(idMedico)).thenReturn(Optional.of(medico));
        when(horarioRepositorio.findByMedicoAndFechaAndDisponibleTrue(medico, fecha)).thenReturn(horariosDisponibles);
        System.out.println("Mocks configurados correctamente");

        // PASO 3: Ejecutar método
        System.out.println("PASO 3: Ejecutar método obtenerHorariosDisponiblesPorMedicoYFecha()");
        List<Horario> resultado = citaServicio.obtenerHorariosDisponiblesPorMedicoYFecha(idMedico, fecha);

        // PASO 4: Verificar resultados
        System.out.println("PASO 4: Verificar resultados con AssertJ");
        assertThat(resultado).isNotNull();
        System.out.println("Resultado no es null");

        assertThat(resultado).hasSize(2);
        System.out.println("La lista contiene 2 horarios disponibles");

        assertThat(resultado.get(0).isDisponible()).isTrue();
        assertThat(resultado.get(1).isDisponible()).isTrue();
        System.out.println("Ambos horarios están marcados como disponibles");

        verify(medicoRepositorio).findById(idMedico);
        verify(horarioRepositorio).findByMedicoAndFechaAndDisponibleTrue(medico, fecha);
        System.out.println("Se verificaron las llamadas a los repositorios");

        System.out.println("****** Test obtenerHorariosDisponiblesPorMedicoYFecha() ejecutado exitosamente ******");
    }
}
