package com.clinica.sistema.Modelo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Horario {
    private LocalDate fecha;
    private LocalTime hora;
    private boolean disponible;
}
