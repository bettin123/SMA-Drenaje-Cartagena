
package com.sma.cartagena.environment;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RedCanalesDrenaje {

    private static final RedCanalesDrenaje INSTANCIA =
            new RedCanalesDrenaje();

    private final Map<String, Canal> canales = new LinkedHashMap<>();
    private final Map<String, Alerta> alertas = new LinkedHashMap<>();
    private final Map<String, Prediccion> predicciones = new LinkedHashMap<>();
    private final Map<String, Prioridad> prioridades = new LinkedHashMap<>();
    private final Map<String, Recurso> recursos = new LinkedHashMap<>();
    private final Map<String, Boolean> disponibilidadRecursos =
            new LinkedHashMap<>();
    private final Map<String, OrdenIntervencion> ordenes =
            new LinkedHashMap<>();

    private RedCanalesDrenaje() {
    }

    public static RedCanalesDrenaje getInstancia() {
        return INSTANCIA;
    }

    // ==================================================
    // 1. MODELOS DE DATOS
    // ==================================================

    public record Canal(
            String id,
            double nivelAgua,
            double caudal,
            double obstruccion,
            boolean sensorDisponible
    ) {
    }

    public record Alerta(
            String id,
            String canalId,
            String nivel,
            double obstruccion,
            String descripcion
    ) {
    }

    public record Prediccion(
            String canalId,
            double indiceRiesgo,
            String nivelRiesgo,
            double nivelAguaEstimado,
            int horizonteMinutos,
            String descripcion
    ) {
    }

    public record Prioridad(
            String canalId,
            String codigoPrioridad,
            String nivelRiesgo,
            double indiceRiesgo,
            String descripcion
    ) {
    }

    public record Recurso(String id, String tipo) {
    }

    public record OrdenIntervencion(
            String id,
            String canalId,
            String prioridad,
            String decision,
            String recursoId,
            String motivo
    ) {
    }

    // ==================================================
    // 2. INICIALIZACIÓN
    // ==================================================

    public synchronized void inicializarDatosDemo() {

        canales.clear();
        alertas.clear();
        predicciones.clear();
        prioridades.clear();
        recursos.clear();
        disponibilidadRecursos.clear();
        ordenes.clear();

        canales.put("CAN-001",
                new Canal("CAN-001", 35.0, 8.0, 10.0, true));

        canales.put("CAN-002",
                new Canal("CAN-002", 72.0, 5.2, 65.0, true));

        canales.put("CAN-003",
                new Canal("CAN-003", 90.0, 3.1, 88.0, true));

        Recurso cuadrilla = new Recurso(
                "CUADRILLA-01",
                "LIMPIEZA_CANAL"
        );

        recursos.put(cuadrilla.id(), cuadrilla);
        disponibilidadRecursos.put(cuadrilla.id(), true);

        System.out.println("[ENTORNO] Red de drenaje inicializada.");
        System.out.println("[ENTORNO] Recursos disponibles: 1 cuadrilla.");
    }

    // ==================================================
    // 3. GESTIÓN DE CANALES
    // ==================================================

    public synchronized List<Canal> obtenerCanales() {
        return List.copyOf(canales.values());
    }

    public synchronized Canal obtenerCanal(String id) {
        return canales.get(id);
    }

    public synchronized void actualizarLectura(
            String id,
            double nivelAgua,
            double caudal,
            double obstruccion,
            boolean sensorDisponible
    ) {

        if (!canales.containsKey(id)) {
            throw new IllegalArgumentException(
                    "Canal inexistente: " + id
            );
        }

        validarPorcentaje(nivelAgua, "Nivel de agua");
        validarPorcentaje(obstruccion, "Obstrucción");

        if (!Double.isFinite(caudal) || caudal < 0) {
            throw new IllegalArgumentException(
                    "El caudal debe ser válido y no negativo."
            );
        }

        canales.put(id, new Canal(
                id, nivelAgua, caudal, obstruccion, sensorDisponible
        ));
    }

    public synchronized void agregarCanal(
            String id,
            double nivelAgua,
            double caudal,
            double obstruccion,
            boolean sensorDisponible
    ) {

        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException(
                    "El identificador del canal no puede estar vacío."
            );
        }

        if (canales.containsKey(id)) {
            throw new IllegalArgumentException(
                    "Ya existe un canal con el identificador: " + id
            );
        }

        validarPorcentaje(nivelAgua, "Nivel de agua");
        validarPorcentaje(obstruccion, "Obstrucción");

        if (!Double.isFinite(caudal) || caudal < 0) {
            throw new IllegalArgumentException(
                    "El caudal debe ser válido y no negativo."
            );
        }

        canales.put(id, new Canal(
                id, nivelAgua, caudal, obstruccion, sensorDisponible
        ));

        System.out.println("[ENTORNO] Nuevo canal agregado: " + id);
    }

    private void validarPorcentaje(double valor, String campo) {

        if (!Double.isFinite(valor) || valor < 0 || valor > 100) {
            throw new IllegalArgumentException(
                    campo + " debe estar entre 0 y 100."
            );
        }
    }

    // ==================================================
    // 4. GESTIÓN DE ALERTAS
    // ==================================================

    public synchronized boolean registrarOActualizarAlerta(
            String canalId,
            String nivel,
            double obstruccion,
            String descripcion
    ) {

        if (!canales.containsKey(canalId)) {
            throw new IllegalArgumentException(
                    "No existe el canal: " + canalId
            );
        }

        validarPorcentaje(obstruccion, "Obstrucción");

        Alerta anterior = alertas.get(canalId);

        if (anterior != null
                && anterior.nivel().equals(nivel)
                && Double.compare(
                anterior.obstruccion(), obstruccion) == 0) {
            return false;
        }

        alertas.put(canalId, new Alerta(
                "ALT-" + canalId,
                canalId,
                nivel,
                obstruccion,
                descripcion
        ));

        return true;
    }

    public synchronized List<Alerta> obtenerAlertas() {
        return List.copyOf(alertas.values());
    }

    public synchronized boolean resolverAlerta(String canalId) {
        return alertas.remove(canalId) != null;
    }

    // ==================================================
    // 5. GESTIÓN DE PREDICCIONES
    // ==================================================

    public synchronized void registrarPrediccion(
            Prediccion prediccion
    ) {

        if (!canales.containsKey(prediccion.canalId())) {
            throw new IllegalArgumentException(
                    "Canal inexistente: " + prediccion.canalId()
            );
        }

        predicciones.put(prediccion.canalId(), prediccion);
    }

    public synchronized List<Prediccion> obtenerPredicciones() {
        return List.copyOf(predicciones.values());
    }

    public synchronized Prediccion obtenerPrediccion(String canalId) {
        return predicciones.get(canalId);
    }

    // NUEVO: eliminar predicción obsoleta
    public synchronized boolean eliminarPrediccion(String canalId) {
        return predicciones.remove(canalId) != null;
    }

    // ==================================================
    // 6. GESTIÓN DE PRIORIDADES
    // ==================================================

    public synchronized boolean registrarOActualizarPrioridad(
            Prioridad prioridad
    ) {

        if (!canales.containsKey(prioridad.canalId())) {
            throw new IllegalArgumentException(
                    "Canal inexistente: " + prioridad.canalId()
            );
        }

        Prioridad anterior = prioridades.get(prioridad.canalId());

        if (anterior != null
                && anterior.codigoPrioridad().equals(
                prioridad.codigoPrioridad())
                && Double.compare(
                anterior.indiceRiesgo(),
                prioridad.indiceRiesgo()) == 0) {
            return false;
        }

        prioridades.put(prioridad.canalId(), prioridad);
        return true;
    }

    public synchronized List<Prioridad> obtenerPrioridades() {
        return prioridades.values().stream()
                .sorted(Comparator.comparingDouble(
                        Prioridad::indiceRiesgo).reversed())
                .toList();
    }

    public synchronized Prioridad obtenerPrioridad(String canalId) {
        return prioridades.get(canalId);
    }

    // NUEVO: eliminar prioridad obsoleta
    public synchronized boolean eliminarPrioridad(String canalId) {
        return prioridades.remove(canalId) != null;
    }

    // ==================================================
    // 7. GESTIÓN DE RECURSOS
    // ==================================================

    public synchronized List<Recurso> obtenerRecursosDisponibles() {

        return recursos.values().stream()
                .filter(recurso ->
                        disponibilidadRecursos.getOrDefault(
                                recurso.id(), false))
                .toList();
    }

    public synchronized Recurso reservarRecurso(String tipo) {

        for (Recurso recurso : recursos.values()) {

            boolean disponible =
                    disponibilidadRecursos.getOrDefault(
                            recurso.id(), false);

            if (disponible && recurso.tipo().equals(tipo)) {
                disponibilidadRecursos.put(recurso.id(), false);
                return recurso;
            }
        }

        return null;
    }

    public synchronized boolean liberarRecurso(String recursoId) {

        if (!recursos.containsKey(recursoId)) {
            return false;
        }

        disponibilidadRecursos.put(recursoId, true);
        return true;
    }

    // ==================================================
    // 8. GESTIÓN DE ÓRDENES
    // ==================================================

    public synchronized boolean registrarOrden(
            OrdenIntervencion orden
    ) {

        if (!canales.containsKey(orden.canalId())) {
            throw new IllegalArgumentException(
                    "Canal inexistente: " + orden.canalId()
            );
        }

        if (ordenes.containsKey(orden.canalId())) {
            return false;
        }

        ordenes.put(orden.canalId(), orden);
        return true;
    }

    public synchronized boolean actualizarOrden(
            OrdenIntervencion orden
    ) {

        if (!canales.containsKey(orden.canalId())) {
            throw new IllegalArgumentException(
                    "Canal inexistente: " + orden.canalId()
            );
        }

        ordenes.put(orden.canalId(), orden);
        return true;
    }

    public synchronized OrdenIntervencion obtenerOrden(String canalId) {
        return ordenes.get(canalId);
    }

    public synchronized List<OrdenIntervencion> obtenerOrdenes() {
        return List.copyOf(ordenes.values());
    }

    // ==================================================
    // 9. REINICIAR RESULTADOS
    // ==================================================

    public synchronized void reiniciarResultadosSimulacion() {

        alertas.clear();
        predicciones.clear();
        prioridades.clear();
        ordenes.clear();

        for (String recursoId : recursos.keySet()) {
            disponibilidadRecursos.put(recursoId, true);
        }

        System.out.println(
                "[ENTORNO] Resultados de simulación reiniciados."
        );

        System.out.println(
                "[ENTORNO] Canales y lecturas conservados."
        );

        System.out.println(
                "[ENTORNO] Recursos liberados: "
                        + obtenerRecursosDisponibles().size()
        );
    }

    // ==================================================
    // 10. VISUALIZACIÓN
    // ==================================================

    public synchronized void mostrarEstadoRed() {

        System.out.println("\n========== ESTADO DE LA RED ==========");

        for (Canal canal : canales.values()) {

            System.out.println("\nCanal: " + canal.id());
            System.out.println("Nivel de agua: "
                    + canal.nivelAgua() + "%");
            System.out.println("Caudal: "
                    + canal.caudal() + " m3/s");
            System.out.println("Obstrucción: "
                    + canal.obstruccion() + "%");
            System.out.println("Sensor disponible: "
                    + canal.sensorDisponible());
        }

        System.out.println("\n======================================");
    }
}
