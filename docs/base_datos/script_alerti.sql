-- =========================================================
-- SCRIPT DE CREACIÓN DE LA BASE DE DATOS ALERTI
-- =========================================================

-- =========================================================
-- TABLA: CONTACTOS
-- Almacena los contactos de emergencia del usuario.
-- =========================================================

CREATE TABLE IF NOT EXISTS contactos (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nombre TEXT NOT NULL,
    numero TEXT NOT NULL,
    prioridad INTEGER NOT NULL
);

-- =========================================================
-- TABLA: EVENTOS_ALERTA
-- Almacena los eventos de caída detectados por ALERTI.
-- =========================================================

CREATE TABLE IF NOT EXISTS eventos_alerta (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    fechaHoraMillis INTEGER NOT NULL,
    latitud REAL,
    longitud REAL,
    magnitudPico REAL NOT NULL,
    estado TEXT NOT NULL,
    contactosNotificados INTEGER NOT NULL
);

-- =========================================================
-- DATOS DE PRUEBA: CONTACTOS
-- =========================================================

INSERT INTO contactos (nombre, numero, prioridad)
VALUES ('Contacto de prueba', '3103375555', 1);

INSERT INTO contactos (nombre, numero, prioridad)
VALUES ('Familiar de prueba', '3201234567', 2);

INSERT INTO contactos (nombre, numero, prioridad)
VALUES ('Contacto secundario', '3009876543', 3);

-- =========================================================
-- DATOS DE PRUEBA: EVENTOS
-- =========================================================

INSERT INTO eventos_alerta (
    fechaHoraMillis,
    latitud,
    longitud,
    magnitudPico,
    estado,
    contactosNotificados
)
VALUES (
    1758200000000,
    5.0676,
    -73.8547,
    23.5,
    'CONFIRMADO',
    3
);

INSERT INTO eventos_alerta (
    fechaHoraMillis,
    latitud,
    longitud,
    magnitudPico,
    estado,
    contactosNotificados
)
VALUES (
    1758203600000,
    NULL,
    NULL,
    22.1,
    'CANCELADO',
    0
);