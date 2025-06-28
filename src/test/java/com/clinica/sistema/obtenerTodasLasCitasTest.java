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

import com.clinica.sistema.Modelo.Cita;
import com.clinica.sistema.Repositorio.CitaRepositorio;
import com.clinica.sistema.Servicio.CitaServicio;

@ExtendWith(MockitoExtension.class)
public class obtenerTodasLasCitasTest {

    @Mock
    private CitaRepositorio citaRepositorio;

    @InjectMocks
    private CitaServicio citaServicio;

    @Test
    void obtenerTodasLasCitas() {
        System.out.println("****** Iniciando test obtenerTodasLasCitas() ******");

        // PASO 1: Preparar datos 
        System.out.println("");
        System.out.println("PASO 1: Preparar datos simulados");
        System.out.println("");
        Cita cita1 = new Cita();
        cita1.setId(1L);
        cita1.setEstado("Pendiente");

        Cita cita2 = new Cita();
        cita2.setId(2L);
        cita2.setEstado("Cancelada");

        List<Cita> citasSimuladas = Arrays.asList(cita1, cita2);
        System.out.println("2 citas simuladas creadas");

        // PASO 2: Configurar 
        System.out.println("");
        System.out.println("PASO 2: Configurar mocks");
        System.out.println("");
        when(citaRepositorio.findAll()).thenReturn(citasSimuladas);
        System.out.println("Mock configurado para retornar lista de 2 citas");

        // PASO 3: Ejecutar método
        System.out.println("");
        System.out.println("PASO 3: Ejecutar método obtenerTodasLasCitas()");
        System.out.println("");
        List<Cita> resultado = citaServicio.obtenerTodasLasCitas();

        // PASO 4: Verificar resultados
        System.out.println("");
        System.out.println("PASO 4: Verificar resultados con AssertJ");
        System.out.println("");

        assertThat(resultado).isNotNull();
        System.out.println("Lista de citas no es null");

        assertThat(resultado).hasSize(2);
        System.out.println("Lista contiene exactamente 2 citas");

        assertThat(resultado.get(0).getEstado()).isEqualTo("Pendiente");
        System.out.println("Primera cita tiene estado 'Pendiente'");

        assertThat(resultado.get(1).getEstado()).isEqualTo("Cancelada");
        System.out.println("Segunda cita tiene estado 'Cancelada'");

        verify(citaRepositorio, times(1)).findAll();
        System.out.println("Se verificó que se llamó una vez a citaRepositorio.findAll()");

        System.out.println("");
        System.out.println("****** Test obtenerTodasLasCitas() ejecutado exitosamente ******");
    }
}
