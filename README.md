# SMA - Sistema Multiagente para la Gestión del Drenaje Pluvial de Cartagena

Sistema multiagente desarrollado en Java utilizando **JADE (Java Agent DEvelopment Framework)**, orientado a la simulación del monitoreo, detección de obstrucciones y gestión de intervenciones preventivas en canales de drenaje pluvial.

El proyecto representa un escenario simulado de la red de drenaje de Cartagena de Indias, en el que diferentes agentes colaboran para identificar condiciones de riesgo, estimar posibles inundaciones y priorizar intervenciones según la disponibilidad de recursos.

## Objetivo del proyecto

Desarrollar un Sistema Multiagente (SMA) capaz de simular la supervisión de canales de drenaje, detectar situaciones que puedan comprometer su funcionamiento y coordinar acciones preventivas mediante la comunicación entre agentes autónomos.

## Arquitectura del sistema

El sistema está compuesto por cinco agentes especializados:

| Agente | Responsabilidad |
|---|---|
| **DetectorObstrucciones** | Supervisa los canales y detecta niveles de obstrucción. Genera alertas y notifica cambios en la disponibilidad de los sensores. |
| **MonitorCanal** | Recibe alertas y estados de los sensores, y supervisa las condiciones de los canales. |
| **PredictorInundaciones** | Estima el nivel de agua y calcula un índice de riesgo de inundación. |
| **GestorPrioridades** | Clasifica los canales según el nivel de riesgo y determina la prioridad de intervención. |
| **PlanificadorPrevencion** | Gestiona las órdenes de intervención y asigna los recursos disponibles según las prioridades. |

### Flujo de funcionamiento

1. El agente detector supervisa las condiciones de los canales.
2. Cuando identifica una obstrucción, genera una alerta y comunica el evento al monitor.
3. El predictor calcula el riesgo de inundación a partir del nivel de agua y el porcentaje de obstrucción.
4. El gestor determina la prioridad de cada canal.
5. El planificador evalúa las prioridades y asigna los recursos disponibles.
6. Si un sensor deja de estar disponible, el sistema actualiza el estado del canal, elimina los resultados desactualizados y suspende la orden correspondiente. Cuando el sensor se recupera, el canal vuelve a evaluarse.

### Comunicación entre agentes

Los agentes intercambian información mediante mensajes ACL (Agent Communication Language), siguiendo conversaciones identificadas mediante `conversationId`.

Entre los mensajes implementados se encuentran:

- `alerta-obstruccion`
- `prediccion-inundacion`
- `prioridad-intervencion`
- `estado-sensor`

El sistema utiliza un entorno compartido (`RedCanalesDrenaje`) para almacenar el estado de los canales, alertas, predicciones, prioridades, recursos y órdenes de intervención.

**Nota de arquitectura:** La solución utiliza un enfoque híbrido: combina comunicación mediante mensajes ACL con un entorno compartido para la persistencia y consulta de información.

## Tecnologías utilizadas

- **Java 21**
- **JADE 4.3**
- **Maven**
- **IntelliJ IDEA**
- **Git y GitHub**

## Estructura del proyecto

```text
SMA-Drenaje-Cartagena/
│
├── src/
│   └── main/
│       └── java/
│           └── com/
│               └── sma/
│                   └── cartagena/
│                       ├── Main.java
│                       │
│                       ├── agents/
│                       │   ├── DetectorObstrucciones.java
│                       │   ├── MonitorCanal.java
│                       │   ├── PredictorInundaciones.java
│                       │   ├── GestorPrioridades.java
│                       │   └── PlanificadorPrevencion.java
│                       │
│                       └── environment/
│                           └── RedCanalesDrenaje.java
│
├── pom.xml
└── README.md
```

## Requisitos

- Java JDK 21.
- IntelliJ IDEA u otro IDE compatible con Maven.
- Maven (o el Maven integrado de IntelliJ).
- Conexión a Internet para descargar las dependencias durante la primera compilación.

## Instalación y ejecución

1. Clonar el repositorio:

```bash
git clone https://github.com/bettin123/SMA-Drenaje-Cartagena.git
```

2. Abrir el proyecto desde IntelliJ IDEA.

3. Esperar a que Maven descargue y sincronice las dependencias definidas en `pom.xml`.

4. Ejecutar la clase principal:

```text
com.sma.cartagena.Main
```

5. Utilizar el menú de consola para consultar los canales, modificar sus condiciones, agregar canales y controlar la simulación.

La interfaz gráfica de JADE RMA permite visualizar los agentes registrados y su estado de ejecución.

## Simulación

El entorno inicial contiene tres canales con diferentes condiciones:

| Canal | Nivel de agua | Caudal | Obstrucción | Sensor |
|---|---:|---:|---:|---|
| CAN-001 | 35 % | 8.0 m³/s | 10 % | Disponible |
| CAN-002 | 72 % | 5.2 m³/s | 65 % | Disponible |
| CAN-003 | 90 % | 3.1 m³/s | 88 % | Disponible |

### Clasificación del riesgo

El predictor calcula un índice de riesgo utilizando el nivel de agua estimado y el porcentaje de obstrucción.

La clasificación implementada es:

- **BAJO:** índice inferior a 40.
- **MODERADO:** índice desde 40 hasta menos de 70.
- **ALTO:** índice igual o superior a 70.

### Gestión de prioridades

El gestor clasifica las intervenciones mediante tres niveles:

- **P1:** prioridad alta.
- **P2:** prioridad media.
- **P3:** prioridad baja.

El planificador considera la prioridad, el índice de riesgo y la disponibilidad de recursos para tomar decisiones.

Las órdenes pueden presentar estados como:

- `AUTORIZADA`
- `ESCALADA`
- `POSPUESTA`
- `SUSPENDIDA`

La simulación contempla la disponibilidad limitada de recursos. Por ejemplo, cuando una cuadrilla está asignada a un canal, otra intervención puede quedar escalada o pospuesta según su prioridad.

## Pruebas realizadas

Se realizaron pruebas funcionales sobre los siguientes escenarios:

- Detección de obstrucciones y generación de alertas.
- Cálculo y clasificación del riesgo de inundación.
- Asignación de prioridades de intervención.
- Asignación de recursos limitados.
- Pérdida de disponibilidad de un sensor.
- Suspensión de órdenes ante la pérdida de datos.
- Recuperación del sensor y reevaluación del canal.
- Liberación y reasignación de recursos.

## Estado del proyecto

Proyecto académico desarrollado como ejercicio de aplicación de los Sistemas Multiagente, comunicación entre agentes y toma de decisiones coordinada.

---

**Repositorio:** [SMA-Drenaje-Cartagena](https://github.com/bettin123/SMA-Drenaje-Cartagena)
