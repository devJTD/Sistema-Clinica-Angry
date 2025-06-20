package com.clinica.sistema.Modelo;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne; // Importación para la relación OneToOne
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity // Indica que esta clase es una entidad JPA
@Table(name = "citas") // Mapea la entidad a una tabla llamada "citas"
public class Cita {

    @Id // Marca este campo como la clave primaria
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Configura la generación automática del ID
    private Long id;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false)
    private LocalTime hora;

    @Column(nullable = false, length = 50) // Por ejemplo: "Pendiente", "Confirmada", "Cancelada", "Completada"
    private String estado;

    @ManyToOne // Muchas citas pueden pertenecer a un solo paciente
    @JoinColumn(name = "paciente_id", nullable = false) // Columna en la tabla 'citas' que hace referencia a la clave primaria de 'pacientes'
    private Paciente paciente; // Referencia a la entidad Paciente

    @ManyToOne // Muchas citas pueden ser realizadas por un solo médico
    @JoinColumn(name = "medico_id", nullable = false) // Columna en la tabla 'citas' que hace referencia a la clave primaria de 'medicos'
    private Medico medico; // Referencia a la entidad Medico

    // ¡NUEVO CAMPO PARA LA RELACIÓN CON NOTIFICACION!
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true) // CASCADE.ALL para guardar/actualizar/eliminar la Notificacion al manipular la Cita. orphanRemoval asegura que se borre si se desvincula.
    @JoinColumn(name = "notificacion_id", referencedColumnName = "id") // Columna en 'citas' que guarda el ID de la 'notificacion'
    private Notificacion notificacion; // Referencia a la entidad Notificacion
}