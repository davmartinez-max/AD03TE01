package ad03te01bis;

import org.vibur.dbcp.ViburDBCPDataSource;
import org.vibur.dbcp.ViburDBCPException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Ejercicio 05 - Tema: Gestión de Conexiones y Pooling con Vibur DBCP y HSQLDB
 * Programa que demuestra la configuración, arranque e inicialización de un pool de conexiones 
 * (Connection Pooling) utilizando Vibur DBCP contra una base de datos embebida HSQLDB,
 * ejecutando operaciones DDL, DML y consultas sobre él.
 */
public class Ejercicio05 {
    public static void main(String[] args) {
        // Creación e inicialización del DataSource de Vibur DBCP dentro de un bloque try-with-resources
        // para asegurar la liberación del pool al finalizar la ejecución.
        try (ViburDBCPDataSource ds = new ViburDBCPDataSource()) {
            // Configuración de los parámetros del driver JDBC y la URL de conexión para HSQLDB (modo fichero local)
            ds.setDriverClassName("org.hsqldb.jdbc.JDBCDriver");
            ds.setJdbcUrl("jdbc:hsqldb:file:data/testdb");
            ds.setUsername("SA");
            ds.setPassword("");
            
            // Configuración del tamaño inicial y máximo del pool de conexiones
            ds.setPoolInitialSize(2);
            ds.setPoolMaxSize(10);
            
            // Iniciar explícitamente el pool de conexiones antes de solicitar ninguna conexión
            ds.start();

            // Solicitud de una conexión directamente al pool gestionado por Vibur
            try (Connection conn = ds.getConnection()) {
                
                // Validación del estado de la conexión mediante un timeout de 2 segundos
                boolean esValida = conn.isValid(2);
                System.out.println("Conexión válida: " + esValida);

                if (esValida) {
                    // Operación DDL: Creación de una tabla auxiliar 'comprobacion' si no existe previamente
                    try (Statement stmt = conn.createStatement()) {
                        stmt.execute("CREATE TABLE IF NOT EXISTS comprobacion (" +
                                     "id INT PRIMARY KEY, " +
                                     "mensaje VARCHAR(100))");
                    }

                    // Limpieza previa del registro (id = 1) para garantizar la idempotencia de la prueba
                    try (Statement stmt = conn.createStatement()) {
                        stmt.execute("DELETE FROM comprobacion WHERE id = 1");
                    }

                    // Operación DML: Inserción segura de un registro utilizando PreparedStatement
                    String insertSQL = "INSERT INTO comprobacion (id, mensaje) VALUES (?, ?)";
                    try (PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {
                        pstmt.setInt(1, 1);
                        pstmt.setString(2, "Conexión HSQLDB mediante Vibur correcta");
                        pstmt.executeUpdate();
                    }

                    // Operación DQL: Consulta y recuperación de la fila insertada para verificar el funcionamiento
                    String selectSQL = "SELECT id, mensaje FROM comprobacion WHERE id = 1";
                    try (Statement stmt = conn.createStatement();
                         ResultSet rs = stmt.executeQuery(selectSQL)) {
                        if (rs.next()) {
                            System.out.println(rs.getInt("id") + " - " + rs.getString("mensaje"));
                        }
                    }
                }

            } catch (Exception e) {
                // Captura de errores durante la ejecución de las sentencias SQL o la gestión de la conexión del pool
                System.err.println("Error en HSQLDB con Vibur: " + e.getMessage());
            } finally {
                // Apagado y destrucción controlada del pool de conexiones para liberar los recursos del sistema
                ds.terminate();
            }
        } catch (ViburDBCPException e) {
            // Captura específica de excepciones en la inicialización o configuración del pool Vibur
            e.printStackTrace();
        }
    }
}