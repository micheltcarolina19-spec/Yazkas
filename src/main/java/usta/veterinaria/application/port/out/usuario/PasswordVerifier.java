
package usta.veterinaria.application.port.out.usuario;

public interface PasswordVerifier {

    boolean verificar(String contrasenaIngresada,
                      String contrasenaCifrada);

}
