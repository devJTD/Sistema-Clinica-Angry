package com.clinica.sistema.Repositorio; 

import com.clinica.sistema.Modelo.Paciente;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository; 

@Repository 
public interface PacienteRepositorio extends JpaRepository<Paciente, Long> {
    Optional<Paciente> findByDni(String dni);
    Optional<Paciente> findByCorreo(String correo);
    Optional<Paciente> findByCorreoAndContraseña(String correo, String contraseña);

}