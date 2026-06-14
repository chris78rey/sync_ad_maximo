Actúa como un arquitecto de software y programador senior experto en Java, Oracle, MAXIMO, Active Directory, LDAP, Apache Tomcat 9, JDBC, auditoría, seguridad, procesos batch, integración empresarial y aplicaciones web en producción.

Se requiere crear un proyecto llamado `sync_ad_maximo` para sincronizar diariamente información entre Active Directory y Oracle MAXIMO.

La solución debe ser una aplicación Java Web empaquetable como WAR y desplegable en Apache Tomcat 9.

El código debe ser modular, claro, mantenible, comentado en las partes críticas y listo para abrir, compilar y ejecutar desde Visual Studio Code.

La aplicación debe ser compatible con Windows para desarrollo y Linux para producción.

# 1. Objetivo general

Crear una aplicación que sincronice usuarios y correos electrónicos entre Active Directory y Oracle MAXIMO, usando la cédula como llave principal de comparación.

El proceso debe permitir:

1. Detectar usuarios de MAXIMO cuyo login cambió en Active Directory.
2. Ejecutar un procedimiento almacenado para migrar usuarios.
3. Crear usuarios nuevos en MAXIMO cuando existan activos en Active Directory y no existan en MAXIMO.
4. Inactivar usuarios en MAXIMO cuando estén deshabilitados en Active Directory, siempre que no existan restricciones funcionales.
5. Sincronizar correos electrónicos de forma independiente al usuario.
6. Detectar duplicados, usuarios reutilizados, cédulas inválidas y conflictos.
7. Registrar toda la auditoría en Oracle.
8. Consultar históricos desde una interfaz web.
9. Exportar reportes CSV bajo demanda desde la aplicación.
10. Enviar automáticamente por correo el reporte de auditoría de la ejecución diaria.
11. Permitir ingreso web solo mediante login contra Active Directory y únicamente para el usuario autorizado `maxadmin`.

# 2. Tecnología requerida

Usar preferentemente:

* Java.
* Maven.
* Apache Tomcat 9.
* Oracle JDBC Thin Driver.
* LDAP para Active Directory.
* JavaMail con paquetes `javax.mail`.
* Servlet API compatible con Tomcat 9.
* `javax.servlet.*`.
* No usar `jakarta.servlet.*`.

La solución debe generar:

target/sync-ad-maximo.war

El WAR debe desplegarse en Apache Tomcat 9.

No se debe asumir Tomcat 10 ni paquetes `jakarta`.

# 3. Contexto funcional

La empresa está migrando usuarios desde un directorio antiguo hacia un nuevo Active Directory.

En el nuevo Active Directory, el Logon Name puede cambiar.

Ejemplo:

Un usuario pudo tener inicialmente:

dmestanza

y luego ser creado o migrado como:

dfmestanza

MAXIMO puede conservar el usuario antiguo, generando problemas con Single Sign-On y con sistemas integrados.

También existen casos donde un usuario fue desactivado y creado nuevamente con otro login.

Además, puede existir reutilización de usuarios.

Ejemplo:

En MAXIMO:

aperez pertenece a Andrés Pérez

Pero en Active Directory:

aperez pertenece actualmente a Álvaro Pérez

Ese caso debe tratarse como conflicto crítico y no debe actualizarse automáticamente.

# 4. Base de datos Oracle MAXIMO

Tabla principal:

MAXIMO.PERSON

Campos relevantes:

* PERSONID: usuario o login en MAXIMO.
* STATUS: estado del usuario en MAXIMO.
* FIRSTNAME: nombres.
* LASTNAME: apellidos.
* EPP_CEDULA: número de cédula.
* EPP_NUM_ROL: número de rol.

Consulta base:

SELECT
PERSONID,
STATUS,
FIRSTNAME,
LASTNAME,
EPP_CEDULA,
EPP_NUM_ROL
FROM MAXIMO.PERSON
WHERE STATUS IN ('ACTIVO', 'INACTIVO');

Reglas:

* PERSONID debe compararse contra AD.sAMAccountName.
* EPP_CEDULA debe compararse contra AD.postalCode.
* STATUS = 'ACTIVO' representa usuario activo en MAXIMO.
* STATUS = 'INACTIVO' representa usuario inactivo en MAXIMO.

# 5. Tabla de correos en MAXIMO

Tabla:

MAXIMO.EMAIL

Campos relevantes:

* EMAILID: identificador del correo.
* PERSONID: usuario o login de MAXIMO.
* EMAILADDRESS: correo electrónico.
* TYPE: tipo de correo.
* ISPRIMARY: indica correo principal.
* ROWSTAMP: campo propio de MAXIMO.

Reglas:

* TYPE debe ser `TRABAJO`.
* ISPRIMARY debe ser `1`.
* PERSONID relaciona el correo con MAXIMO.PERSON.PERSONID.
* EMAILADDRESS debe compararse contra AD.mail.
* El correo no debe repetirse entre personas.
* Si un correo de AD ya está asignado a otro PERSONID en MAXIMO, no debe actualizarse ni insertarse.

Ejemplo de UPDATE:

UPDATE MAXIMO.EMAIL
SET EMAILADDRESS = ?
WHERE PERSONID = ?
AND ISPRIMARY = 1;

Ejemplo conceptual de INSERT:

INSERT INTO MAXIMO.EMAIL
(EMAILID, PERSONID, EMAILADDRESS, TYPE, ISPRIMARY, ROWSTAMP)
VALUES
(?, ?, ?, 'TRABAJO', 1, ?);

Importante:

No asumir definitivamente cómo se generan EMAILID y ROWSTAMP.

La solución debe dejar esto configurable.

Crear métodos documentados para obtener:

* siguiente EMAILID
* siguiente ROWSTAMP

Si existen secuencias Oracle, permitir configurarlas en el archivo externo.

Ejemplo:

maximo:
emailIdSequence: MAXIMO.EMAILSEQ
rowstampSequence: MAXIMO.ROWSTAMPSEQ

Si no se conocen las secuencias, dejar métodos placeholder bien documentados para adaptar a la realidad de MAXIMO.

# 6. Active Directory

Campos equivalentes:

