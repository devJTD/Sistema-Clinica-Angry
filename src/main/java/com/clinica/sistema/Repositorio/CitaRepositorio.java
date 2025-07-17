package com.clinica.sistema.Repositorio;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.clinica.sistema.Modelo.Cita;
import com.clinica.sistema.Modelo.Paciente;

@Repository
public interface CitaRepositorio extends JpaRepository<Cita, Long> {
    List<Cita> findByPacienteAndEstado(Paciente paciente, String estado);
    List<Cita> findByPacienteAndEstadoNot(Paciente paciente, String estado);
    List<Cita> findByPaciente(Paciente paciente);
}