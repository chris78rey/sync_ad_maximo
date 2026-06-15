# Notas para continuar

Estado actual al cerrar la sesion:

- La aplicacion ya corre como WAR en Tomcat 9.
- Existe un dashboard unico en `/` con accesos a reporte, historial, CSV, health, demo, vista de correo y logout.
- El historial de ejecuciones ya se puede listar desde Oracle, abrir por `runId` y exportar a CSV.
- El scheduler ya respeta `horaEjecucion` cuando esta configurada.
- El reporte principal ya enlaza al historial.
- El grafo de `graphify` fue actualizado con el estado actual del codigo.

Siguiente paso recomendado para mañana:

1. Revisar los pendientes de arquitectura que faltan contra el documento original.
2. Decidir si el siguiente foco es estructura del proyecto, pruebas formales o cierre de capas restantes.
3. Si se va a seguir con UI, agregar una vista de dashboard con indicadores reales.

Comandos utiles para retomar:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\mvn-jdk.ps1 test
powershell -ExecutionPolicy Bypass -File .\scripts\mvn-jdk.ps1 package
$env:JAVA_HOME='C:\Program Files\Java\jdk-24'
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
$deps = Get-Content target/test-classpath.txt
$cp = @('target/classes','target/test-classes') + ($deps -split ';')
java -cp ($cp -join ';') com.syncadmaximo.MockTestRunner
```

Rutas utiles:

- `/`
- `/report`
- `/api/reportes/historial`
- `/api/reportes/historial.csv`
- `/health`
- `/demo`
- `/mail/preview`