* postalCode: cédula.
* sAMAccountName: Logon Name.
* mail: correo electrónico.
* givenName: nombres.
* sn: apellidos.
* displayName: nombre completo.
* userAccountControl: permite determinar si una cuenta está habilitada o deshabilitada.

Reglas:

* Para migración, creación y correo, considerar solo usuarios habilitados en AD.
* Para inactivación, considerar usuarios deshabilitados en AD.
* Excluir cuentas deshabilitadas usando userAccountControl cuando se consulten usuarios habilitados.
* Consultar usuarios deshabilitados cuando se ejecute el proceso de inactivación.
* No procesar usuarios sin sAMAccountName.
* No procesar usuarios sin cédula válida en postalCode.

# 7. Llave de comparación

La llave principal de comparación es la cédula.

Comparaciones correctas:

MAXIMO.PERSON.EPP_CEDULA = AD.postalCode

MAXIMO.PERSON.PERSONID = AD.sAMAccountName

MAXIMO.EMAIL.EMAILADDRESS = AD.mail

No comparar cédula contra usuario.

La cédula solo sirve para identificar a la misma persona entre MAXIMO y Active Directory.

# 8. Normalización de datos

Crear utilitarios para normalizar datos.

## Cédula

Reglas:

* Quitar espacios.
* Quitar guiones.
* Quitar caracteres no numéricos.
* Si tiene 10 dígitos, usarla.
* Si tiene 9 dígitos, permitir estrategia configurable:

  * AGREGAR_CERO
  * REPORTAR_NOVEDAD
* Si tiene menos de 9 o más de 10 dígitos, enviar a observados.
* No procesar cédulas vacías.

Configuración:

proceso:
estrategiaCedula9Digitos: AGREGAR_CERO

## Usuario

Reglas:

* Quitar espacios.
* Comparar en minúsculas.
* Mantener el valor real del AD al ejecutar procedimientos almacenados.
* No cambiar mayúsculas/minúsculas del dato original del AD al guardar o migrar.

## Correo

Reglas:

* Quitar espacios.
* Comparar en minúsculas.
* Validar formato básico de correo.
* No actualizar si AD.mail está vacío.
* No actualizar si AD.mail tiene formato inválido.
* No actualizar si el correo ya pertenece a otro PERSONID.

# 9. Arquitectura funcional

Diseñar un solo proceso orquestador diario.

No crear jobs aislados que no se comuniquen entre sí.

El orquestador debe ejecutar internamente los procesos en orden controlado:

1. Cargar configuración.
2. Crear registro de ejecución.
3. Leer datos de MAXIMO.
4. Leer datos de Active Directory.
5. Normalizar datos.
6. Validar duplicados y conflictos.
7. Procesar migración de usuarios.
8. Procesar creación de usuarios nuevos.
9. Procesar sincronización de correo.
10. Procesar inactivación de usuarios.
11. Registrar auditoría.
12. Actualizar totales de ejecución.
13. Enviar correo automático del reporte, si está habilitado.
14. Finalizar registro de ejecución.

La razón de un solo orquestador es evitar que un proceso cree, inactive o migre usuarios sin conocer el resultado de las validaciones generales.

# 10. Proceso 1: migración de usuarios

Objetivo:

Detectar usuarios que existen en MAXIMO con una cédula determinada, pero cuyo PERSONID es diferente al sAMAccountName activo del AD para esa misma cédula.

Condiciones para migrar:

* La cédula existe en MAXIMO.
* La cédula existe en AD.
* La cuenta de AD está habilitada.
* AD.sAMAccountName no está vacío.
* MAXIMO.PERSON.PERSONID es diferente a AD.sAMAccountName.
* La cédula no está duplicada en AD.
* No existe conflicto de usuario reutilizado.
* El nuevo PERSONID no existe en MAXIMO con otra cédula.
* No existe ambigüedad.

Acción:

Ejecutar procedimiento almacenado:

migrar_usuario(usuario_actual_maximo, usuario_correcto_ad)

Ejemplo:

migrar_usuario('jimenema', 'MOJimenez');

El primer parámetro es el usuario actual registrado en MAXIMO.

El segundo parámetro es el usuario correcto proveniente de Active Directory.

En modo dry-run:

* No ejecutar el procedimiento.
* Registrar qué procedimiento se habría ejecutado.
* Registrar estado DRY_RUN_USUARIO_MIGRARIA.

En modo producción:

* Ejecutar procedimiento dentro de transacción.
* Registrar auditoría.
* Hacer commit solo si el procedimiento y la auditoría fueron correctos.
* Hacer rollback si ocurre error.

# 11. Proceso 2: creación de usuarios nuevos

Objetivo:

Crear en MAXIMO usuarios habilitados en AD que no existan en MAXIMO.

Condiciones:

* Usuario habilitado en AD.
* postalCode válido.
* sAMAccountName válido.
* La cédula no existe en MAXIMO.
* El sAMAccountName no existe en MAXIMO con otra cédula.
* La cédula no está duplicada en AD.
* No existe conflicto de usuario reutilizado.

Acción:

Ejecutar procedimiento almacenado:

crear_usuario(...)

Como no se conocen todos los parámetros reales del procedimiento, crear un método configurable:

callCrearUsuario(AdUser adUser)

Este método debe estar claramente documentado para completar los parámetros reales.

Debe considerar datos como:

* sAMAccountName
* cédula
* nombres
* apellidos
* correo
* número de rol si aplica
* estado

En modo dry-run:

* No ejecutar procedimiento.
* Registrar qué usuario sería creado.
* Registrar estado DRY_RUN_USUARIO_CREARIA.

En modo producción:

* Ejecutar procedimiento dentro de transacción.
* Auditar resultado.
* Confirmar commit solo si la operación y la auditoría fueron correctas.

# 12. Proceso 3: inactivación de usuarios

Objetivo:

Detectar usuarios que están deshabilitados en AD y todavía están activos en MAXIMO.

Condiciones:

* AD indica cuenta deshabilitada.
* MAXIMO.PERSON.STATUS = 'ACTIVO'.
* La cédula permite identificar la misma persona.
* No existe otro usuario habilitado en AD para esa misma cédula que deba mantenerse o migrarse.
* No existen restricciones funcionales en MAXIMO.

Antes de inactivar, validar restricciones.

