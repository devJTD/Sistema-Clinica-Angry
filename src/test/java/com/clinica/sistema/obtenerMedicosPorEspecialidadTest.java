package com.clinica.sistema;

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

import com.clinica.sistema.Modelo.Especialidad;
import com.clinica.sistema.Modelo.Medico;
import com.clinica.sistema.Repositorio.EspecialidadRepositorio;
import com.clinica.sistema.Repositorio.MedicoRepositorio;
import com.clinica.sistema.Servicio.CitaServicio;

@ExtendWith(MockitoExtension.class)
public class obtenerMedicosPorEspecialidadTest {

    @Mock
    private EspecialidadRepositorio especialidadRepositorio;

    @Mock
    private MedicoRepositorio medicoRepositorio;

    @InjectMocks
    private CitaServicio citaServicio;

    @Test
    void obtenerMedicosPorEspecialidad() {
        System.out.println("****** Iniciando test obtenerMedicosPorEspecialidad() ******");

        // PASO 1: Preparar datos simulados
        System.out.println("PASO 1: Preparar datos simulados");
        Long idEspecialidad = 1L;

        Especialidad especialidad = new Especialidad();
        especialidad.setId(idEspecialidad);
        especialidad.setNombre("Dermatología");
        System.out.println("Especialidad simulada creada con ID 1 y nombre 'Dermatología'");

        Medico m1 = new Medico();
        m1.setId(10L);
        m1.setNombre("Ana");
        m1.setApellido("Santos");
        m1.setEspecialidad(especialidad);

        Medico m2 = new Medico();
        m2.setId(11L);
        m2.setNombre("Luis");
        m2.setApellido("Ramos");
        m2.setEspecialidad(especialidad);

        List<Medico> medicosSimulados = Arrays.asList(m1, m2);
        System.out.println("2 médicos simulados asociados a la especialidad");

        // PASO 2: Configurar mocks
        System.out.println("PASO 2: Configurar mocks");
        when(especialidadRepositorio.findById(idEspecialidad)).thenReturn(Optional.of(especialidad));
        when(medicoRepositorio.findByEspecialidad(especialidad)).thenReturn(medicosSimulados);
        System.out.println("Mocks configurados correctamente");

        // PASO 3: Ejecutar método
        System.out.println("PASO 3: Ejecutar método obtenerMedicosPorEspecialidad()");
        List<Medico> resultado = citaServicio.obtenerMedicosPorEspecialidad(idEspecialidad);

        // PASO 4: Verificar resultados
        System.out.println("PASO 4: Verificar resultados con AssertJ");
        assertThat(resultado).isNotNull();
        System.out.println("Lista de médicos no es null");

        assertThat(resultado).hasSize(2);
        System.out.println("Lista contiene exactamente 2 médicos");

        assertThat(resultado.get(0).getNombre()).isEqualTo("Ana");
        assertThat(resultado.get(1).getNombre()).isEqualTo("Luis");
        System.out.println("Los nombres de los médicos coinciden con los simulados");

        verify(especialidadRepositorio).findById(idEspecialidad);
        verify(medicoRepositorio).findByEspecialidad(especialidad);
        System.out.println("Se verificaron las llamadas a los repositorios correctamente");

        System.out.println("****** Test obtenerMedicosPorEspecialidad() ejecutado exitosamente ******");
    }
}
