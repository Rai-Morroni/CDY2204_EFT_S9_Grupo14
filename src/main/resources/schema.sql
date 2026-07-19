-- Script de creación de tabla para la Gestión de Guías de Despacho
-- Base de Datos: Oracle Autonomous Database

CREATE TABLE guias_despacho (
    id VARCHAR2(36) PRIMARY KEY,
    numero_pedido VARCHAR2(50) NOT NULL,
    transportista VARCHAR2(100) NOT NULL,
    fecha DATE NOT NULL,
    destinatario VARCHAR2(150) NOT NULL,
    direccion_entrega VARCHAR2(255) NOT NULL,
    detalle_pedido VARCHAR2(1000),
    estado VARCHAR2(30) DEFAULT 'EN_COLA',
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Comentario opcional para verificar la inserción inicial
-- SELECT * FROM guias_despacho;