Crear métodos placeholder:

* tieneActivosFijosAsignados(personId)
* perteneceAGruposCriticos(personId)
* tieneOrdenesTrabajoAbiertas(personId)
* tieneFlujosPendientes(personId)
* tieneOtrasRestricciones(personId)

Si existe alguna restricción:

* No inactivar.
* Registrar OBSERVADO_RESTRICCIONES_INACTIVACION.
* Guardar detalle en auditoría.

Si no existen restricciones:

* Ejecutar procedimiento almacenado:

inactivar_usuario(personId)

Si no existe procedimiento, dejar método configurable para adaptar.

Preferir procedimiento almacenado sobre UPDATE directo, porque MAXIMO puede tener lógica de negocio interna.

En modo dry-run:

* No inactivar.
* Registrar estado DRY_RUN_USUARIO_INACTIVARIA.

En modo producción:

* Inactivar solo casos seguros.
* Auditar resultado.
* Hacer rollback si ocurre error.

# 13. Proceso 4: sincronización de correo

Objetivo:

Actualizar o insertar el correo principal de trabajo en MAXIMO.EMAIL para usuarios habilitados en AD.

Este proceso debe ser independiente de la migración del usuario.

Puede ocurrir que:

* El usuario esté correcto y el correo incorrecto.
* El usuario esté incorrecto y el correo correcto.
* Ambos estén incorrectos.
* Ambos estén correctos.

Reglas:

* Procesar solo usuarios habilitados en AD.
* MAXIMO.PERSON debe existir.
* AD.mail no debe estar vacío.
* AD.mail debe tener formato válido.
* El correo no debe estar asignado a otro PERSONID.
* Si el correo ya pertenece a otro PERSONID, no actualizar ni insertar.
* Registrar OBSERVADO_EMAIL_DUPLICADO.

Casos:

1. Usuario existe en MAXIMO y tiene correo incorrecto:

   * Actualizar MAXIMO.EMAIL.EMAILADDRESS.
   * Registrar EMAIL_ACTUALIZADO.

2. Usuario existe en MAXIMO y no tiene correo:

   * Insertar correo principal.
   * Registrar EMAIL_INSERTADO.

3. Usuario existe y el correo ya es correcto:

   * No hacer nada.
   * Registrar SIN_CAMBIOS si aplica.

4. Correo AD ya pertenece a otro PERSONID:

   * No actualizar.
   * Registrar OBSERVADO_EMAIL_DUPLICADO.

5. Correo AD vacío:

   * No actualizar.
   * Registrar OBSERVADO_SIN_EMAIL_AD.

6. Correo AD inválido:

   * No actualizar.
   * Registrar OBSERVADO_EMAIL_INVALIDO.

# 14. Validaciones de duplicados y conflictos

Antes de actualizar, migrar, crear o inactivar, validar lo siguiente:

## Duplicados en Active Directory

Si una misma cédula aparece en más de un usuario de AD:

* No actualizar automáticamente.
* No migrar automáticamente.
* No crear automáticamente.
* Registrar OBSERVADO_DUPLICADO_AD.
* Incluir todos los usuarios AD encontrados en auditoría.

## Duplicados en MAXIMO

Si una misma cédula aparece en más de una persona de MAXIMO:

* No actualizar automáticamente salvo que exista una regla clara.
* Si se puede identificar un usuario activo correcto y otro inactivo incorrecto, registrarlo.
* Si se detecta que uno debe inactivarse, validar restricciones antes.
* Si hay ambigüedad, registrar OBSERVADO_DUPLICADO_MAXIMO.

## Logon Name reutilizado

Si AD.sAMAccountName coincide con un PERSONID de MAXIMO, pero la cédula es diferente:

* No migrar.
* No crear.
* No actualizar correo.
* Registrar OBSERVADO_LOGIN_REUTILIZADO.
* Considerar conflicto crítico.

## Cédula inválida

Si la cédula no cumple las reglas de normalización:

* No procesar.
* Registrar OBSERVADO_CEDULA_INVALIDA.

# 15. Modos de ejecución

La aplicación debe soportar dos modos:

## dry-run

* No ejecuta procedimientos.
* No actualiza Oracle.
* No inserta correos.
* No inactiva usuarios.
* Solo simula acciones.
* Registra auditoría de simulación si está configurado.
* Permite revisar qué habría ocurrido.

## produccion

* Ejecuta procedimientos y actualizaciones solo en casos seguros.
* Registra auditoría.
* Usa transacciones.
* Hace commit solo si la operación y auditoría fueron correctas.
* Hace rollback si ocurre error.

# 16. Ejecución desde la aplicación web

Crear endpoints protegidos para ejecutar procesos manualmente.

Endpoints sugeridos:

POST /sync-ad-maximo/api/sync/dry-run

POST /sync-ad-maximo/api/sync/produccion

POST /sync-ad-maximo/api/sync/produccion?cedula=2100816418

POST /sync-ad-maximo/api/sync/produccion?proceso=correo

POST /sync-ad-maximo/api/sync/produccion?proceso=todos

Procesos permitidos:

* migracion
* creacion
* correo
* inactivacion
* todos

Todos los endpoints administrativos deben requerir sesión autenticada y usuario autorizado `maxadmin`.

# 17. Scheduler interno

La aplicación debe incluir un scheduler interno configurable.

Puede usarse:

* ScheduledExecutorService
* o Quartz

El scheduler debe:

* iniciar cuando se despliega el WAR
* detenerse correctamente cuando Tomcat apague o redespliegue la aplicación
* evitar ejecuciones duplicadas
* tener lock interno
* registrar inicio y fin
* registrar duración
* registrar errores

Configuración:

scheduler:
habilitado: true
horaEjecucion: "06:00"
modo: produccion
proceso: todos

Si scheduler.habilitado = false, no debe ejecutarse automáticamente.

# 18. Compatibilidad con Apache Tomcat 9

La aplicación debe:

* generarse como WAR
* usar `javax.servlet.*`
* no usar `jakarta.servlet.*`
* usar Servlet API con scope provided
* no requerir Tomcat 10
* no usar dependencias incompatibles con Tomcat 9

Dependencia esperada:

