package ad03te01;

import java.sql.*;
import java.util.Scanner;

/**
 * Ejercicio 02 - Tema: Acceso a Bases de Datos con JDBC
 * Programa interactivo que permite consultar y actualizar un registro (nombre de alumno) 
 * utilizando PreparedStatement para garantizar la seguridad frente a inyecciones SQL.
 */
public class Ejercicio02 {
    
    // Parámetros de conexión a la base de datos MySQL
    private static final String URL = "jdbc:mysql://localhost:3306/dbformacion?serverTimezone=UTC";
    private static final String USER = "user";
    private static final String PASS = "user";

    public static void main(String[] args) {
        // Inicialización del Scanner para la lectura de datos introducidos por el usuario a través de consola
        Scanner sc = new Scanner(System.in);
        System.out.print("Introduce el DNI del alumno: ");
        String dni = sc.nextLine().trim();

        // Definición de las sentencias SQL parametrizadas (? como comodín) para consulta y actualización
        String sqlSelect = "SELECT nombre FROM alumnos WHERE dni = ?";
        String sqlUpdate = "UPDATE alumnos SET nombre = ? WHERE dni = ?";

        // Bloque try-with-resources anidado para la conexión y el PreparedStatement de selección,
        // asegurando la liberación automática de recursos en caso de éxito o error.
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             PreparedStatement pstmtSelect = conn.prepareStatement(sqlSelect)) {

            // Asignación del parámetro DNI en la consulta de selección
            pstmtSelect.setString(1, dni);
            
            try (ResultSet rs = pstmtSelect.executeQuery()) {
                // Validación de la existencia del registro en la base de datos
                if (!rs.next()) {
                    System.out.println("El DNI introducido no existe en la base de datos.");
                    return; // Salida temprana del programa si no se encuentra el alumno
                }

                // Recuperación y visualización del estado actual del registro
                String nombreActual = rs.getString("nombre");
                System.out.println("Nombre actual: " + nombreActual);

                // Solicitud del nuevo valor al usuario
                System.out.print("Nuevo nombre (deja en blanco para no modificar): ");
                String nuevoNombre = sc.nextLine().trim();

                // Validación de negocio: si no se introduce nada, se aborta la actualización
                if (nuevoNombre.isEmpty()) {
                    System.out.println("No se ha realizado ninguna modificación.");
                    return;
                }

                // Creación de un nuevo PreparedStatement específico para la operación de actualización (DML)
                try (PreparedStatement pstmtUpdate = conn.prepareStatement(sqlUpdate)) {
                    // Mapeo de parámetros: 1º el nuevo nombre, 2º el DNI del alumno a modificar
                    pstmtUpdate.setString(1, nuevoNombre);
                    pstmtUpdate.setString(2, dni);
                    
                    // Ejecución de la sentencia de actualización (executeUpdate devuelve las filas afectadas)
                    pstmtUpdate.executeUpdate();
                    System.out.println("Nombre actualizado correctamente.");
                }
            }

        } catch (SQLException e) {
            // Captura de errores relacionados con el driver, la conexión o fallos de ejecución SQL
            System.err.println("Error en la base de datos: " + e.getMessage());
        } finally {
            // Cierre explícito del recurso Scanner para evitar fugas de recursos del sistema de entrada/salida
            sc.close();
        }
    }
}