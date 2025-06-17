package com.clinica.sistema.Repositorio;

import com.clinica.sistema.Modelo.Especialidad;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EspecialidadRepositorio extends JpaRepository<Especialidad, Long> {
    Optional<Especialidad> findByNombre(String nombre);

}