<dependency>
    <groupId>javax.servlet</groupId>
    <artifactId>javax.servlet-api</artifactId>
    <version>4.0.1</version>
    <scope>provided</scope>
</dependency>

# 19. Configuración externa por ambiente

La PC de desarrollo usa Windows.

El servidor de despliegue usa Linux.

No asumir rutas fijas como `/opt`.

La aplicación debe leer la configuración desde una variable de entorno:

SYNC_AD_MAXIMO_CONFIG

Ejemplo Linux:

/etc/sync_ad_maximo/application-prod.yml

Ejemplo Windows:

C:\sync_ad_maximo\config\application-dev.yml

El archivo externo debe contener:

* conexión Oracle
* usuario técnico Oracle
* clave técnica Oracle
* conexión LDAP
* usuario técnico LDAP
* clave técnica LDAP
* configuración SMTP
* configuración de seguridad
* configuración del scheduler

El archivo con claves reales no debe ir dentro de `src/main/resources`, porque quedaría dentro del WAR.

El proyecto debe incluir solo un archivo de ejemplo:

application-example.yml

con datos ficticios.

Ejemplo de configuración:

oracle:
url: jdbc:oracle:thin:@//SERVIDOR_ORACLE:1521/SERVICE_NAME
username: MAXIMO_SYNC
password: CLAVE_ORACLE_PRODUCCION
schema: MAXIMO

ldap:
url: ldap://SERVIDOR_AD:389
domain: EMPRESA.LOCAL
baseDn: DC=empresa,DC=local
bindUser: [sync_ldap@empresa.local](mailto:sync_ldap@empresa.local)
bindPassword: CLAVE_LDAP_PRODUCCION
userSearchAttribute: sAMAccountName

security:
authMode: ACTIVE_DIRECTORY
allowedUsers:
- maxadmin
allowedGroups: []
sessionTimeoutMinutes: 30
allowManualProductionExecution: true
allowOnlyDryRunFromWeb: false

scheduler:
habilitado: true
horaEjecucion: "06:00"
modo: produccion
proceso: todos

mail:
enabled: true
host: smtp.empresa.local
port: 587
username: usuario_smtp
password: clave_smtp
starttls: true
from: [sync-ad-maximo@empresa.local](mailto:sync-ad-maximo@empresa.local)
subjectPrefix: "[SYNC AD MAXIMO]"
reportMode: RUN_ID
recipients:
to:
- [sistemas@empresa.local](mailto:sistemas@empresa.local)
cc: []
bcc: []

proceso:
estrategiaCedula9Digitos: AGREGAR_CERO
registrarAuditoriaDryRun: true

maximo:
procedureMigrarUsuario: migrar_usuario
procedureCrearUsuario: crear_usuario
procedureInactivarUsuario: inactivar_usuario
emailType: TRABAJO
emailPrimary: 1
emailIdSequence: ""
rowstampSequence: ""

# 20. Configuración en Linux con Tomcat

El README debe explicar dos formas de configurar SYNC_AD_MAXIMO_CONFIG.

## Opción A: setenv.sh

Si existe `$CATALINA_BASE/bin/setenv.sh`, agregar:

export SYNC_AD_MAXIMO_CONFIG=/etc/sync_ad_maximo/application-prod.yml

## Opción B: systemd

Si Tomcat corre como servicio:

sudo systemctl edit tomcat9

Agregar:

[Service]
Environment="SYNC_AD_MAXIMO_CONFIG=/etc/sync_ad_maximo/application-prod.yml"

Luego ejecutar:

sudo systemctl daemon-reload
sudo systemctl restart tomcat9

# 21. Configuración en Windows para desarrollo

El README debe explicar:

set SYNC_AD_MAXIMO_CONFIG=C:\sync_ad_maximo\config\application-dev.yml

O permanente:

setx SYNC_AD_MAXIMO_CONFIG "C:\sync_ad_maximo\config\application-dev.yml"

# 22. Manejo de credenciales

No quemar credenciales en varias clases del código.

No imprimir credenciales en logs.

No devolver credenciales en endpoints.

No incluir credenciales en reportes.

No subir archivos reales de configuración al repositorio.

Si por decisión institucional se desea compilar credenciales dentro del programa, crear una clase aislada:

InternalSecretsConfig.java

Esta clase debe estar documentada como opción no recomendada.

La aplicación debe priorizar la configuración externa por SYNC_AD_MAXIMO_CONFIG.

Solo si no existe SYNC_AD_MAXIMO_CONFIG, podrá usar InternalSecretsConfig como fallback opcional.

# 23. Login contra Active Directory

La aplicación web debe tener login.

El login debe validarse contra Active Directory.

Reglas:

1. El usuario ingresa usuario y contraseña.
2. La aplicación valida las credenciales contra AD mediante LDAP bind.
3. Si las credenciales son incorrectas, rechaza acceso.
4. Si las credenciales son correctas, valida autorización.
5. Solo puede ingresar el usuario `maxadmin`.

Por defecto:

security:
allowedUsers:
- maxadmin

También permitir autorización por grupo si se configura:

security:
allowedGroups:
- maxadmin

La contraseña de maxadmin no debe guardarse en la aplicación.

Debe validarse contra Active Directory en cada inicio de sesión.

# 24. Credenciales LDAP técnicas vs credenciales de login

La aplicación tendrá dos usos diferentes de Active Directory.

## Usuario técnico LDAP

Se configura en application-prod.yml.

Sirve para consultar:

* usuarios
* cédulas
* correos
* grupos
* estado de cuentas

Ejemplo:

[sync_ldap@empresa.local](mailto:sync_ldap@empresa.local)

## Usuario humano de login

Lo escribe la persona en la pantalla de login.

Ejemplo:

maxadmin

La contraseña no se almacena.

Solo se usa para validar acceso contra Active Directory.

# 25. Seguridad de sesión

Después de autenticar correctamente:

* Crear sesión HTTP.
* Guardar solo datos mínimos:

  * username
  * displayName
  * email
  * grupos
  * fecha de login
* No guardar contraseña.
* No registrar contraseña.
* Agregar logout.
* Invalidar sesión al cerrar sesión.
* Proteger todos los endpoints administrativos.

Endpoint público o semipúblico:

GET /sync-ad-maximo/api/health

El endpoint health no debe exponer información sensible.

