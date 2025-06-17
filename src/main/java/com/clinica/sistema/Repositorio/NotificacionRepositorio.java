package com.clinica.sistema.Repositorio;

import com.clinica.sistema.Modelo.Notificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificacionRepositorio extends JpaRepository<Notificacion, Long> {

}