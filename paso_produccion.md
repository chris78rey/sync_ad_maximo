# Paso a producción de `sync_ad_maximo`

Este documento resume los pasos recomendados para pasar la aplicación a producción de forma segura y ordenada.

## 1) Confirmar el alcance de producción

Antes de desplegar:

- La aplicación debe estar compilada como `WAR`.
- Debe ejecutarse sobre un contenedor Servlet compatible con `javax.servlet-api 4.0.1`.
- La versión objetivo del proyecto es Java 11 (`maven.compiler.release=11`).
- El usuario web por defecto `maxadmin` no debe quedar como acceso real de producción.
- El modo demo debe quedar deshabilitado, restringido o aislado del entorno productivo.

## 2) Preparar el entorno destino

Verifica que el servidor de producción cumpla con esto:

- Java 11 instalado y configurado.
- Tomcat 9.x o un contenedor equivalente compatible con Servlet 4.0.
- Conectividad hacia:
  - Active Directory / LDAP
  - Oracle / MAXIMO
  - SMTP corporativo
- Zona horaria configurada para Ecuador:
  - `America/Guayaquil`
- Codificación UTF-8 en consola, navegador y base de datos.

## 3) Crear la configuración externa

La aplicación carga configuración externa desde la variable de entorno:

- `SYNC_AD_MAXIMO_CONFIG`

Si esa variable está definida, la app lee el archivo externo indicado.
Si no está definida, usa `src/main/resources/application.properties` del classpath.

### Recomendación

En producción, usar un archivo externo, por ejemplo:

- Windows: `C:\opt\sync_ad_maximo\config\application.properties`
- Linux: `/opt/sync_ad_maximo/config/application.properties`

### Valores mínimos a revisar

En ese archivo ajusta, como mínimo:

- `app.name`
- `app.timezone=America/Guayaquil`
- `app.locale=es-EC`
- `sync.strategy.cedula9Digitos`
- `sync.web.allowedUser`
- parámetros de AD/LDAP
- parámetros de MAXIMO/Oracle
- parámetros SMTP

### Importante

No dejar en producción un valor de prueba como:

- `sync.web.allowedUser=maxadmin`

Ese valor solo sirve para pruebas o modo controlado.

## 4) Conectar autenticación real

La capa web intenta resolver el autenticador así:

1. Busca un objeto `webAuthenticator` en el `ServletContext`.
2. Si no existe, usa `DirectoryBackedWebAuthenticator`.
3. Ese autenticador depende de `DirectoryService`.

### En producción debes asegurar una de estas dos opciones:

- Opción A: inyectar un `DirectoryService` real en el `ServletContext`.
- Opción B: registrar un `webAuthenticator` real que valide contra AD.

### No dejar solo el fallback actual

Si no se inyecta `DirectoryService`, el autenticador puede quedar en un modo simplificado que no representa seguridad real.

## 5) Preparar Oracle / MAXIMO

Antes de pasar a producción:

- Ejecutar los scripts SQL de `src/main/resources/sql/`.
- Crear tablas de auditoría, secuencias e índices.
- Validar permisos del usuario Oracle que usará la app.
- Probar lectura/escritura contra las tablas de MAXIMO.
- Confirmar esquema correcto:
  - `maximo.personSchema=MAXIMO`
  - `maximo.emailSchema=MAXIMO`

### Recomendación operativa

Hacer primero una carga en ambiente de prueba con datos representativos, y solo después aplicar en producción.

## 6) Preparar SMTP

Verifica la configuración de correo para que el resumen de ejecución llegue correctamente.

Debes validar:

- host SMTP
- puerto
- usuario/clave si aplica
- TLS/SSL si aplica
- dirección remitente
- destinatarios reales de operación

### Prueba mínima

Antes del corte final, confirmar que:

- el resumen de una ejecución llega por correo
- el contenido respeta UTF-8
- el correo refleja el mismo resultado que el reporte web

## 7) Construir el artefacto

Genera el WAR final de producción.

### Opción recomendada con Maven

```bash
mvn clean package
```

El artefacto esperado es algo como:

- `target/sync-ad-maximo.war`

### Validaciones previas al build