# 26. Filtro de autenticación

Crear filtro compatible con Tomcat 9:

AuthFilter.java

Debe usar:

javax.servlet.Filter

Responsabilidades:

* Permitir acceso a login.
* Permitir acceso a logout.
* Permitir acceso a health.
* Permitir recursos estáticos.
* Bloquear endpoints administrativos si no existe sesión.
* Validar que el usuario autenticado sea maxadmin o pertenezca al grupo autorizado.
* Redirigir a login si es navegación web.
* Responder 401 o 403 si es petición API.

# 27. Auditoría centralizada en Oracle

La fuente principal de auditoría, errores, novedades y reportes debe ser Oracle.

No depender de carpetas físicas del servidor para revisar reportes.

El desarrollador puede no tener acceso a paths del ambiente productivo.

Por eso:

* Toda ejecución debe registrarse en Oracle.
* Toda novedad debe registrarse en Oracle.
* Todo error debe registrarse en Oracle.
* Los reportes deben consultarse desde la aplicación web.
* Los CSV deben generarse bajo demanda desde Oracle y descargarse por navegador.

La aplicación puede escribir logs técnicos mínimos en la salida estándar de Tomcat, pero no deben ser la fuente principal de seguimiento funcional.

# 28. Tabla de ejecución

Crear tabla:

CREATE TABLE MAXIMO.SYNC_AD_MAXIMO_RUN (
RUN_ID NUMBER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
FECHA_INICIO TIMESTAMP DEFAULT SYSTIMESTAMP,
FECHA_FIN TIMESTAMP,
MODO VARCHAR2(20),
PROCESO VARCHAR2(50),
USUARIO_EJECUTOR VARCHAR2(100),
ORIGEN_EJECUCION VARCHAR2(30),
ESTADO VARCHAR2(50),
TOTAL_MAXIMO NUMBER DEFAULT 0,
TOTAL_AD NUMBER DEFAULT 0,
TOTAL_MIGRADOS NUMBER DEFAULT 0,
TOTAL_CREADOS NUMBER DEFAULT 0,
TOTAL_INACTIVADOS NUMBER DEFAULT 0,
TOTAL_EMAIL_ACTUALIZADOS NUMBER DEFAULT 0,
TOTAL_EMAIL_INSERTADOS NUMBER DEFAULT 0,
TOTAL_SIN_CAMBIOS NUMBER DEFAULT 0,
TOTAL_OBSERVADOS NUMBER DEFAULT 0,
TOTAL_ERRORES NUMBER DEFAULT 0,
MENSAJE VARCHAR2(1000)
);

Si Oracle no soporta IDENTITY, generar alternativa con SEQUENCE.

Estados de ejecución:

* INICIADO
* FINALIZADO_OK
* FINALIZADO_CON_OBSERVACIONES
* FINALIZADO_CON_ERRORES
* ERROR_GENERAL

# 29. Tabla de auditoría funcional

Crear tabla:

CREATE TABLE MAXIMO.SYNC_AD_MAXIMO_AUDIT (
ID NUMBER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
RUN_ID NUMBER,
FECHA_EVENTO TIMESTAMP DEFAULT SYSTIMESTAMP,
MODO VARCHAR2(20),
PROCESO VARCHAR2(50),
CEDULA VARCHAR2(20),
PERSONID_MAXIMO_ANTERIOR VARCHAR2(100),
PERSONID_MAXIMO_NUEVO VARCHAR2(100),
PERSONID_AD VARCHAR2(100),
EMAIL_ANTERIOR VARCHAR2(250),
EMAIL_NUEVO VARCHAR2(250),
EMAIL_AD VARCHAR2(250),
ESTADO VARCHAR2(80),
MENSAJE VARCHAR2(1000),
DETALLE_ERROR CLOB,
CONSTRAINT FK_SYNC_AD_MAXIMO_RUN
FOREIGN KEY (RUN_ID)
REFERENCES MAXIMO.SYNC_AD_MAXIMO_RUN(RUN_ID)
);

Estados de auditoría:

* USUARIO_MIGRADO
* USUARIO_CREADO
* USUARIO_INACTIVADO
* EMAIL_ACTUALIZADO
* EMAIL_INSERTADO
* SIN_CAMBIOS
* DRY_RUN_USUARIO_MIGRARIA
* DRY_RUN_USUARIO_CREARIA
* DRY_RUN_USUARIO_INACTIVARIA
* DRY_RUN_EMAIL_ACTUALIZARIA
* DRY_RUN_EMAIL_INSERTARIA
* OBSERVADO_DUPLICADO_AD
* OBSERVADO_DUPLICADO_MAXIMO
* OBSERVADO_NO_ENCONTRADO_AD
* OBSERVADO_SIN_USUARIO_AD
* OBSERVADO_SIN_EMAIL_AD
* OBSERVADO_EMAIL_INVALIDO
* OBSERVADO_EMAIL_DUPLICADO
* OBSERVADO_LOGIN_REUTILIZADO
* OBSERVADO_CEDULA_INVALIDA
* OBSERVADO_RESTRICCIONES_INACTIVACION
* ERROR

# 30. Tabla de auditoría de acceso

Crear tabla:

CREATE TABLE MAXIMO.SYNC_AD_MAXIMO_ACCESS_AUDIT (
ID NUMBER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
FECHA_EVENTO TIMESTAMP DEFAULT SYSTIMESTAMP,
USUARIO VARCHAR2(100),
IP_ORIGEN VARCHAR2(100),
ACCION VARCHAR2(100),
ESTADO VARCHAR2(100),
MENSAJE VARCHAR2(1000)
);

Estados:

* LOGIN_OK
* LOGIN_FALLIDO_CREDENCIALES
* LOGIN_FALLIDO_NO_AUTORIZADO
* LOGOUT
* ACCESO_DENEGADO
* EJECUCION_MANUAL_DRY_RUN
* EJECUCION_MANUAL_PRODUCCION

# 31. Tabla de auditoría de correos enviados

Crear tabla:

