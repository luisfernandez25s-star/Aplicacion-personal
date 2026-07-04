# Monitor de Sensores - Wear OS & Android

Este proyecto consiste en un ecosistema de dos aplicaciones para el monitoreo de salud en tiempo real.

## Contenido del Proyecto

1.  **Módulo App (Celular)**: Recibe los datos del reloj, los muestra en una interfaz espejo y permite la subida manual a MongoDB Atlas a través de una API en Render.
2.  **Módulo Wear (Reloj)**: Captura datos de los sensores de ritmo cardíaco, acelerómetro y giroscopio. Incluye un sistema de permisos obligatorio y modo de simulación.

## Instalación (APKs)

Para facilitar la revisión, los instaladores se encuentran en la carpeta `release_apks/`:
*   `app-celular.apk`: Instalar en el teléfono móvil.
*   `wear-reloj.apk`: Instalar en el Smartwatch (Wear OS).

## Funcionalidades
*   **Sincronización Manual**: Se envía un único registro a la base de datos al pulsar el botón para evitar duplicidad de datos.
*   **Interfaz Espejo**: El celular muestra exactamente lo mismo que el reloj con emojis y datos en tiempo real.
*   **Permisos Profesionales**: La aplicación solicita acceso a sensores corporales y actividad física siguiendo las mejores prácticas de Android.

## Base de Datos
Los datos se almacenan en MongoDB Atlas y pueden ser visualizados a través del panel de control configurado en la URL de Render.