- `mvn clean test` o, si no hay Maven disponible en ese entorno, al menos compilar y ejecutar los tests mock locales.
- Verificar que el WAR no incluya secretos.
- Verificar que el build conserve UTF-8.

## 8) Desplegar en el contenedor

Despliega el WAR en Tomcat 9.

Pasos sugeridos:

1. Detener la aplicación anterior.
2. Respaldar el WAR y la configuración vigente.
3. Copiar el nuevo `sync-ad-maximo.war` a `webapps/`.
4. Asegurar que la variable `SYNC_AD_MAXIMO_CONFIG` esté definida en el servicio.
5. Levantar Tomcat.
6. Revisar los logs de arranque.

### Verificación de arranque

Debes ver en logs:

- despliegue exitoso del WAR
- ausencia de errores de clases o recursos
- ausencia de problemas de codificación

## 9) Hacer smoke tests post-despliegue

Después de desplegar, probar al menos estas URLs:

- `/sync-ad-maximo/health`
- `/sync-ad-maximo/login`
- `/sync-ad-maximo/report`
- `/sync-ad-maximo/report.csv`
- `/sync-ad-maximo/mail/preview`

### Qué validar en cada una

#### `/health`
- Respuesta HTTP 200
- JSON con `status=UP`
- Fecha/hora correcta

#### `/login`
- Renderiza bien el formulario
- Texto en español correcto
- No muestra caracteres rotos como `cÃ³digo`

#### `/report`
- Muestra resumen de ejecución
- Renderiza tabla de issues
- Enlaces a CSV y correo funcionan

#### `/report.csv`
- Descarga CSV válido
- El archivo respeta UTF-8

#### `/mail/preview`
- El texto del correo coincide con el reporte
- El resumen es legible y completo

## 10) Confirmar el flujo real de negocio

En producción no basta con abrir la web; hay que validar el flujo de negocio completo:

- autenticación web
- lectura de AD
- validación de datos
- interacción con MAXIMO / Oracle
- auditoría de ejecución
- correo de notificación

### Escenarios mínimos

- usuario válido
- usuario no autorizado
- usuario duplicado o con conflicto
- corrida sin cambios
- corrida con cambios reales
- error de conexión a AD
- error de conexión a Oracle
- error de SMTP

## 11) Seguridad mínima antes de salir

Revisa estos puntos antes de ponerlo en vivo:

- cambiar credenciales de prueba
- restringir acceso a red interna/VPN
- no exponer el demo público en producción
- revisar logs para que no impriman secretos
- limitar permisos de Oracle al mínimo necesario
- usar TLS donde corresponda
- proteger backups y archivos de configuración

## 12) Plan de rollback

Debe existir un plan de reversión claro.

### Mantener a mano

- WAR anterior
- archivo de configuración anterior
- scripts SQL aplicados
- usuario/clave de servicio anteriores

### Si algo falla

1. Detener Tomcat.
2. Restaurar el WAR anterior.
3. Restaurar la configuración anterior.
4. Reiniciar Tomcat.
5. Confirmar `/health`.

## 13) Recomendación final de operación

No pasar a producción hasta confirmar todo esto:

- autenticación real activa
- configuración externa apuntando al entorno correcto
- Oracle/MAXIMO probados
- SMTP probado
- UTF-8 correcto en UI y CSV
- demo deshabilitado o aislado
- smoke tests completos exitosos

## Checklist rápido

- [ ] Java 11 listo
- [ ] Tomcat 9 listo
- [ ] `SYNC_AD_MAXIMO_CONFIG` definido
- [ ] `sync.web.allowedUser` no es un valor de prueba
- [ ] AD/LDAP conectado
- [ ] Oracle/MAXIMO conectados
- [ ] SMTP probado
- [ ] WAR compilado y desplegado
- [ ] `/health` responde 200
- [ ] `/login` funciona
- [ ] `/report` muestra datos correctos
- [ ] `/report.csv` descarga bien
- [ ] `/mail/preview` coincide con el reporte
- [ ] Demo deshabilitado o restringido
- [ ] Rollback preparado

## Nota

Este documento describe el camino recomendado para producción con el código actual del proyecto. Si se cambia la estrategia de autenticación, despliegue o configuración externa, conviene actualizar estos pasos.