CREATE TABLE MAXIMO.SYNC_AD_MAXIMO_MAIL_AUDIT (
ID NUMBER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
RUN_ID NUMBER,
FECHA_ENVIO TIMESTAMP DEFAULT SYSTIMESTAMP,
DESTINATARIOS VARCHAR2(1000),
COPIAS VARCHAR2(1000),
ASUNTO VARCHAR2(500),
ESTADO VARCHAR2(100),
MENSAJE VARCHAR2(1000),
DETALLE_ERROR CLOB
);

Estados:

* EMAIL_REPORTE_ENVIADO
* EMAIL_REPORTE_ERROR
* EMAIL_REPORTE_DESHABILITADO

Si Oracle no soporta IDENTITY, generar alternativa con SEQUENCE para todas las tablas.

# 32. Manejo transaccional de auditoría

Para operaciones exitosas:

1. Ejecutar cambio en MAXIMO.
2. Registrar auditoría.
3. Hacer commit solo si ambas acciones fueron correctas.

Para errores críticos:

1. Hacer rollback de la operación de negocio.
2. Registrar el error en auditoría en una transacción separada.
3. No perder el detalle técnico del error.

Si falla el envío de correo:

* No revertir cambios de MAXIMO.
* Registrar error de correo en auditoría.
* Registrar error en tabla de mail audit.
* Mantener estado de ejecución final.

# 33. Reportes desde aplicación web

Crear pantallas web o endpoints para consultar históricos desde Oracle.

Pantallas sugeridas:

1. Historial de ejecuciones.
2. Detalle de una ejecución.
3. Observados.
4. Errores.
5. Auditoría de accesos.
6. Auditoría de correos enviados.

Filtros sugeridos:

* fecha desde
* fecha hasta
* RUN_ID
* cédula
* PERSONID
* estado
* proceso
* usuario ejecutor

Endpoints sugeridos:

GET /sync-ad-maximo/api/reportes/ejecuciones

GET /sync-ad-maximo/api/reportes/ejecuciones/{runId}

GET /sync-ad-maximo/api/reportes/observados

GET /sync-ad-maximo/api/reportes/errores

GET /sync-ad-maximo/api/reportes/accesos

GET /sync-ad-maximo/api/reportes/correos

Todos deben estar protegidos, excepto health.

# 34. Exportación CSV bajo demanda

Los reportes CSV no deben depender de carpetas físicas.

Deben generarse en memoria desde Oracle y descargarse desde el navegador.

Endpoints:

GET /sync-ad-maximo/api/reportes/ejecuciones/{runId}/csv

GET /sync-ad-maximo/api/reportes/observados/csv

GET /sync-ad-maximo/api/reportes/errores/csv

GET /sync-ad-maximo/api/reportes/accesos/csv

GET /sync-ad-maximo/api/reportes/correos/csv

El CSV debe incluir como mínimo:

* fecha_evento
* modo
* proceso
* cedula
* personid_maximo_anterior
* personid_maximo_nuevo
* personid_ad
* email_anterior
* email_nuevo
* email_ad
* estado
* mensaje
* detalle_error

# 35. Envío automático de reporte diario por correo

La aplicación debe incluir opción configurable para enviar automáticamente por correo el reporte de auditoría de cada ejecución.

El correo debe enviarse al finalizar cada ejecución, sea exitosa, con observaciones o con errores.

El correo debe incluir:

1. Resumen en el cuerpo del mensaje.
2. Totales de la ejecución.
3. Estado final.
4. Archivo CSV adjunto con detalle de auditoría.
5. Mensaje especial si existieron observados o errores.

Configuración:

mail:
enabled: true
host: smtp.empresa.local
port: 587
username: usuario_smtp
password: clave_smtp
starttls: true
from: [sync-ad-maximo@empresa.local](mailto:sync-ad-maximo@empresa.local)
subjectPrefix: "[SYNC AD MAXIMO]"
reportMode: RUN_ID
recipients:
to:
- [sistemas@empresa.local](mailto:sistemas@empresa.local)
- [talento.humano@empresa.local](mailto:talento.humano@empresa.local)
cc:
- [auditoria.ti@empresa.local](mailto:auditoria.ti@empresa.local)
bcc: []

reportMode permitido:

* RUN_ID
* DAILY

Por defecto usar RUN_ID.

RUN_ID envía solo la auditoría de la ejecución actual.

DAILY envía toda la auditoría del día.

Nombre del adjunto:

sync_ad_maximo_auditoria_YYYYMMDD_RUNID.csv

Ejemplo:

sync_ad_maximo_auditoria_20260529_105.csv

# 36. Contenido del correo

El cuerpo del correo debe ser HTML simple.

Debe incluir:

* fecha y hora de inicio
* fecha y hora de fin
* modo de ejecución
* proceso ejecutado
* usuario ejecutor si fue manual
* origen de ejecución: SCHEDULER o MANUAL
* estado final
* total de registros leídos desde MAXIMO
* total de usuarios leídos desde AD
* total de usuarios migrados
* total de usuarios creados
* total de usuarios inactivados
* total de correos actualizados
* total de correos insertados
* total de registros sin cambios
* total de observados
* total de errores

Asunto sugerido:

[SYNC AD MAXIMO] Reporte diario - FINALIZADO_CON_OBSERVACIONES - 2026-05-29

# 37. Reenvío manual de reporte

Agregar opción para reenviar el reporte de una ejecución anterior.

Endpoint:

POST /sync-ad-maximo/api/reportes/ejecuciones/{runId}/enviar-correo

Reglas:

* Solo puede usarlo usuario autenticado.
* Solo maxadmin.
* Registrar auditoría de acceso.
* Registrar auditoría del envío.
* No modificar datos de la ejecución original.

# 38. Dependencias Maven esperadas

El pom.xml debe incluir dependencias para:

* Oracle JDBC Thin Driver.
* Servlet API javax con scope provided.
* LDAP.
* YAML.
* Logging.
* CSV.
* JavaMail javax.mail.
* Pruebas unitarias si aplica.

No usar jakarta.mail.

No usar jakarta.servlet.

# 39. Estructura esperada del proyecto

Generar esta estructura:

