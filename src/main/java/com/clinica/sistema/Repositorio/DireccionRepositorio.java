package com.clinica.sistema.Repositorio;

import com.clinica.sistema.Modelo.Direccion;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DireccionRepositorio extends JpaRepository<Direccion, Long> {
    List<Direccion> findByPacienteId(Long pacienteId);
    Optional<Direccion> findByIdAndPacienteId(Long id, Long pacienteId);
}
