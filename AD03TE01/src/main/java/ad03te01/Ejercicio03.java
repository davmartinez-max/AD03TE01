package ad03te01;

import java.sql.*;
import java.util.Scanner;
import java.util.regex.Pattern;

/**
 * Ejercicio 03 - Tema: Transacciones y Gestión Avanzada en JDBC
 * Programa completo de matriculación que implementa control transaccional (ACID),
 * validación de formatos (DNI mediante Regex), control de stock/plazas y prevención
 * de matrículas duplicadas.
 */
public class Ejercicio03 {
    
    // Parámetros de conexión a la base de datos MySQL
    private static final String URL = "jdbc:mysql://localhost:3306/dbformacion?serverTimezone=UTC";
    private static final String USER = "user";
    private static final String PASS = "user";

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        // Entrada y normalización del DNI del alumno (convertido a mayúsculas)
        System.out.print("Introduce el DNI del alumno: ");
        String dni = sc.nextLine().trim().toUpperCase();
        
        // Validación de sintaxis del DNI mediante Expresión Regular (8 dígitos y 1 letra)
        if (!Pattern.matches("^[0-9]{8}[A-Z]$", dni)) {
            System.out.println("Error: El formato del DNI no es válido (debe tener 8 números y una letra).");
            sc.close();
            return;
        }

        // Bloque try-with-resources para la conexión a la base de datos
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS)) {
            // Desactivación del autocommit para iniciar una transacción controlada manualmente
            conn.setAutoCommit(false); 

            boolean alumnoExiste = false;
            String nombreAlumno = "";

            // Comprobación previa de la existencia del alumno en el sistema
            String sqlCheckAlumno = "SELECT nombre FROM alumnos WHERE dni = ?";
            try (PreparedStatement ps = conn.prepareStatement(sqlCheckAlumno)) {
                ps.setString(1, dni);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        alumnoExiste = true;
                        nombreAlumno = rs.getString("nombre");
                        System.out.println("Alumno: " + nombreAlumno);
                    }
                }
            }

            // Si el alumno no existe, se solicita su alta en la misma transacción
            if (!alumnoExiste) {
                System.out.print("El alumno no está registrado. Introduce su nombre completo: ");
                nombreAlumno = sc.nextLine().trim();
                
                // Validación de integridad de los datos introducidos
                if (nombreAlumno.isEmpty()) {
                    System.out.println("El nombre no puede estar vacío. Cancelando operación.");
                    conn.rollback(); // Revierte cualquier cambio previo si falla la validación
                    sc.close();
                    return;
                }

                // Inserción del nuevo alumno en la base de datos
                String sqlInsertAlumno = "INSERT INTO alumnos (dni, nombre) VALUES (?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(sqlInsertAlumno)) {
                    ps.setString(1, dni);
                    ps.setString(2, nombreAlumno);
                    ps.executeUpdate();
                }
            }

            // Listado de cursos disponibles con cálculo dinámico de plazas libres
            System.out.println("\nCursos disponibles:");
            String sqlCursos = "SELECT c.id_curso, c.titulo, a.capacidad, " +
                               "(a.capacidad - COUNT(m.dni)) AS plazas_libres " +
                               "FROM cursos c " +
                               "JOIN aulas a ON c.id_aula = a.id_aula " +
                               "LEFT JOIN matriculas m ON c.id_curso = m.id_curso " +
                               "GROUP BY c.id_curso, c.titulo, a.capacidad";

            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sqlCursos)) {
                while (rs.next()) {
                    System.out.printf("%d. %-25s plazas libres: %d%n",
                            rs.getInt("id_curso"),
                            rs.getString("titulo"),
                            rs.getInt("plazas_libres"));
                }
            }

            // Solicitud del ID del curso en el que se desea matricular
            System.out.print("Introduce el ID del curso: ");
            int idCurso = Integer.parseInt(sc.nextLine().trim());

            // Validación de la existencia del curso y recuento actualizado de plazas libres
            String sqlValidarCurso = "SELECT c.titulo, a.capacidad, " +
                                     "(a.capacidad - (SELECT COUNT(*) FROM matriculas WHERE id_curso = ?)) AS libres " +
                                     "FROM cursos c JOIN aulas a ON c.id_aula = a.id_aula WHERE c.id_curso = ?";
            
            String tituloCurso = "";
            int plazasLibres = 0;

            try (PreparedStatement ps = conn.prepareStatement(sqlValidarCurso)) {
                ps.setInt(1, idCurso);
                ps.setInt(2, idCurso);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        System.out.println("El curso seleccionado no existe.");
                        conn.rollback(); // Abortar transacción por curso inválido
                        sc.close();
                        return;
                    }
                    tituloCurso = rs.getString("titulo");
                    plazasLibres = rs.getInt("libres");
                }
            }

            // Validación de regla de negocio: control de aforo / plazas agotadas
            if (plazasLibres <= 0) {
                System.out.println("Error: El curso está completo. No quedan plazas libres.");
                conn.rollback();
                sc.close();
                return;
            }

            // Validación de duplicidad: comprobar si el alumno ya está matriculado en este curso
            String sqlCheckMatricula = "SELECT * FROM matriculas WHERE dni = ? AND id_curso = ?";
            try (PreparedStatement ps = conn.prepareStatement(sqlCheckMatricula)) {
                ps.setString(1, dni);
                ps.setInt(2, idCurso);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        System.out.println("Error: El alumno ya está matriculado en este curso.");
                        conn.rollback();
                        sc.close();
                        return;
                    }
                }
            }

            // Inserción de la nueva matrícula utilizando la función CURDATE() de SQL para la fecha actual
            String sqlInsertMatricula = "INSERT INTO matriculas (dni, id_curso, fecha_matricula) VALUES (?, ?, CURDATE())";
            try (PreparedStatement ps = conn.prepareStatement(sqlInsertMatricula)) {
                ps.setString(1, dni);
                ps.setInt(2, idCurso);
                ps.executeUpdate();
            }

            // Confirmación explícita de la transacción (todas las operaciones se aplican de forma atómica)
            conn.commit();
            System.out.println("Matrícula registrada para " + nombreAlumno + " en " + tituloCurso + ".");

        } catch (Exception e) {
            // Captura genérica de excepciones: ante cualquier fallo imprevisto, se revierte toda la transacción
            System.err.println("Error durante el proceso de matriculación. Se ha realizado rollback. Detalle: " + e.getMessage());
        } finally {
            // Cierre del recurso de entrada por consola
            sc.close();
        }
    }
}