sync_ad_maximo/
├── pom.xml
├── README.md
├── config/
│   └── application-example.yml
├── scripts/
│   ├── crear_tablas_auditoria.sql
│   ├── crear_secuencias_auditoria.sql
│   ├── crear_indices.sql
│   └── ejemplo_context_tomcat.xml
├── src/
│   └── main/
│       ├── java/
│       │   └── ec/
│       │       └── empresa/
│       │           └── syncadmaximo/
│       │               ├── Main.java
│       │               ├── config/
│       │               │   ├── AppConfig.java
│       │               │   ├── OracleConfig.java
│       │               │   ├── LdapConfig.java
│       │               │   ├── MailConfig.java
│       │               │   ├── SecurityConfig.java
│       │               │   └── InternalSecretsConfig.java
│       │               ├── model/
│       │               │   ├── AdUser.java
│       │               │   ├── MaximoPerson.java
│       │               │   ├── MaximoEmail.java
│       │               │   ├── SyncResult.java
│       │               │   ├── AuditRecord.java
│       │               │   ├── RunRecord.java
│       │               │   └── AuthenticatedUser.java
│       │               ├── repository/
│       │               │   ├── MaximoRepository.java
│       │               │   ├── AuditRepository.java
│       │               │   ├── RunAuditRepository.java
│       │               │   ├── AccessAuditRepository.java
│       │               │   ├── MailAuditRepository.java
│       │               │   └── ReportRepository.java
│       │               ├── ldap/
│       │               │   └── ActiveDirectoryClient.java
│       │               ├── security/
│       │               │   ├── ActiveDirectoryAuthService.java
│       │               │   └── PasswordUtils.java
│       │               ├── scheduler/
│       │               │   └── SyncScheduler.java
│       │               ├── service/
│       │               │   ├── SyncOrchestrator.java
│       │               │   ├── MigrationService.java
│       │               │   ├── CreationService.java
│       │               │   ├── InactivationService.java
│       │               │   ├── EmailSyncService.java
│       │               │   ├── ValidationService.java
│       │               │   ├── RunHistoryService.java
│       │               │   ├── ReportService.java
│       │               │   ├── CsvExportService.java
│       │               │   ├── AuditCsvGenerator.java
│       │               │   ├── MailService.java
│       │               │   └── DailyReportEmailService.java
│       │               ├── web/
│       │               │   ├── LoginServlet.java
│       │               │   ├── LogoutServlet.java
│       │               │   ├── AuthFilter.java
│       │               │   ├── SyncController.java
│       │               │   ├── ReportController.java
│       │               │   └── HealthController.java
│       │               └── util/
│       │                   ├── CedulaUtils.java
│       │                   ├── EmailUtils.java
│       │                   ├── TextUtils.java
│       │                   ├── CsvUtils.java
│       │                   └── DateUtils.java
│       └── webapp/
│           ├── login.jsp
│           ├── index.jsp
│           └── WEB-INF/
│               └── web.xml

# 40. Responsabilidades principales de clases

## SyncOrchestrator

* Coordinar todo el proceso.
* Crear RUN_ID.
* Ejecutar validaciones.
* Llamar servicios de migración, creación, correo e inactivación.
* Actualizar totales.
* Finalizar ejecución.
* Invocar envío de correo si está habilitado.

## MigrationService

* Detectar usuarios con PERSONID diferente al sAMAccountName.
* Ejecutar migrar_usuario.
* Auditar resultado.

## CreationService

* Detectar usuarios activos en AD que no existen en MAXIMO.
* Ejecutar crear_usuario.
* Auditar resultado.

## InactivationService

* Detectar usuarios deshabilitados en AD y activos en MAXIMO.
* Validar restricciones.
* Ejecutar inactivar_usuario.
* Auditar resultado.

## EmailSyncService

* Comparar correo MAXIMO.EMAIL contra AD.mail.
* Actualizar o insertar correo.
* Validar correo duplicado.
* Auditar resultado.

## ValidationService

* Detectar duplicados.
* Detectar cédulas inválidas.
* Detectar login reutilizado.
* Detectar ambigüedades.

## ActiveDirectoryClient

* Consultar usuarios habilitados.
* Consultar usuarios deshabilitados.
* Consultar usuario por sAMAccountName.
* Consultar grupos del usuario.
* Validar credenciales de login.

## ActiveDirectoryAuthService

* Validar login contra AD.
* Confirmar si el usuario es maxadmin.
* Consultar grupos si aplica.

## AuthFilter

* Proteger endpoints.
* Validar sesión.
* Bloquear accesos no autorizados.

## ReportController

* Consultar reportes.
* Exportar CSV.
* Reenviar correo de una ejecución.

## DailyReportEmailService

* Generar resumen HTML.
* Generar CSV de auditoría.
* Enviar correo.
* Registrar auditoría del envío.

# 41. Índices recomendados

Generar script SQL para índices:

CREATE INDEX MAXIMO.IDX_PERSON_EPP_CEDULA
ON MAXIMO.PERSON(EPP_CEDULA);

CREATE INDEX MAXIMO.IDX_PERSON_PERSONID
ON MAXIMO.PERSON(PERSONID);

CREATE INDEX MAXIMO.IDX_EMAIL_PERSONID
ON MAXIMO.EMAIL(PERSONID);

CREATE INDEX MAXIMO.IDX_EMAIL_EMAILADDRESS
ON MAXIMO.EMAIL(EMAILADDRESS);

CREATE INDEX MAXIMO.IDX_SYNC_RUN_FECHA
ON MAXIMO.SYNC_AD_MAXIMO_RUN(FECHA_INICIO);

CREATE INDEX MAXIMO.IDX_SYNC_RUN_ESTADO
ON MAXIMO.SYNC_AD_MAXIMO_RUN(ESTADO);

CREATE INDEX MAXIMO.IDX_SYNC_AUDIT_RUN
ON MAXIMO.SYNC_AD_MAXIMO_AUDIT(RUN_ID);

CREATE INDEX MAXIMO.IDX_SYNC_AUDIT_CEDULA
ON MAXIMO.SYNC_AD_MAXIMO_AUDIT(CEDULA);

CREATE INDEX MAXIMO.IDX_SYNC_AUDIT_PERSONID_AD
ON MAXIMO.SYNC_AD_MAXIMO_AUDIT(PERSONID_AD);

CREATE INDEX MAXIMO.IDX_SYNC_AUDIT_ESTADO
ON MAXIMO.SYNC_AD_MAXIMO_AUDIT(ESTADO);

