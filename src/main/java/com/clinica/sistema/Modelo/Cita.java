package com.clinica.sistema.Modelo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Cita {
    private Long id;
    private LocalDate fecha;
    private LocalTime hora;
    private String estado;
    private Long idPaciente;
    private Long idMedico;
}
