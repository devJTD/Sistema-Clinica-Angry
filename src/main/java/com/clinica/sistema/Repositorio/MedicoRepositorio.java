package com.clinica.sistema.Repositorio;

import com.clinica.sistema.Modelo.Especialidad;
import com.clinica.sistema.Modelo.Medico;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MedicoRepositorio extends JpaRepository<Medico, Long> {
    List<Medico> findByEspecialidad(Especialidad especialidad);
    Optional<Medico> findByNombreAndApellido(String nombre, String apellido);

}