CREATE INDEX MAXIMO.IDX_SYNC_ACCESS_FECHA
ON MAXIMO.SYNC_AD_MAXIMO_ACCESS_AUDIT(FECHA_EVENTO);

CREATE INDEX MAXIMO.IDX_SYNC_ACCESS_USUARIO
ON MAXIMO.SYNC_AD_MAXIMO_ACCESS_AUDIT(USUARIO);

# 42. Reglas técnicas obligatorias

* Usar consultas parametrizadas.
* Usar transacciones Oracle.
* Usar Oracle JDBC Thin Driver.
* No requerir Oracle Client instalado.
* No procesar registros ambiguos.
* No procesar cédulas duplicadas sin validación.
* No migrar si existe conflicto de login reutilizado.
* No crear si el usuario ya existe con otra cédula.
* No actualizar correo si está vacío.
* No actualizar correo si es inválido.
* No actualizar correo si ya pertenece a otro PERSONID.
* No inactivar si existen restricciones.
* Hacer rollback ante errores críticos.
* Registrar error en auditoría aunque se haga rollback de negocio.
* No registrar contraseñas.
* No registrar credenciales.
* No exponer información sensible en health.
* Generar CSV desde memoria.
* No depender de carpetas físicas para reportes.
* Mantener el valor original de AD al actualizar.
* Comparar en minúsculas solo para validación lógica.
* Proteger endpoints administrativos.
* Permitir acceso solo a maxadmin.

# 43. README requerido

Generar README.md con:

1. Descripción del proyecto.
2. Arquitectura general.
3. Requisitos.
4. Compatibilidad con Tomcat 9.
5. Cómo compilar:
   mvn clean package
6. Resultado esperado:
   target/sync-ad-maximo.war
7. Cómo desplegar en Tomcat 9.
8. Cómo configurar SYNC_AD_MAXIMO_CONFIG en Windows.
9. Cómo configurar SYNC_AD_MAXIMO_CONFIG en Linux con setenv.sh.
10. Cómo configurar SYNC_AD_MAXIMO_CONFIG en Linux con systemd.
11. Cómo configurar Oracle.
12. Cómo configurar LDAP.
13. Cómo configurar SMTP.
14. Cómo configurar scheduler.
15. Cómo configurar usuario autorizado maxadmin.
16. Cómo ingresar al sistema.
17. Cómo ejecutar dry-run.
18. Cómo ejecutar producción.
19. Cómo consultar auditoría.
20. Cómo exportar CSV.
21. Cómo reenviar reporte por correo.
22. Cómo revisar auditoría de accesos.
23. Cómo revisar auditoría de correos enviados.
24. Cómo crear tablas de auditoría.
25. Cómo crear índices.
26. Cómo adaptar procedimientos almacenados.
27. Cómo adaptar EMAILID y ROWSTAMP.
28. Recomendaciones de seguridad.
29. Consideraciones para producción.
30. Mantenimiento de auditoría histórica.

# 44. Resultado esperado

Primero generar todos los archivos del proyecto con código completo.

Luego generar:

1. README.md.
2. pom.xml.
3. application-example.yml.
4. scripts SQL de auditoría.
5. scripts SQL de índices.
6. clases Java completas.
7. servlets compatibles con Tomcat 9.
8. filtro de autenticación.
9. scheduler.
10. servicios de sincronización.
11. generación de CSV.
12. envío de correo.
13. consultas de reportes.

El código debe estar listo para copiar, pegar, compilar y revisar en Visual Studio Code.

No generar explicaciones genéricas. Generar una implementación base funcional y extensible.

Este prompt ya queda integrado para pedirle a Copilot una solución completa, no solo un script suelto.



----


Dado que las instrucciones originales mencionan brevemente incluir dependencias para "Pruebas unitarias si aplica"
 y están diseñadas para pedirle el código a un asistente de IA (como Copilot)
, la mejor forma de solicitar el uso de mocks es agregar un apartado específico a tus instrucciones (el prompt) que obligue a la IA a generar pruebas unitarias aisladas.
Para pedirlo correctamente, te sugiero agregar a tu solicitud original los siguientes requerimientos:
1. Exigir frameworks de Mocking en el pom.xml: Debes pedir explícitamente que, además de las dependencias mencionadas
, se incluyan JUnit 5 y Mockito (o un framework similar) con el scope test.
2. Ampliar la estructura de directorios: La estructura actual del proyecto solo pide generar la carpeta src/main/java
. Debes solicitar que también se genere la carpeta src/test/java/ec/empresa/syncadmaximo/ con sus respectivas clases de prueba.
3. Especificar qué componentes deben ser "mockeados": Para lograr un ambiente aislado en desarrollo, debes pedirle a la IA que cree pruebas unitarias para la clase principal SyncOrchestrator
 y los servicios de negocio (MigrationService, CreationService, etc.), utilizando mocks para aislar estrictamente las siguientes capas externas
:
Base de Datos Oracle: Mockear MaximoRepository, AuditRepository y las demás clases del paquete repository para no requerir conexión a la base real.
Active Directory: Mockear ActiveDirectoryClient
 para simular la existencia de usuarios habilitados, deshabilitados o con conflictos sin conectarse al servidor LDAP.
Servidor SMTP: Mockear MailService o DailyReportEmailService
 para simular el envío de correos.
Ejemplo de cómo redactar la petición a agregar:
"Para el desarrollo local, es obligatorio el uso de pruebas unitarias con mocks. Añade JUnit 5 y Mockito al pom.xml. Extiende la estructura del proyecto para incluir el directorio src/test/java/... y genera pruebas unitarias exhaustivas para la clase SyncOrchestrator y los servicios del paquete service. En estas pruebas, debes mockear obligatoriamente la capa de repositorios (Oracle), el cliente ActiveDirectoryClient (LDAP) y los servicios de correo, garantizando que los tests se ejecuten al 100% en aislamiento y sin requerir infraestructura externa."
Al pedirlo de esta manera, combinarás lo mejor de ambos mundos: tendrás mocks para las pruebas unitarias que garanticen la calidad del código durante el desarrollo local, y mantendrás el modo dry-run
 para que los administradores puedan realizar simulaciones funcionales con datos reales en los ambientes de prueba o producción.
