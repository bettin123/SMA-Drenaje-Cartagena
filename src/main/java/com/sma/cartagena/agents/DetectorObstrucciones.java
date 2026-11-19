
package com.sma.cartagena.agents;

import com.sma.cartagena.environment.RedCanalesDrenaje;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DetectorObstrucciones extends Agent {

    private static final double UMBRAL_MODERADO = 40.0;
    private static final double UMBRAL_CRITICO = 70.0;

    private final Map<String, String> nivelesEnviados =
            new HashMap<>();

    // Canales cuyos sensores ya fueron reportados como inactivos.
    private final Set<String> sensoresSinDatos =
            new HashSet<>();

    @Override
    protected void setup() {

        System.out.println("[DETECTOR] Agente iniciado.");
        System.out.println("[DETECTOR] Iniciando monitoreo de obstrucciones.");

        addBehaviour(new TickerBehaviour(this, 4000) {

            @Override
            protected void onTick() {

                RedCanalesDrenaje entorno =
                        RedCanalesDrenaje.getInstancia();

                System.out.println("\n[DETECTOR] Analizando canales...");

                for (RedCanalesDrenaje.Canal canal :
                        entorno.obtenerCanales()) {

                    // =========================================
                    // SENSOR NO DISPONIBLE
                    // =========================================

                    if (!canal.sensorDisponible()) {

                        // Eliminar alerta anterior: ya no es confiable.
                        entorno.resolverAlerta(canal.id());

                        nivelesEnviados.remove(canal.id());

                        // Notificar solamente cuando cambia el estado.
                        if (sensoresSinDatos.add(canal.id())) {

                            System.out.println(
                                    "[DETECTOR] SENSOR SIN DATOS: "
                                            + canal.id()
                            );

                            enviarEstadoSensorACL(
                                    canal.id(),
                                    "SIN_DATOS"
                            );
                        }

                        continue;
                    }

                    // =========================================
                    // SENSOR RECUPERADO
                    // =========================================

                    if (sensoresSinDatos.remove(canal.id())) {

                        System.out.println(
                                "[DETECTOR] Sensor recuperado: "
                                        + canal.id()
                        );

                        enviarEstadoSensorACL(
                                canal.id(),
                                "DISPONIBLE"
                        );
                    }

                    // =========================================
                    // DETECCIÓN DE OBSTRUCCIONES
                    // =========================================

                    double obstruccion = canal.obstruccion();

                    if (obstruccion >= UMBRAL_CRITICO) {

                        registrarAlerta(entorno, canal, "CRITICA");

                    } else if (obstruccion >= UMBRAL_MODERADO) {

                        registrarAlerta(entorno, canal, "MODERADA");

                    } else {

                        boolean resuelta =
                                entorno.resolverAlerta(canal.id());

                        if (resuelta) {

                            nivelesEnviados.remove(canal.id());

                            System.out.println(
                                    "[DETECTOR] Obstrucción resuelta en "
                                            + canal.id()
                            );
                        }
                    }
                }
            }
        });
    }

    // ==================================================
    // 1. REGISTRAR ALERTAS
    // ==================================================

    private void registrarAlerta(
            RedCanalesDrenaje entorno,
            RedCanalesDrenaje.Canal canal,
            String nivel
    ) {

        String descripcion =
                "Obstrucción " + nivel.toLowerCase()
                        + " detectada en " + canal.id();

        boolean nueva =
                entorno.registrarOActualizarAlerta(
                        canal.id(),
                        nivel,
                        canal.obstruccion(),
                        descripcion
                );

        if (nueva) {

            System.out.println(
                    "[DETECTOR] ALERTA " + nivel
                            + " | Canal: " + canal.id()
                            + " | Obstrucción: "
                            + canal.obstruccion() + "%"
            );
        }

        String nivelAnterior = nivelesEnviados.get(canal.id());

        if (nueva || !nivel.equals(nivelAnterior)) {

            enviarAlertaACL(canal, nivel);

            nivelesEnviados.put(canal.id(), nivel);
        }
    }

    // ==================================================
    // 2. ENVIAR ALERTA AL MONITOR
    // ==================================================

    private void enviarAlertaACL(
            RedCanalesDrenaje.Canal canal,
            String nivel
    ) {

        ACLMessage mensaje = new ACLMessage(ACLMessage.INFORM);

        mensaje.addReceiver(
                new AID("Monitor", AID.ISLOCALNAME)
        );

        mensaje.setConversationId("alerta-obstruccion");

        mensaje.setContent(
                canal.id() + "|"
                        + nivel + "|"
                        + canal.obstruccion()
        );

        send(mensaje);

        System.out.println(
                "[DETECTOR] Alerta ACL enviada a MONITOR: "
                        + canal.id() + " | " + nivel
        );
    }

    // ==================================================
    // 3. ENVIAR ESTADO DEL SENSOR
    // ==================================================

    private void enviarEstadoSensorACL(
            String canalId,
            String estado
    ) {

        ACLMessage mensaje = new ACLMessage(ACLMessage.INFORM);

        // Notificar al Monitor.
        mensaje.addReceiver(
                new AID("Monitor", AID.ISLOCALNAME)
        );

        // NUEVO: notificar también al Gestor.
        mensaje.addReceiver(
                new AID("Gestor", AID.ISLOCALNAME)
        );

        mensaje.setConversationId("estado-sensor");

        mensaje.setContent(
                canalId + "|" + estado
        );

        send(mensaje);

        System.out.println(
                "[DETECTOR] Estado del sensor enviado a MONITOR y GESTOR: "
                        + canalId + " | " + estado
        );
    }

    @Override
    protected void takeDown() {
        System.out.println("[DETECTOR] Agente finalizado.");
    }
}
