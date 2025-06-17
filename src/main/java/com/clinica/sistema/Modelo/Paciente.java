package com.clinica.sistema.Modelo;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity // Indica que esta clase es una entidad JPA
@Table(name = "pacientes") // Mapea la entidad a una tabla llamada "pacientes"
public class Paciente {

    @Id // Marca este campo como la clave primaria
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Configura la generación automática del ID
    private Long id;

    @Column(nullable = false, length = 100) // Configura la columna para no ser nula y tener una longitud máxima
    private String nombre;

    @Column(nullable = false, length = 100)
    private String apellido;

    @Column(nullable = false, unique = true, length = 8) // DNI debe ser único y tener longitud 8
    private String dni;

    @Column(length = 20) // Teléfono puede ser más corto
    private String telefono;

    @Column(nullable = false, unique = true, length = 100) // Correo debe ser único
    private String correo;

    @Column(nullable = false, length = 255) // Contraseña puede ser más larga para seguridad (hashing)
    private String contraseña;
}