# Lecciones Aprendidas

Documento vivo para evitar repetir errores operativos y de desarrollo en `sync_ad_maximo`.

## 1. No usar rutas de ejemplo como si fueran reales

- `C:\ruta\a\apache-tomcat-9` era un ejemplo, no una instalación válida.
- Antes de automatizar despliegues, confirmar la ruta real de Tomcat o usar variables como `CATALINA_HOME`.
- Los scripts deben fallar con un mensaje claro si la ruta no existe.

## 2. Validar la versión real de Java que usa Maven

- El proyecto compila con Java 11+, pero el sistema tenía Maven ejecutándose con Java 8 por defecto.
- No asumir que `java -version` y `mvn -version` usan el mismo runtime.
- Antes de probar o empaquetar, verificar el JDK que ejecuta Maven.

## 3. Preferir scripts que detecten el entorno

- El helper de Maven debe buscar un JDK moderno automáticamente.
- Si existe `JAVA_HOME`, no debe ser aceptado a ciegas si apunta a una versión incompatible.
- Para Tomcat, el script debe aceptar `TOMCAT9_HOME`, `TOMCAT_HOME` o `CATALINA_HOME`.

## 4. No abrir la interfaz antes de que el servidor responda

- Lanzar el navegador demasiado pronto genera falsos errores de "no se puede acceder a la página".
- El despliegue debe esperar una respuesta HTTP real antes de abrir la URL.
- Si el servidor no responde, el script debe decir dónde revisar logs.

## 5. Usar el grafo antes de leer todo el repositorio

- Para preguntas de arquitectura o relaciones de código, usar `graphify query`, `graphify path` o `graphify explain` primero.
- Evitar barridos amplios de archivos cuando el grafo ya puede acotar la respuesta.
- Esto ahorra tiempo y tokens.

## 6. Actualizar el grafo después de cambios

- Si se cambia código, documentación o estructura relevante, ejecutar `graphify update .`.
- No considerar archivos `graphify-out/` sucios como motivo para saltarse la actualización.
- Mantener el grafo alineado evita diagnósticos viejos o inconsistentes.

## 7. Basarse en requerimientos y en lo ya implementado

- Antes de programar algo nuevo, comparar:
  - lo solicitado
  - lo que dicen los requerimientos
  - lo que ya existe en el código
- Reusar módulos existentes siempre que sea posible.
- No crear estructuras paralelas si el proyecto ya tiene una base funcional.

## 8. Documentar las rutas operativas reales

- Deben quedar registradas las rutas efectivas de despliegue, build y acceso web.
- También conviene documentar qué variables de entorno son aceptadas por los scripts.
- Las instrucciones deben ser ejecutables, no solo descriptivas.

## 9. Confirmar el estado con pruebas reales

- No dar por válido un flujo solo porque "debería funcionar".
- Ejecutar `mvn test` y `mvn package` con el JDK correcto.
- Si algo falla, distinguir entre problema del código y problema del entorno.

## 10. Mantener mensajes de error claros

- Si falta una ruta, una variable o un servidor, el script debe decir exactamente qué falta.
- Evitar errores genéricos que obliguen a adivinar.
- Los mensajes claros ahorran tiempo en depuración.

