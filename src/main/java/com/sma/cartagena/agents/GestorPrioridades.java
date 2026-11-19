
package com.sma.cartagena.agents;

import com.sma.cartagena.environment.RedCanalesDrenaje;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

public class GestorPrioridades extends Agent {

    @Override
    protected void setup() {

        System.out.println("[GESTOR] Agente iniciado.");
        System.out.println("[GESTOR] Preparado para gestionar prioridades.");

        addBehaviour(new CyclicBehaviour(this) {

            @Override
            public void action() {

                ACLMessage mensaje = receive();

                if (mensaje == null) {
                    block();
                    return;
                }

                String conversacion = mensaje.getConversationId();

                if (conversacion == null) {
                    return;
                }

                switch (conversacion) {

                    case "prediccion-inundacion" ->
                            procesarPrediccion(mensaje);

                    case "estado-sensor" ->
                            procesarEstadoSensor(mensaje);

                    default -> System.out.println(
                            "[GESTOR] Mensaje no reconocido: "
                                    + conversacion
                    );
                }
            }
        });
    }

    // ==================================================
    // 1. PROCESAR PREDICCIONES
    // ==================================================

    private void procesarPrediccion(ACLMessage mensaje) {

        try {

            String[] datos = mensaje.getContent().split("\\|");

            if (datos.length != 5) {
                System.out.println(
                        "[GESTOR] Predicción recibida con formato inválido."
                );
                return;
            }

            String canalId = datos[0];
            String nivelRiesgo = datos[1];
            double indiceRiesgo = Double.parseDouble(datos[2]);
            double nivelEstimado = Double.parseDouble(datos[3]);
            int horizonte = Integer.parseInt(datos[4]);

            System.out.println(
                    "\n[GESTOR] PREDICCIÓN ACL RECIBIDA"
            );

            System.out.println(
                    "[GESTOR] Remitente: "
                            + mensaje.getSender().getLocalName()
            );

            System.out.println(
                    "[GESTOR] Canal: " + canalId
                            + " | Riesgo: " + nivelRiesgo
                            + " | Índice: "
                            + String.format("%.2f", indiceRiesgo)
            );

            String codigoPrioridad;
            String descripcion;

            switch (nivelRiesgo) {

                case "ALTO" -> {
                    codigoPrioridad = "P1";
                    descripcion = "Atención crítica inmediata";
                }

                case "MODERADO" -> {
                    codigoPrioridad = "P2";
                    descripcion = "Atención prioritaria";
                }

                default -> {
                    codigoPrioridad = "P3";
                    descripcion = "Atención preventiva";
                }
            }

            RedCanalesDrenaje.Prioridad prioridad =
                    new RedCanalesDrenaje.Prioridad(
                            canalId,
                            codigoPrioridad,
                            nivelRiesgo,
                            indiceRiesgo,
                            descripcion
                    );

            RedCanalesDrenaje entorno =
                    RedCanalesDrenaje.getInstancia();

            entorno.registrarOActualizarPrioridad(prioridad);

            System.out.println(
                    "[GESTOR] PRIORIDAD ASIGNADA"
                            + " | Canal: " + canalId
                            + " | Prioridad: " + codigoPrioridad
                            + " | Riesgo: " + nivelRiesgo
                            + " | Índice: "
                            + String.format("%.2f", indiceRiesgo)
            );

            System.out.println(
                    "[GESTOR] Horizonte: " + horizonte
                            + " min | Agua estimada: "
                            + String.format("%.2f", nivelEstimado)
                            + "%"
            );

            enviarPrioridadPlanificador(prioridad);

        } catch (NumberFormatException e) {

            System.out.println(
                    "[GESTOR] Error al interpretar los datos numéricos."
            );

        } catch (Exception e) {

            System.out.println(
                    "[GESTOR] Error procesando predicción: "
                            + e.getMessage()
            );
        }
    }

    // ==================================================
    // 2. PROCESAR ESTADO DEL SENSOR
    // ==================================================

    private void procesarEstadoSensor(ACLMessage mensaje) {

        try {

            String[] datos = mensaje.getContent().split("\\|");

            if (datos.length != 2) {
                System.out.println(
                        "[GESTOR] Estado del sensor con formato inválido."
                );
                return;
            }

            String canalId = datos[0];
            String estado = datos[1];

            RedCanalesDrenaje entorno =
                    RedCanalesDrenaje.getInstancia();

            if ("SIN_DATOS".equals(estado)) {

                boolean eliminada =
                        entorno.eliminarPrioridad(canalId);

                System.out.println(
                        "\n[GESTOR] SENSOR NO DISPONIBLE"
                );

                System.out.println(
                        "[GESTOR] Canal: " + canalId
                );

                if (eliminada) {
                    System.out.println(
                            "[GESTOR] Prioridad anterior eliminada."
                    );
                }

            } else if ("DISPONIBLE".equals(estado)) {

                System.out.println(
                        "\n[GESTOR] SENSOR RECUPERADO"
                );

                System.out.println(
                        "[GESTOR] Canal: " + canalId
                );

                System.out.println(
                        "[GESTOR] Esperando una nueva predicción."
                );

            } else {

                System.out.println(
                        "[GESTOR] Estado de sensor desconocido: "
                                + estado
                );

                return;
            }

            // Informar al Planificador del cambio de estado
            enviarEstadoSensorPlanificador(canalId, estado);

        } catch (Exception e) {

            System.out.println(
                    "[GESTOR] Error procesando estado del sensor: "
                            + e.getMessage()
            );
        }
    }

    // ==================================================
    // 3. ENVIAR PRIORIDAD AL PLANIFICADOR
    // ==================================================

    private void enviarPrioridadPlanificador(
            RedCanalesDrenaje.Prioridad prioridad
    ) {

        ACLMessage mensajePrioridad =
                new ACLMessage(ACLMessage.INFORM);

        mensajePrioridad.addReceiver(
                new AID("Planificador", AID.ISLOCALNAME)
        );

        mensajePrioridad.setConversationId(
                "prioridad-intervencion"
        );

        mensajePrioridad.setContent(
                prioridad.canalId() + "|"
                        + prioridad.codigoPrioridad() + "|"
                        + prioridad.nivelRiesgo() + "|"
                        + prioridad.indiceRiesgo() + "|"
                        + prioridad.descripcion()
        );

        send(mensajePrioridad);

        System.out.println(
                "[GESTOR] Prioridad enviada al Planificador: "
                        + prioridad.canalId()
        );
    }

    // ==================================================
    // 4. NOTIFICAR ESTADO DEL SENSOR AL PLANIFICADOR
    // ==================================================

    private void enviarEstadoSensorPlanificador(
            String canalId,
            String estado
    ) {

        ACLMessage mensaje = new ACLMessage(ACLMessage.INFORM);

        mensaje.addReceiver(
                new AID("Planificador", AID.ISLOCALNAME)
        );

        mensaje.setConversationId("estado-sensor");

        mensaje.setContent(canalId + "|" + estado);

        send(mensaje);

        System.out.println(
                "[GESTOR] Estado del sensor enviado al Planificador: "
                        + canalId + " | " + estado
        );
    }

    @Override
    protected void takeDown() {
        System.out.println("[GESTOR] Agente finalizado.");
    }
}
