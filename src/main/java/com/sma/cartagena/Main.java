
package com.sma.cartagena;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;

import com.sma.cartagena.environment.RedCanalesDrenaje;
import com.sma.cartagena.agents.*;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Scanner;

public class Main {

    private static final Scanner scanner = new Scanner(System.in);

    private static AgentContainer container;
    private static AgentController[] agentes;

    private static final RedCanalesDrenaje entorno =
            RedCanalesDrenaje.getInstancia();

    public static void main(String[] args) {

        configurarConsola();

        try {
            Runtime runtime = Runtime.instance();

            Profile profile = new ProfileImpl();
            profile.setParameter(Profile.GUI, "true");

            container = runtime.createMainContainer(profile);

            System.out.println("======================================");
            System.out.println(" SMA - DRENAJE PLUVIAL CARTAGENA");
            System.out.println("======================================");

            entorno.inicializarDatosDemo();

            menuPrincipal();

        } catch (Exception e) {
            System.err.println("Error al iniciar el SMA:");
            e.printStackTrace();

        } finally {
            detenerAgentes();
            scanner.close();
        }
    }

    // ==================================================
    // 1. CONSOLA EXCLUSIVA PARA EL MENÚ
    // ==================================================

    private static void configurarConsola() {

        PrintStream salidaOriginal = System.out;
        Thread hiloConsola = Thread.currentThread();

        System.setOut(new PrintStream(new OutputStream() {

            @Override
            public void write(int dato) {
                if (Thread.currentThread() == hiloConsola) {
                    salidaOriginal.write(dato);
                }
            }

            @Override
            public void write(byte[] datos, int inicio, int longitud) {
                if (Thread.currentThread() == hiloConsola) {
                    salidaOriginal.write(datos, inicio, longitud);
                }
            }

            @Override
            public void flush() {
                salidaOriginal.flush();
            }

        }, true));
    }

    // ==================================================
    // 2. MENÚ PRINCIPAL
    // ==================================================

    private static void menuPrincipal() {

        boolean salir = false;

        while (!salir) {

            System.out.println("\n======================================");
            System.out.println("       CONFIGURACIÓN DEL SMA");
            System.out.println("======================================");
            System.out.println("1. Consultar canales");
            System.out.println("2. Modificar un canal");
            System.out.println("3. Agregar un canal");
            System.out.println("4. Consultar resultados de simulación");

            if (agentes == null) {
                System.out.println("5. Iniciar simulación");
            } else {
                System.out.println("5. Detener simulación");
            }

            System.out.println("6. Salir");
            System.out.println("--------------------------------------");
            System.out.print("Seleccione una opción: ");

            int opcion = leerEntero();

            switch (opcion) {

                case 1 -> entorno.mostrarEstadoRed();

                case 2 -> modificarCanal();

                case 3 -> agregarCanal();

                case 4 -> mostrarResultados();

                case 5 -> ejecutarSimulacion();

                case 6 -> {
                    detenerAgentes();
                    salir = true;
                    System.out.println("Saliendo del sistema...");
                }

                default -> System.out.println(
                        "Opción inválida. Intente nuevamente."
                );
            }
        }
    }

    // ==================================================
    // 3. MODIFICAR CANAL
    // ==================================================

    private static void modificarCanal() {

        System.out.println("\n========== MODIFICAR CANAL ==========");

        entorno.mostrarEstadoRed();

        System.out.print("\nIngrese el ID del canal: ");
        String id = scanner.nextLine().trim().toUpperCase();

        RedCanalesDrenaje.Canal canal = entorno.obtenerCanal(id);

        if (canal == null) {
            System.out.println("El canal indicado no existe.");
            return;
        }

        try {
            System.out.println("\nIngrese los nuevos valores:");

            double nivelAgua = leerDouble("Nivel de agua (%): ");
            double caudal = leerDouble("Caudal (m3/s): ");
            double obstruccion = leerDouble("Obstrucción (%): ");
            boolean sensor = leerBoolean("¿Sensor disponible? (S/N): ");

            entorno.actualizarLectura(
                    id,
                    nivelAgua,
                    caudal,
                    obstruccion,
                    sensor
            );

            System.out.println(
                    "\n[CONFIGURACIÓN] Canal actualizado correctamente."
            );

            if (agentes != null) {
                System.out.println(
                        "[SMA] Los agentes evaluarán las nuevas lecturas " +
                                "en sus próximos ciclos."
                );
            }

        } catch (IllegalArgumentException e) {
            System.out.println("[ERROR] " + e.getMessage());
        }
    }

    // ==================================================
    // 4. AGREGAR CANAL
    // ==================================================

    private static void agregarCanal() {

        System.out.println("\n========== AGREGAR CANAL ==========");

        try {
            System.out.print("Identificador del canal: ");
            String id = scanner.nextLine().trim().toUpperCase();

            double nivelAgua = leerDouble("Nivel de agua (%): ");
            double caudal = leerDouble("Caudal (m3/s): ");
            double obstruccion = leerDouble("Obstrucción (%): ");
            boolean sensor = leerBoolean("¿Sensor disponible? (S/N): ");

            entorno.agregarCanal(
                    id,
                    nivelAgua,
                    caudal,
                    obstruccion,
                    sensor
            );

            System.out.println(
                    "[CONFIGURACIÓN] Canal agregado correctamente."
            );

        } catch (IllegalArgumentException e) {
            System.out.println("[ERROR] " + e.getMessage());
        }
    }

    // ==================================================
    // 5. CONSULTAR RESULTADOS
    // ==================================================

