package com.clinica.sistema.Modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Table(name = "horarios") // Mapea la entidad a una tabla llamada "horarios"
public class Horario {

    @Id // Marca este campo como la clave primaria
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Configura la generación automática del ID
    private Long id; // Agregamos un ID para que Horario pueda ser una entidad independiente

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false)
    private LocalTime hora;

    @Column(nullable = false)
    private boolean disponible;

    @ManyToOne // Muchas instancias de Horario pueden pertenecer a un solo Medico
    @JoinColumn(name = "medico_id", nullable = false) // Columna en la tabla 'horarios' que hace referencia a la clave primaria de 'medicos'
    private Medico medico; // Referencia a la entidad Medico
}