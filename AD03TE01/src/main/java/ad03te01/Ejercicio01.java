package ad03te01;

import java.sql.*;

/**
 * Ejercicio 01 - Tema: Acceso a Bases de Datos con JDBC
 * Programa que realiza una consulta multitabla (cursos, aulas y matrículas) 
 * utilizando JDBC y muestra los resultados formateados por consola.
 */
public class Ejercicio01 {

    // Parámetros de conexión a la base de datos MySQL (URL, usuario y contraseña)
    private static final String URL = "jdbc:mysql://localhost:3306/dbformacion?serverTimezone=UTC";
    private static final String USER = "user";
    private static final String PASS = "user";

    public static void main(String[] args) {
        // Consulta SQL que asocia cursos con sus aulas y cuenta los matriculados mediante LEFT JOIN,
        // agrupando por los campos necesarios y ordenando cronológicamente por fecha.
        String sql = "SELECT c.id_curso, c.titulo, c.fecha, " +
                     "COUNT(m.dni) AS matriculados, a.capacidad, a.nombre AS aula, a.edificio " +
                     "FROM cursos c " +
                     "JOIN aulas a ON c.id_aula = a.id_aula " +
                     "LEFT JOIN matriculas m ON c.id_curso = m.id_curso " +
                     "GROUP BY c.id_curso, c.titulo, c.fecha, a.capacidad, a.nombre, a.edificio " +
                     "ORDER BY c.fecha ASC";

        // Estructura try-with-resources para asegurar el cierre automático 
        // de la conexión, la sentencia y el conjunto de resultados (evita fugas de memoria).
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            // Impresión de la cabecera de la tabla con formato de ancho fijo
            System.out.printf("%-3s %-25s %-12s %-11s %-25s %-12s%n", "ID", "Curso", "Fecha", "Matrículas", "Aula", "Edificio");
            System.out.println("--------------------------------------------------------------------------------------------------");

            // Iteración sobre el ResultSet para procesar cada fila devuelta por la base de datos
            while (rs.next()) {
                // Mapeo de columnas SQL a variables Java utilizando los alias de la consulta
                int id = rs.getInt("id_curso");
                String titulo = rs.getString("titulo");
                String fecha = rs.getDate("fecha").toString();
                int matriculados = rs.getInt("matriculados");
                int capacidad = rs.getInt("capacidad");
                String aula = rs.getString("aula");
                String edificio = rs.getString("edificio");

                // Presentación formateada de los datos, mostrando plazas ocupadas frente a capacidad total
                System.out.printf("%-3d %-25s %-12s %2d/%-8d %-25s %-12s%n", 
                        id, titulo, fecha, matriculados, capacidad, aula, edificio);
            }

        } catch (SQLException e) {
            // Captura de excepciones SQL para gestionar errores de conexión, sintaxis o ejecución
            System.err.println("Error al consultar los cursos: " + e.getMessage());
        }
    }
}