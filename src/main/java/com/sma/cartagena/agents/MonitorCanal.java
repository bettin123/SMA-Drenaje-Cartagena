package com.sma.cartagena.agents;

import com.sma.cartagena.environment.RedCanalesDrenaje;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;

public class MonitorCanal extends Agent {

    @Override
    protected void setup() {

        System.out.println("[MONITOR] Agente iniciado.");
        System.out.println("[MONITOR] Iniciando monitoreo de canales.");

        // Recepción de mensajes ACL
        addBehaviour(new CyclicBehaviour(this) {

            @Override
            public void action() {

                ACLMessage mensaje = receive();

                if (mensaje != null) {

                    String conversacion = mensaje.getConversationId();
                    String contenido = mensaje.getContent();

                    // =====================================
                    // ALERTA DE OBSTRUCCIÓN
                    // =====================================

                    if ("alerta-obstruccion".equals(conversacion)) {

                        String[] datos = contenido.split("\\|");

                        if (datos.length == 3) {

                            System.out.println(
                                    "\n[MONITOR] ALERTA ACL RECIBIDA"
                            );

                            System.out.println(
                                    "[MONITOR] Remitente: "
                                            + mensaje.getSender().getLocalName()
                            );

                            System.out.println(
                                    "[MONITOR] Canal: " + datos[0]
                                            + " | Nivel: " + datos[1]
                                            + " | Obstrucción: " + datos[2] + "%"
                            );

                        } else {
                            System.out.println(
                                    "[MONITOR] Alerta ACL con formato inválido."
                            );
                        }

                        // =====================================
                        // ESTADO DEL SENSOR
                        // =====================================

                    } else if ("estado-sensor".equals(conversacion)) {

                        String[] datos = contenido.split("\\|");

                        if (datos.length == 2) {

                            System.out.println(
                                    "\n[MONITOR] CAMBIO DE ESTADO DEL SENSOR"
                            );

                            System.out.println(
                                    "[MONITOR] Remitente: "
                                            + mensaje.getSender().getLocalName()
                            );

                            if ("SIN_DATOS".equals(datos[1])) {

                                System.out.println(
                                        "[MONITOR] ¡ATENCIÓN! El sensor del canal "
                                                + datos[0]
                                                + " no está disponible."
                                );

                            } else if ("DISPONIBLE".equals(datos[1])) {

                                System.out.println(
                                        "[MONITOR] Sensor del canal "
                                                + datos[0]
                                                + " recuperado."
                                );

                            } else {

                                System.out.println(
                                        "[MONITOR] Estado de sensor desconocido: "
                                                + datos[1]
                                );
                            }

                        } else {

                            System.out.println(
                                    "[MONITOR] Mensaje de sensor con formato inválido."
                            );
                        }

                    } else {

                        System.out.println(
                                "[MONITOR] Mensaje ACL no reconocido: "
                                        + conversacion
                        );
                    }

                } else {

                    block();
                }
            }
        });

        // Monitoreo periódico de la red
        addBehaviour(new TickerBehaviour(this, 7000) {

            @Override
            protected void onTick() {

                RedCanalesDrenaje entorno =
                        RedCanalesDrenaje.getInstancia();

                System.out.println(
                        "\n[MONITOR] Consultando estado de la red..."
                );

                for (RedCanalesDrenaje.Canal canal :
                        entorno.obtenerCanales()) {

                    String estado;

                    if (!canal.sensorDisponible()) {
                        estado = "SIN DATOS";
                    } else if (canal.nivelAgua() >= 90) {
                        estado = "NIVEL CRITICO";
                    } else if (canal.nivelAgua() >= 70) {
                        estado = "NIVEL ELEVADO";
                    } else {
                        estado = "NORMAL";
                    }

                    System.out.println(
                            "[MONITOR] " + canal.id()
                                    + " | Agua: " + canal.nivelAgua()
                                    + "% | Caudal: " + canal.caudal()
                                    + " m3/s | Estado: " + estado
                    );
                }

                System.out.println("\n[MONITOR] Alertas activas:");

                var alertas = entorno.obtenerAlertas();

                if (alertas.isEmpty()) {
                    System.out.println(
                            "[MONITOR] Sin alertas activas."
                    );
                }

                for (RedCanalesDrenaje.Alerta alerta : alertas) {

                    System.out.println(
                            "[MONITOR] " + alerta.id()
                                    + " | " + alerta.canalId()
                                    + " | Nivel: " + alerta.nivel()
                                    + " | " + alerta.descripcion()
                    );
                }
            }
        });
    }

    @Override
    protected void takeDown() {
        System.out.println("[MONITOR] Agente finalizado.");
    }
}