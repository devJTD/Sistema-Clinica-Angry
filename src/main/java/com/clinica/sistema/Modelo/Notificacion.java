package com.clinica.sistema.Modelo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Notificacion {
    private Long id;
    private String mensaje;
    private Cita cita;
}
