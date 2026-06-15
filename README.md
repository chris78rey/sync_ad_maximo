# sync-ad-maximo

Aplicación Java/Maven para sincronizar usuarios y correos entre Active Directory y Oracle MAXIMO.

## Qué hace

- Detecta cambios de `PERSONID` en AD y migra el login en MAXIMO.
- Crea usuarios nuevos cuando el usuario existe en AD y no en MAXIMO.
- Inactiva usuarios cuando AD los reporta deshabilitados.
- Sincroniza el correo primario de MAXIMO contra `AD.mail`.
- Registra auditoría de ejecución, eventos de acceso y correos enviados.
- Expone interfaz web para consultar reportes y exportar CSV.

## Arquitectura

- `orchestration`: coordinación del proceso principal.
- `service`: servicios de creación, correo, validación, mail, auditoría.
- `repository`: acceso a Oracle.
- `ldap`: cliente Active Directory.
- `web`: filtros, login, reportes y health.
- `scheduler`: ejecución programada o manual.

## Requisitos

- Java 11 o superior.
- Maven 3.9+.
- Tomcat 9.
- Acceso a Oracle y LDAP cuando se pruebe en modo real.

## Compilación

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\mvn-jdk.ps1 package
```

Resultado esperado:

```text
target/sync-ad-maximo-0.1.0-SNAPSHOT.war
```

## Pruebas locales

El proyecto incluye runner manual de mocks:

```powershell
$env:JAVA_HOME="C:\Program Files\Java\jdk-24"
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
mvn -q -DincludeScope=test dependency:build-classpath -Dmdep.outputFile=target/test-classpath.txt
$deps = Get-Content target/test-classpath.txt
$cp = @('target/classes','target/test-classes') + ($deps -split ';')
java -cp ($cp -join ';') com.syncadmaximo.MockTestRunner
```

## Despliegue en Tomcat 9

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\deploy-tomcat.ps1
```

El script usa `TOMCAT9_HOME`, `TOMCAT_HOME` o `CATALINA_HOME`.

## Configuración externa

Define `SYNC_AD_MAXIMO_CONFIG` apuntando a un archivo `.properties` o `.yml/.yaml`.

### Windows

```powershell
$env:SYNC_AD_MAXIMO_CONFIG="D:\config\sync-ad-maximo.yml"
```

### Linux con `setenv.sh`

```bash
export SYNC_AD_MAXIMO_CONFIG=/opt/sync-ad-maximo/config/application.yml
```

### Linux con `systemd`

```ini
Environment=SYNC_AD_MAXIMO_CONFIG=/opt/sync-ad-maximo/config/application.yml
```

## Configuración Oracle

Propiedades principales:

- `oracle.url`
- `oracle.username`
- `oracle.password`
- `oracle.schema`
- `oracle.runSequenceName`
- `oracle.auditSequenceName`
- `oracle.accessAuditSequenceName`
- `oracle.mailAuditSequenceName`
- `oracle.emailIdSequenceName`
- `oracle.rowstampSequenceName`

## Configuración LDAP

Propiedades principales:

- `ldap.url`
- `ldap.bindUser`
- `ldap.bindPassword`
- `ldap.baseDn`
- `ldap.enabledFilter`
- `ldap.disabledFilter`

## Configuración SMTP

Propiedades principales:

- `mail.enabled`
- `mail.host`
- `mail.port`
- `mail.username`
- `mail.password`
- `mail.starttls`
- `mail.from`
- `mail.subjectPrefix`

## Scheduler

Propiedades principales:

- `sync.scheduler.enabled`
- `sync.scheduler.horaEjecucion`
- `sync.scheduler.fixedDelaySeconds`
- `sync.scheduler.initialDelaySeconds`
- `sync.execution.mode`

## Acceso web

- Usuario autorizado: `maxadmin`
- Login: `/login`
- Reporte: `/report`
- CSV: `/report.csv`
- Health: `/health`

## Scripts SQL

Los scripts operativos están en `scripts/`:

- `crear_tablas_auditoria.sql`
- `crear_secuencias_auditoria.sql`
- `crear_indices.sql`

Las versiones fuente equivalentes están en `src/main/resources/sql/`.

## Ajuste de `EMAILID` y `ROWSTAMP`

La inserción de correos usa secuencias configurables.
Si la instancia de MAXIMO usa nombres distintos, ajusta:

- `oracle.emailIdSequenceName`
- `oracle.rowstampSequenceName`

## Lecciones operativas

- No usar rutas de ejemplo como si fueran reales.
- Verificar el JDK que ejecuta Maven.
- Esperar a que Tomcat responda antes de abrir el navegador.
- Actualizar `graphify` después de cambios relevantes.

