# 🔒 Bloqueador de Toques (Touch Blocker)

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-purple.svg)](https://kotlinlang.org)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

Una aplicación nativa para Android diseñada para evitar **toques accidentales en la pantalla** mediante la superposición de una capa invisible de intercepción de eventos de entrada. Es ideal para ver videos, mostrar contenido a terceros o guardar el dispositivo en el bolsillo sin realizar pulsaciones involuntarias.

---

## 📱 ¿Cómo funciona?

La aplicación opera principalmente mediante una **notificación interactiva e inmersiva**:

1. **Configuración rápida:** El usuario concede los permisos necesarios en la interfaz principal.
2. **Activación desde Notificación:** Al presionar la notificación fija en la barra de estado, se activa un servicio en primer plano (*Foreground Service*).
3. **Capa Invisible:** Se añade una vista transparente de nivel del sistema que cubre la totalidad de la pantalla y absorbe todos los gestos y clics.
4. **Desactivación:** Un segundo toque en la notificación remueve la vista y restablece el comportamiento normal del dispositivo.

---

## 🛠️ Características Principales

* **Bloqueo con un toque:** Interacción rápida desde la cortina de notificaciones.
* **Capa invisible de superposición:** Hace uso de `WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY` para prevenir toques sin alterar la visualización del contenido.
* **Modo inmersivo:** Oculta las barras del sistema (navegación y estado) durante el bloqueo.
* **Persistencia de permisos:** Utiliza almacenamiento interno en texto plano para verificar el estado de los permisos de forma eficiente.
* **Compatibilidad moderna:** Soportado en versiones de Android 8.0 hasta Android 14+ (`UPSIDE_DOWN_CAKE`), cumpliendo con las especificaciones de permisos para servicios en primer plano.

---

## 🏗️ Arquitectura y Componentes

El proyecto consta de dos componentes principales:

### 1. `MainActivity.kt` (Pantalla de Configuración)
* Gestiona la solicitud de permisos dinámicos (`POST_NOTIFICATIONS` y `SYSTEM_ALERT_WINDOW`).
* Controla los interruptores (*Switches*) de la interfaz de usuario (`activity_main.xml`).
* Registra el estado de los permisos en archivos locales (`PermisoNotificacion.txt` y `PermisoSuperposicion.txt`).
* Inicializa el canal de notificaciones con prioridad `IMPORTANCE_LOW` para evitar avisos sonoros molestos.

### 2. `BloqueadorService.kt` (Servicio en Segundo Plano)
* Implementado como un `Foreground Service` para garantizar su ejecución ininterrumpida por el sistema operativo.
* Genera la vista transparente (`Color.TRANSPARENT`) agregada mediante el `WindowManager`.
* Aplica `SYSTEM_UI_FLAG_IMMERSIVE_STICKY` para bloquear gestos en los bordes de la pantalla.
* Actualiza dinámicamente la notificación cambiando títulos e íconos según el estado activo/inactivo.

---

## 🔄 Flujo de Ejecución

```
[Usuario abre la App]
         │
         ▼
[Otorga Permisos en Pantalla] ───► Se guardan banderas de estado (TXT)
         │
         ▼
[Aparece Notificación Inicial]
         │
         ▼  (Clic en la notificación)
[Se inicia BloqueadorService] ───► Se genera la capa transparente por encima
         │
         ▼  (Clic para desactivar)
[Se destruye el Servicio]     ───► Se remueve la capa y se restaura el control
```

---

## 📋 Requisitos del Sistema

* **Lenguaje:** Kotlin
* **SDK Mínimo:** API 21 (Android 5.0 Lollipop)
* **SDK Objetivo:** API 34 (Android 14)
* **Herramienta de construcción:** Gradle

---

## 🔑 Permisos Requeridos

En el archivo `AndroidManifest.xml` deben declararse los siguientes permisos:

```xml
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />
```

---
