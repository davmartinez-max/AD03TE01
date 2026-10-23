package ad03te01;

import java.sql.*;
import java.util.Scanner;

/**
 * Ejercicio 04 - Tema: Invocación de Procedimientos y Funciones Almacenadas con JDBC
 * Programa que lista los cursos disponibles y utiliza un objeto CallableStatement 
 * para invocar una función almacenada en la base de datos que calcula el total de matriculados.
 */
public class Ejercicio04 {
    
    // Parámetros de conexión a la base de datos MySQL
    private static finalString URL = "jdbc:mysql://localhost:3306/dbformacion?serverTimezone=UTC";
    private static final String USER = "user";
    private static final String PASS = "user";

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        // Bloque try-with-resources anidado para la conexión, el Statement y el ResultSet del listado de cursos
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id_curso, titulo FROM cursos")) {

            // Muestra por pantalla los cursos existentes para que el usuario elija uno
            System.out.println("Cursos:");
            while (rs.next()) {
                System.out.println(rs.getInt("id_curso") + ". " + rs.getString("titulo"));
            }

            // Solicitud del identificador del curso al usuario
            System.out.print("Introduce el ID del curso: ");
            int idCurso = Integer.parseInt(sc.nextLine().trim());

            // Sintaxis JDBC para invocar funciones almacenadas que devuelven un valor ({? = call nombre_funcion(?)});
            // El primer interrogante (?) representa el valor de retorno (OUT) y el segundo el parámetro de entrada (IN).
            String callSql = "{ ? = call obtener_numero_matriculados(?) }";
            
            // Uso de CallableStatement para la ejecución de rutinas almacenadas en la base de datos
            try (CallableStatement cstmt = conn.prepareCall(callSql)) {
                // Registro del parámetro de salida indicando su posición (1) y el tipo de dato SQL esperado (INTEGER)
                cstmt.registerOutParameter(1, Types.INTEGER);
                
                // Asignación del valor al parámetro de entrada (IN) en la posición 2
                cstmt.setInt(2, idCurso);
                
                // Ejecución de la función almacenada
                cstmt.execute();

                // Recuperación del resultado devuelto por la función a través del parámetro de salida
                int totalMatriculados = cstmt.getInt(1);
                System.out.println("Número de alumnos matriculados: " + totalMatriculados);
            }

        } catch (Exception e) {
            // Captura de excepciones generales (incluyendo SQLException por errores en la llamada a la función)
            System.err.println("Error al invocar la función almacenada: " + e.getMessage());
        } finally {
            // Cierre del recurso de entrada por consola
            sc.close();
        }
    }
}