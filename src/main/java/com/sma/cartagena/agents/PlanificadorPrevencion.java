
package com.sma.cartagena.agents;

import com.sma.cartagena.environment.RedCanalesDrenaje;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlanificadorPrevencion extends Agent {

    private final Map<String, RedCanalesDrenaje.Prioridad>
            prioridadesPendientes = new HashMap<>();

    @Override
    protected void setup() {

        System.out.println("[PLANIFICADOR] Agente iniciado.");
        System.out.println("[PLANIFICADOR] Preparado para planificar intervenciones.");

        // ==================================================
        // 1. RECIBIR MENSAJES DEL GESTOR
        // ==================================================

        addBehaviour(new CyclicBehaviour(this) {

            @Override
            public void action() {

                ACLMessage mensaje = receive();

                if (mensaje == null) {
                    block();
                    return;
                }

                if (mensaje.getPerformative() != ACLMessage.INFORM) {
                    return;
                }

                String conversacion = mensaje.getConversationId();

                try {

                    if ("prioridad-intervencion".equals(conversacion)) {

                        procesarPrioridad(mensaje);

                    } else if ("estado-sensor".equals(conversacion)) {

                        procesarEstadoSensor(mensaje);

                    } else {

                        System.out.println(
                                "[PLANIFICADOR] Mensaje desconocido: "
                                        + conversacion
                        );
                    }

                } catch (Exception e) {

                    System.out.println(
                            "[PLANIFICADOR] Error procesando mensaje: "
                                    + e.getMessage()
                    );
                }
            }
        });

        // ==================================================
        // 2. PROCESAR Y ACTUALIZAR INTERVENCIONES
        // ==================================================

        addBehaviour(new TickerBehaviour(this, 2000) {

            @Override
            protected void onTick() {

                if (prioridadesPendientes.isEmpty()) {
                    return;
                }

                RedCanalesDrenaje entorno =
                        RedCanalesDrenaje.getInstancia();

                List<RedCanalesDrenaje.Prioridad> prioridades =
                        new ArrayList<>(prioridadesPendientes.values());

                prioridadesPendientes.clear();

                // Ordenar de mayor a menor riesgo.
                prioridades.sort(
                        Comparator.comparingDouble(
                                RedCanalesDrenaje.Prioridad::indiceRiesgo
                        ).reversed()
                );

                System.out.println(
                        "\n[PLANIFICADOR] Evaluando prioridades actualizadas..."
                );

                // ==================================================
                // FASE 1: LIBERAR RECURSOS DE CANALES P3
                // ==================================================

                for (RedCanalesDrenaje.Prioridad prioridad : prioridades) {

                    if (!prioridad.codigoPrioridad().equals("P3")) {
                        continue;
                    }

                    RedCanalesDrenaje.OrdenIntervencion ordenAnterior =
                            entorno.obtenerOrden(prioridad.canalId());

                    if (ordenAnterior != null
                            && !ordenAnterior.recursoId().equals("NINGUNO")) {

                        entorno.liberarRecurso(ordenAnterior.recursoId());

                        System.out.println(
                                "[PLANIFICADOR] Recurso liberado: "
                                        + ordenAnterior.recursoId()
                                        + " | Canal: " + prioridad.canalId()
                        );
                    }
                }

                // ==================================================
                // FASE 2: EVALUAR Y ACTUALIZAR ÓRDENES
                // ==================================================

                for (RedCanalesDrenaje.Prioridad prioridad : prioridades) {

                    String canalId = prioridad.canalId();
                    String codigoPrioridad = prioridad.codigoPrioridad();

                    RedCanalesDrenaje.OrdenIntervencion ordenAnterior =
                            entorno.obtenerOrden(canalId);

                    System.out.println(
                            "[PLANIFICADOR] Evaluando: " + canalId
                                    + " | Prioridad: " + codigoPrioridad
                                    + " | Índice: "
                                    + String.format("%.2f", prioridad.indiceRiesgo())
                    );

                    String decision;
                    String recursoId = "NINGUNO";
                    String motivo;

                    // Si el canal ya tenía una cuadrilla y sigue
                    // requiriendo intervención, conservar la reserva.
                    boolean tieneRecursoAnterior =
                            ordenAnterior != null
                                    && !ordenAnterior.recursoId().equals("NINGUNO");

                    if (codigoPrioridad.equals("P1")
                            || codigoPrioridad.equals("P2")) {

                        RedCanalesDrenaje.Recurso recurso = null;

                        if (tieneRecursoAnterior) {

                            recursoId = ordenAnterior.recursoId();

                        } else {

                            recurso = entorno.reservarRecurso("LIMPIEZA_CANAL");

                            if (recurso != null) {
                                recursoId = recurso.id();
                            }
                        }

                        if (!recursoId.equals("NINGUNO")) {

                            decision = "AUTORIZADA";

                            motivo = codigoPrioridad.equals("P1")
                                    ? "Riesgo crítico. Recurso reservado."
                                    : "Riesgo moderado. Recurso reservado.";

                        } else if (codigoPrioridad.equals("P1")) {

                            decision = "ESCALADA";
                            motivo = "Riesgo crítico sin recursos disponibles.";

                        } else {

                            decision = "POSPUESTA";
                            motivo = "Recurso no disponible. Pendiente de atención.";
                        }

                    } else {

                        decision = "POSPUESTA";
                        motivo = "Riesgo bajo. Se mantiene vigilancia preventiva.";
                    }

                    // Crear la nueva versión de la orden.
                    RedCanalesDrenaje.OrdenIntervencion nuevaOrden =
                            new RedCanalesDrenaje.OrdenIntervencion(
                                    "ORD-" + canalId,
                                    canalId,
                                    codigoPrioridad,
                                    decision,
                                    recursoId,
                                    motivo
                            );

                    // Registrar o reemplazar la orden anterior.
                    entorno.actualizarOrden(nuevaOrden);

                    System.out.println(
                            "[PLANIFICADOR] ORDEN "
                                    + (ordenAnterior == null ? "CREADA" : "ACTUALIZADA")
                                    + " | Canal: " + canalId
                                    + " | Prioridad: " + codigoPrioridad
                                    + " | Decisión: " + decision
                                    + " | Recurso: " + recursoId
                    );
                }

                System.out.println(
                        "[PLANIFICADOR] Evaluación finalizada. Órdenes: "
                                + entorno.obtenerOrdenes().size()
                );
            }
        });
    }

    // ==================================================
    // 3. PROCESAR PRIORIDADES
    // ==================================================

    private void procesarPrioridad(ACLMessage mensaje) {

        String[] datos =
                mensaje.getContent().split("\\|", 5);

        if (datos.length != 5) {

            System.out.println(
                    "[PLANIFICADOR] Mensaje de prioridad inválido."
            );

            return;
        }

        String canalId = datos[0];
        String codigoPrioridad = datos[1];
        String nivelRiesgo = datos[2];
        double indiceRiesgo = Double.parseDouble(datos[3]);
        String descripcion = datos[4];

        RedCanalesDrenaje.Prioridad prioridad =
                new RedCanalesDrenaje.Prioridad(
                        canalId,
                        codigoPrioridad,
                        nivelRiesgo,
                        indiceRiesgo,
                        descripcion
                );

        // Conservar la prioridad más reciente de cada canal.
        prioridadesPendientes.put(canalId, prioridad);

        System.out.println(
                "[PLANIFICADOR] Prioridad recibida: "
                        + canalId
                        + " | " + codigoPrioridad
                        + " | Índice: "
                        + String.format("%.2f", indiceRiesgo)
        );
    }

    // ==================================================
    // 4. PROCESAR ESTADO DEL SENSOR
    // ==================================================

    private void procesarEstadoSensor(ACLMessage mensaje) {

        String[] datos = mensaje.getContent().split("\\|", 2);

        if (datos.length != 2) {

            System.out.println(
                    "[PLANIFICADOR] Mensaje de estado de sensor inválido."
            );

            return;
        }

        String canalId = datos[0];
        String estado = datos[1];

        RedCanalesDrenaje entorno =
                RedCanalesDrenaje.getInstancia();

        // =========================================
        // SENSOR SIN DATOS
        // =========================================

        if (estado.equals("SIN_DATOS")) {

            System.out.println(
                    "\n[PLANIFICADOR] ¡ATENCIÓN! Sensor sin datos en "
                            + canalId
            );

            // Eliminar cualquier prioridad que aún esté pendiente.
            prioridadesPendientes.remove(canalId);

            // Consultar si existe una orden de intervención.
            RedCanalesDrenaje.OrdenIntervencion ordenAnterior =
                    entorno.obtenerOrden(canalId);

            if (ordenAnterior == null) {

                System.out.println(
                        "[PLANIFICADOR] No existe una orden activa para "
                                + canalId
                );

                return;
            }

            // Liberar el recurso reservado, si existe.
            String recursoAnterior = ordenAnterior.recursoId();

            if (!recursoAnterior.equals("NINGUNO")) {

                entorno.liberarRecurso(recursoAnterior);

                System.out.println(
                        "[PLANIFICADOR] Recurso liberado: "
                                + recursoAnterior
                                + " | Canal: " + canalId
                );
            }

            // Suspender la orden por falta de información.
            RedCanalesDrenaje.OrdenIntervencion ordenSuspendida =
                    new RedCanalesDrenaje.OrdenIntervencion(
                            ordenAnterior.id(),
                            canalId,
                            ordenAnterior.prioridad(),
                            "SUSPENDIDA",
                            "NINGUNO",
                            "Intervención suspendida por falta de datos del sensor."
                    );

            entorno.actualizarOrden(ordenSuspendida);

            System.out.println(
                    "[PLANIFICADOR] ORDEN SUSPENDIDA"
                            + " | Canal: " + canalId
                            + " | Recurso liberado: " + recursoAnterior
            );

            // =========================================
            // SENSOR DISPONIBLE
            // =========================================

        } else if (estado.equals("DISPONIBLE")) {

            System.out.println(
                    "[PLANIFICADOR] Sensor recuperado en "
                            + canalId
            );

            System.out.println(
                    "[PLANIFICADOR] Esperando una nueva predicción "
                            + "para reactivar la planificación."
            );

        } else {

            System.out.println(
                    "[PLANIFICADOR] Estado de sensor desconocido: "
                            + estado
            );
        }
    }

    @Override
    protected void takeDown() {
        System.out.println("[PLANIFICADOR] Agente finalizado.");
    }
}
