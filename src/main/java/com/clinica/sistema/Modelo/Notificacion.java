package com.clinica.sistema.Modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity // Indica que esta clase es una entidad JPA
@Table(name = "notificaciones") // Mapea la entidad a una tabla llamada "notificaciones"
public class Notificacion {

    @Id // Marca este campo como la clave primaria
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Configura la generación automática del ID
    private Long id;

    @Column(nullable = false, length = 500) // Un mensaje puede ser largo
    private String mensaje;

    @Column(name = "id_cita", nullable = false) // Nombre de columna explícito, no nulo
    private Long idCita; // Este campo podría ser una relación con la entidad Cita
}