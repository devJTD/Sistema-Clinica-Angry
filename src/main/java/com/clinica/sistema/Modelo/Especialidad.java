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
@Table(name = "especialidades") // Mapea la entidad a una tabla llamada "especialidades"
public class Especialidad {

    @Id // Marca este campo como la clave primaria
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Configura la generación automática del ID
    private Long id;

    @Column(nullable = false, unique = true, length = 100) // El nombre de la especialidad debe ser único y no nulo
    private String nombre;
}