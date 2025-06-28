package com.clinica.sistema;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.clinica.sistema.Modelo.Especialidad;
import com.clinica.sistema.Repositorio.EspecialidadRepositorio;
import com.clinica.sistema.Servicio.CitaServicio;

@ExtendWith(MockitoExtension.class)
public class obtenerTodasLasEspecialidadesTest {

    @Mock
    private EspecialidadRepositorio especialidadRepositorio;

    @InjectMocks
    private CitaServicio citaServicio;

    @Test
    void obtenerTodasLasEspecialidades() {
        System.out.println("****** Iniciando test obtenerTodasLasEspecialidades() ******");

        // PASO 1: Preparar datos simulados
        System.out.println("PASO 1: Preparar datos simulados");
        Especialidad esp1 = new Especialidad();
        esp1.setId(1L);
        esp1.setNombre("Pediatría");

        Especialidad esp2 = new Especialidad();
        esp2.setId(2L);
        esp2.setNombre("Cardiología");

        List<Especialidad> especialidadesSimuladas = Arrays.asList(esp1, esp2);
        System.out.println("Se simularon 2 especialidades: Pediatría y Cardiología");

        // PASO 2: Configurar mocks
        System.out.println("");
        System.out.println("PASO 2: Configuración de mocks");
        when(especialidadRepositorio.findAll()).thenReturn(especialidadesSimuladas);
        System.out.println("Mock configurado para retornar lista de especialidades simuladas");

        // PASO 3: Ejecutar método
        System.out.println("");
        System.out.println("PASO 3: Ejecutando citaServicio.obtenerTodasLasEspecialidades()");
        List<Especialidad> resultado = citaServicio.obtenerTodasLasEspecialidades();

        // PASO 4: Verificar resultados
        System.out.println("");
        System.out.println("PASO 4: Verificando resultados con AssertJ");

        assertThat(resultado).isNotNull();
        System.out.println("La lista retornada no es null, no está vacía");

        assertThat(resultado).hasSize(2);
        System.out.println("La lista contiene exactamente 2 especialidades");

        assertThat(resultado.get(0).getNombre()).isEqualTo("Pediatría");
        assertThat(resultado.get(1).getNombre()).isEqualTo("Cardiología");
        System.out.println("Los nombres de las especialidades son correctos");

        verify(especialidadRepositorio, times(1)).findAll();
        System.out.println("Se verificó que se llamó al repositorio exactamente 1 vez");

        System.out.println("");
        System.out.println("****** Test obtenerTodasLasEspecialidades() ejecutado exitosamente ******");
    }
}