    private static void mostrarResultados() {

        System.out.println("\n======================================");
        System.out.println("       RESULTADOS DE SIMULACIÓN");
        System.out.println("======================================");

        System.out.println("\n--- ALERTAS ---");

        var alertas = entorno.obtenerAlertas();

        if (alertas.isEmpty()) {
            System.out.println("No hay alertas registradas.");
        }

        for (var alerta : alertas) {
            System.out.println(
                    alerta.canalId()
                            + " | " + alerta.nivel()
                            + " | Obstrucción: " + alerta.obstruccion() + "%"
                            + " | " + alerta.descripcion()
            );
        }

        System.out.println("\n--- PREDICCIONES ---");

        var predicciones = entorno.obtenerPredicciones();

        if (predicciones.isEmpty()) {
            System.out.println("No hay predicciones registradas.");
        }

        for (var prediccion : predicciones) {
            System.out.printf(
                    "%s | Riesgo: %s | Índice: %.2f"
                            + " | Agua estimada: %.2f%%"
                            + " | Horizonte: %d min%n",
                    prediccion.canalId(),
                    prediccion.nivelRiesgo(),
                    prediccion.indiceRiesgo(),
                    prediccion.nivelAguaEstimado(),
                    prediccion.horizonteMinutos()
            );
        }

        System.out.println("\n--- PRIORIDADES ---");

        var prioridades = entorno.obtenerPrioridades();

        if (prioridades.isEmpty()) {
            System.out.println("No hay prioridades registradas.");
        }

        for (var prioridad : prioridades) {
            System.out.printf(
                    "%s | %s | Riesgo: %s | Índice: %.2f%n",
                    prioridad.canalId(),
                    prioridad.codigoPrioridad(),
                    prioridad.nivelRiesgo(),
                    prioridad.indiceRiesgo()
            );
        }

        System.out.println("\n--- ÓRDENES DE INTERVENCIÓN ---");

        var ordenes = entorno.obtenerOrdenes();

        if (ordenes.isEmpty()) {
            System.out.println("No hay órdenes registradas.");
        }

        for (var orden : ordenes) {
            System.out.println(
                    orden.id()
                            + " | Canal: " + orden.canalId()
                            + " | Prioridad: " + orden.prioridad()
                            + " | Decisión: " + orden.decision()
                            + " | Recurso: " + orden.recursoId()
                            + " | Motivo: " + orden.motivo()
            );
        }

        System.out.println("\n======================================");
    }

    // ==================================================
    // 6. INICIAR / DETENER SIMULACIÓN
    // ==================================================

    private static void ejecutarSimulacion() {

        if (agentes != null) {
            detenerAgentes();
            return;
        }

        try {
            entorno.reiniciarResultadosSimulacion();

            System.out.println("\n======================================");
            System.out.println("       INICIANDO SIMULACIÓN");
            System.out.println("======================================");

            entorno.mostrarEstadoRed();

            agentes = new AgentController[5];

            agentes[0] = container.createNewAgent(
                    "Detector",
                    DetectorObstrucciones.class.getName(),
                    null
            );

            agentes[1] = container.createNewAgent(
                    "Monitor",
                    MonitorCanal.class.getName(),
                    null
            );

            agentes[2] = container.createNewAgent(
                    "Predictor",
                    PredictorInundaciones.class.getName(),
                    null
            );

            agentes[3] = container.createNewAgent(
                    "Gestor",
                    GestorPrioridades.class.getName(),
                    null
            );

            agentes[4] = container.createNewAgent(
                    "Planificador",
                    PlanificadorPrevencion.class.getName(),
                    null
            );

            for (AgentController agente : agentes) {
                agente.start();
            }

            System.out.println("\n[SMA] Los cinco agentes están activos.");
            System.out.println(
                    "[SMA] La consola está disponible para modificar canales."
            );

        } catch (Exception e) {

            System.err.println(
                    "[ERROR] No se pudo iniciar la simulación."
            );

            e.printStackTrace();

            detenerAgentes();
        }
    }

    // ==================================================
    // 7. DETENER AGENTES
    // ==================================================

    private static void detenerAgentes() {

        if (agentes == null) {
            return;
        }

        System.out.println("\n[SMA] Deteniendo agentes...");

        for (AgentController agente : agentes) {

            if (agente != null) {
                try {
                    agente.kill();

                } catch (Exception e) {
                    System.out.println(
                            "[SMA] No se pudo detener un agente: "
                                    + e.getMessage()
                    );
                }
            }
        }

        agentes = null;

        System.out.println("[SMA] Simulación detenida.");
    }

    // ==================================================
    // 8. MÉTODOS DE ENTRADA
    // ==================================================

    private static int leerEntero() {

        while (true) {
            try {
                return Integer.parseInt(
                        scanner.nextLine().trim()
                );

            } catch (NumberFormatException e) {
                System.out.print(
                        "Ingrese un número entero válido: "
                );
            }
        }
    }

    private static double leerDouble(String mensaje) {

        while (true) {

            System.out.print(mensaje);

            try {
                String entrada = scanner.nextLine()
                        .trim()
                        .replace(',', '.');

                double valor = Double.parseDouble(entrada);

                if (!Double.isFinite(valor)) {
                    System.out.println("Ingrese un número válido.");
                    continue;
                }

                return valor;

            } catch (NumberFormatException e) {
                System.out.println(
                        "Entrada inválida. Intente nuevamente."
                );
            }
        }
    }

    private static boolean leerBoolean(String mensaje) {

        while (true) {

            System.out.print(mensaje);

            String entrada = scanner.nextLine()
                    .trim()
                    .toUpperCase();

            if (entrada.equals("S")) {
                return true;
            }

            if (entrada.equals("N")) {
                return false;
            }

            System.out.println("Ingrese S o N.");
        }
    }
}
