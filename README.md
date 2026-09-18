# ALERTI

## Descripción

ALERTI es una aplicación móvil desarrollada para Android que permite detectar posibles caídas mediante los sensores del dispositivo. Cuando se detecta una posible caída, la aplicación inicia un protocolo de verificación y, si el usuario no responde, envía una alerta por SMS a los contactos de emergencia registrados.

## Objetivo

Desarrollar una aplicación móvil capaz de detectar posibles accidentes o caídas y generar alertas automáticas a contactos de emergencia, utilizando sensores, ubicación GPS y mensajes SMS.

## Tecnologías utilizadas

- Kotlin
- Android Studio
- Android SDK
- Room Database
- SQLite
- Sensores de acelerómetro y giroscopio
- GPS / ubicación
- SMS
- Coroutines

## Funcionalidades

- Registro de contactos de emergencia.
- Consulta de contactos registrados.
- Modificación de contactos.
- Eliminación de contactos.
- Asignación de prioridad a los contactos.
- Detección de posibles caídas.
- Verificación de inmovilidad.
- Confirmación o cancelación de una alerta.
- Obtención de ubicación.
- Envío de mensajes SMS a contactos de emergencia.
- Registro de eventos de alerta.
- Consulta del historial de eventos.

## Base de datos

ALERTI utiliza Room sobre SQLite.

La base de datos contiene las siguientes tablas:

### contactos

Almacena los contactos de emergencia.

- id
- nombre
- numero
- prioridad

### eventos_alerta

Almacena los eventos detectados por la aplicación.

- id
- fechaHoraMillis
- latitud
- longitud
- magnitudPico
- estado
- contactosNotificados

La estructura utiliza atributos atómicos y cumple con la Primera Forma Normal (1FN).

El script de creación y los datos de prueba se encuentran en:

`docs/base_datos/script_alerti.sql`

## Arquitectura

Actualmente el proyecto utiliza una separación entre la interfaz de usuario y la capa de persistencia mediante Room.

La persistencia se implementa mediante:

`MainActivity → DAO → Room/SQLite`

La arquitectura será mejorada posteriormente mediante una capa Repository y ViewModel.

## Requisitos

- Android Studio.
- JDK compatible con la versión del proyecto.
- Android SDK.
- Dispositivo Android físico o emulador.
- Permisos de ubicación y SMS para las funciones correspondientes.

## Instalación

1. Clonar el repositorio.
2. Abrir el proyecto en Android Studio.
3. Esperar la sincronización de Gradle.
4. Conectar un dispositivo Android o iniciar un emulador.
5. Ejecutar la aplicación.

## Base de datos de prueba

La aplicación puede crear un contacto de prueba cuando la base de datos se encuentra vacía.

También se incluyen datos de prueba en el script SQL ubicado en:

`docs/base_datos/script_alerti.sql`

## Estructura del proyecto

```text
ALERTI/
├── app/
├── docs/
│   └── base_datos/
│       ├── modelo_fisico.png
│       └── script_alerti.sql
├── README.md
└── ...