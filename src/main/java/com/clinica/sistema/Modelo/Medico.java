package com.clinica.sistema.Modelo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Medico {
    private Long id;
    private String nombreCompleto;
    private Especialidad especialidad;
    private List<Horario> horariosDisponibles;
}
