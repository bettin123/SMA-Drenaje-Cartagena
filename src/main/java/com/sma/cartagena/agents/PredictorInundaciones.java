
package com.sma.cartagena.agents;

import com.sma.cartagena.environment.RedCanalesDrenaje;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;

public class PredictorInundaciones extends Agent {

    private static final int HORIZONTE_MINUTOS = 45;

    @Override
    protected void setup() {

        System.out.println("[PREDICTOR] Agente iniciado.");
        System.out.println("[PREDICTOR] Preparado para evaluar riesgos.");

        addBehaviour(new TickerBehaviour(this, 10000) {

            @Override
            protected void onTick() {

                RedCanalesDrenaje entorno =
                        RedCanalesDrenaje.getInstancia();

                System.out.println(
                        "\n[PREDICTOR] Evaluando riesgos de inundación..."
                );

                for (RedCanalesDrenaje.Canal canal :
                        entorno.obtenerCanales()) {

                    // Verificar disponibilidad del sensor
                    if (!canal.sensorDisponible()) {

                        boolean eliminada =
                                entorno.eliminarPrediccion(canal.id());

                        System.out.println(
                                "[PREDICTOR] Sensor sin datos en "
                                        + canal.id()
                        );

                        if (eliminada) {
                            System.out.println(
                                    "[PREDICTOR] Predicción anterior eliminada: "
                                            + canal.id()
                            );
                        }

                        continue;
                    }

                    // Estimar aumento del nivel de agua
                    double incremento =
                            (canal.obstruccion() / 100.0) * 15.0;

                    double nivelEstimado = Math.min(
                            100.0,
                            canal.nivelAgua() + incremento
                    );

                    // Calcular índice de riesgo
                    double indiceRiesgo =
                            (nivelEstimado * 0.60)
                                    + (canal.obstruccion() * 0.40);

                    // Clasificar riesgo
                    String nivelRiesgo;

                    if (indiceRiesgo >= 70) {
                        nivelRiesgo = "ALTO";
                    } else if (indiceRiesgo >= 40) {
                        nivelRiesgo = "MODERADO";
                    } else {
                        nivelRiesgo = "BAJO";
                    }

                    String descripcion =
                            "Riesgo " + nivelRiesgo.toLowerCase()
                                    + " estimado para los próximos "
                                    + HORIZONTE_MINUTOS + " minutos.";

                    // Crear predicción
                    RedCanalesDrenaje.Prediccion prediccion =
                            new RedCanalesDrenaje.Prediccion(
                                    canal.id(),
                                    indiceRiesgo,
                                    nivelRiesgo,
                                    nivelEstimado,
                                    HORIZONTE_MINUTOS,
                                    descripcion
                            );

                    // Registrar predicción en el entorno compartido
                    entorno.registrarPrediccion(prediccion);

                    System.out.println(
                            "[PREDICTOR] Canal: " + canal.id()
                                    + " | Riesgo: " + nivelRiesgo
                                    + " | Índice: "
                                    + String.format("%.2f", indiceRiesgo)
                                    + " | Agua estimada: "
                                    + String.format("%.2f", nivelEstimado)
                                    + "%"
                                    + " | Horizonte: "
                                    + HORIZONTE_MINUTOS + " min"
                    );

                    // Enviar predicción directamente al Gestor
                    enviarPrediccionACL(prediccion);
                }
            }
        });
    }

    private void enviarPrediccionACL(
            RedCanalesDrenaje.Prediccion prediccion
    ) {

        ACLMessage mensaje = new ACLMessage(ACLMessage.INFORM);

        // Nombre local registrado en Main.java
        mensaje.addReceiver(
                new AID("Gestor", AID.ISLOCALNAME)
        );

        mensaje.setConversationId("prediccion-inundacion");

        mensaje.setContent(
                prediccion.canalId() + "|"
                        + prediccion.nivelRiesgo() + "|"
                        + prediccion.indiceRiesgo() + "|"
                        + prediccion.nivelAguaEstimado() + "|"
                        + prediccion.horizonteMinutos()
        );

        send(mensaje);

        System.out.println(
                "[PREDICTOR] Predicción ACL enviada a GESTOR: "
                        + prediccion.canalId()
                        + " | " + prediccion.nivelRiesgo()
        );
    }

    @Override
    protected void takeDown() {
        System.out.println("[PREDICTOR] Agente finalizado.");
    }
}
