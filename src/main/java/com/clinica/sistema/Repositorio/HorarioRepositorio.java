package com.clinica.sistema.Repositorio;

import com.clinica.sistema.Modelo.Horario;
import com.clinica.sistema.Modelo.Medico; // Necesario para el método findByMedico
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate; // Necesario para la fecha
import java.time.LocalTime; // Necesario para la hora
import java.util.List; // Necesario para los métodos que devuelven listas
import java.util.Optional;

@Repository
public interface HorarioRepositorio extends JpaRepository<Horario, Long> {
    Optional<Horario> findByFechaAndHoraAndMedico(LocalDate fecha, LocalTime hora, Medico medico);

    List<Horario> findByMedicoAndFechaAndDisponibleTrue(Medico medico, LocalDate fecha);

    Optional<Horario> findByMedicoAndFechaAndHora(Medico medico, LocalDate fecha, LocalTime hora);

}