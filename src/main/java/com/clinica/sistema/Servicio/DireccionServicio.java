// src/main/java/com/clinica/sistema/Servicio/DireccionServicio.java
package com.clinica.sistema.Servicio;

import com.clinica.sistema.Modelo.Direccion;
import com.clinica.sistema.Repositorio.DireccionRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Importar Transactional

import java.util.List;
import java.util.Optional;

@Service
public class DireccionServicio {

    private final DireccionRepositorio direccionRepositorio;

    public DireccionServicio(DireccionRepositorio direccionRepositorio) {
        this.direccionRepositorio = direccionRepositorio;
    }

    // Método para obtener todas las direcciones de un paciente
    public List<Direccion> obtenerDireccionesPorPaciente(Long pacienteId) {
        return direccionRepositorio.findByPacienteId(pacienteId);
    }

    // Método para guardar una nueva dirección
    @Transactional // Importante para operaciones de escritura en BD
    public Direccion guardarDireccion(Direccion direccion) {
        return direccionRepositorio.save(direccion);
    }

    // Método para buscar una dirección por ID y asegurarse de que pertenece a un paciente específico
    public Optional<Direccion> buscarPorIdYPacienteId(Long id, Long pacienteId) {
        return direccionRepositorio.findByIdAndPacienteId(id, pacienteId);
    }
}