# 🚀 Dynamic Island for Android

Una implementación fluida y funcional de la **Dynamic Island** para Android, construida con **Jetpack Compose** y arquitectura basada en servicios. Esta aplicación no solo replica la estética visual, sino que se integra profundamente con el sistema para manejar notificaciones reales y control de medios de aplicaciones externas como Spotify, YouTube y más.

---

## ✨ Características Principales

* **🎨 UI Adaptativa con Compose:** Interfaz construida íntegramente con Jetpack Compose, utilizando `animateContentSize` para transiciones orgánicas entre estados compacto y expandido.
* **🎵 Control de Medios en Tiempo Real:** Integración con `MediaSession` para obtener metadatos (título, artista) y carátulas de álbumes directamente desde el controlador de medios del sistema.
* **📡 Sistema de Escucha de Notificaciones:** Uso de `NotificationListenerService` para capturar eventos de aplicaciones de terceros sin latencia.
* **🔘 Controles Interactivos:** Botones funcionales para Reproducir/Pausar, Siguiente y Anterior que envían comandos de vuelta a la aplicación de música activa.
* **🖼️ Superposición de Sistema (Overlay):** Implementación técnica mediante `WindowManager` para mostrar la isla sobre cualquier aplicación, respetando el área de la cámara (notch).

---

## 🛠️ Stack Tecnológico

* **Lenguaje:** [Kotlin](https://kotlinlang.org/)
* **UI:** [Jetpack Compose](https://developer.android.com/jetpack/compose) (Material 3)
* **Servicios:** Foreground Services, NotificationListenerService
* **Media:** MediaController & MediaSession API

---

## ⚙️ Arquitectura y Flujo de Datos

El proyecto utiliza un sistema de comunicación desacoplado mediante **Broadcast Receivers**:



1.  **NotificationReceiver:** Escucha las notificaciones del sistema, extrae el `MediaSession.Token` y envía los metadatos (título, artista, bitmap) a la interfaz.
2.  **IslandService:** Gestiona la ventana flotante (`TYPE_APPLICATION_OVERLAY`) y mantiene el ciclo de vida de la aplicación en primer plano para evitar que el sistema la cierre.
3.  **UIPill (Compose):** Capa de presentación reactiva que cambia su estado visual según la información recibida.

---

## ⚠️ Configuración y Permisos

Para un funcionamiento óptimo, se deben otorgar los siguientes permisos en el dispositivo:

1.  **Aparecer encima (Overlay):** Necesario para dibujar la isla sobre otras apps.
2.  **Acceso a Notificaciones:** Necesario para que el `NotificationListenerService` pueda leer los datos de música.
3.  **Optimización de Batería:** Configurar la app como **"Sin restricciones"** (especialmente en dispositivos Xiaomi/Samsung) para evitar cierres del servicio.

---

## 🚀 Instalación

1. Clona el repositorio:
   ```bash
   https://github.com/zara-te05/DynamicIsland.git
