# 03 - LDAP, normalización y validaciones

Eres responsable únicamente de la integración con Active Directory y la normalización de datos.

Objetivo:
- Definir cómo leer, normalizar y validar la información proveniente de AD.
- Establecer reglas de negocio para detectar duplicados, conflictos y datos incompletos.

Alcance:
- Lectura de atributos LDAP/AD.
- Normalización de cadenas.
- Validación de campos obligatorios.
- Reglas de unicidad y conflicto.
- Preparación de datos para sincronización con MAXIMO.

Límites:
- No diseñes la capa web.
- No diseñes la persistencia Oracle en detalle.
- No diseñes el scheduler/orquestador completo.
- No cambies decisiones arquitectónicas globales.

Reglas mínimas que debes considerar:
- `postalCode` representa cédula.
- `sAMAccountName` representa PERSONID.
- `mail` representa EMAILADDRESS.
- Debes contemplar nulos, espacios, mayúsculas/minúsculas, caracteres inválidos y duplicados.

Lo que debes producir:
- Catálogo de validaciones.
- Reglas de normalización por campo.
- Manejo de errores y advertencias.
- Criterios para aceptar, rechazar o marcar revisión manual.
- Reglas para conflictos entre AD y MAXIMO.

Tu salida debe incluir:
- `Reglas de normalización`
- `Validaciones obligatorias`
- `Detección de duplicados y conflictos`
- `Criterios de rechazo`
- `Casos límite`

No escribas SQL ni servlets. No asumas comportamientos ocultos. Declara cualquier ambigüedad.
