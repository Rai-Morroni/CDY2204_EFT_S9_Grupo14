package com.transportes.guiadespacho.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repositorio JPA para la tabla GUIAS_PROCESADAS en Oracle Cloud.
 * Spring Data JPA genera automaticamente la implementacion en tiempo de ejecucion.
 */
@Repository
public interface GuiaProcesadaRepository extends JpaRepository<GuiaProcesadaEntity, String> {

    /**
     * Consulta guias procesadas por transportista (busqueda exacta,
     * insensible a mayusculas/minusculas).
     */
    List<GuiaProcesadaEntity> findByTransportistaIgnoreCase(String transportista);

    /**
     * Consulta guias procesadas por fecha exacta.
     */
    List<GuiaProcesadaEntity> findByFecha(LocalDate fecha);

    /**
     * Consulta guias procesadas por transportista Y fecha (combinada).
     */
    List<GuiaProcesadaEntity> findByTransportistaIgnoreCaseAndFecha(String transportista, LocalDate fecha);
}
