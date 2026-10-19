package Modelo;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import Excepciones.AutenticacionException;
import Excepciones.ClaveInvalidaException;

public abstract class Usuario {
    public static final int LARGO_MINIMO_CLAVE = 6;

    // Parámetros del hash de contraseñas (ver hashear/verificar más abajo)
    private static final String ALGORITMO_HASH = "PBKDF2WithHmacSHA256";
    private static final int ITERACIONES_HASH = 210_000;
    private static final int BYTES_SAL = 16;
    private static final int BITS_HASH = 256;
    private static final SecureRandom ALEATORIO = new SecureRandom();

    protected String id;
    protected String contrasenaHash; // nunca la contraseña en texto plano
    protected boolean bloqueado;
    protected boolean requiereCambioClave;

    /** Usuario nuevo: clave inicial idéntica al ID (guardada hasheada) y cambio de clave obligatorio. */
    public Usuario(String id) {
        this.id = id;
        this.contrasenaHash = hashear(id == null ? "" : id);
        this.bloqueado = false;
        this.requiereCambioClave = true;
    }

    /** Reconstruye un usuario ya guardado en la base. No recalcula el hash: usa el que vino de la BD. */
    protected Usuario(String id, String contrasenaHash, boolean bloqueado, boolean requiereCambioClave) {
        this.id = id;
        this.contrasenaHash = contrasenaHash;
        this.bloqueado = bloqueado;
        this.requiereCambioClave = requiereCambioClave;
    }

    public void iniciarSesion(String contrasenaIngresada) throws AutenticacionException {
        // Primero se valida la clave y recién después se informa si está bloqueado:
        // así alguien sin la clave no puede descubrir qué IDs existen o están bloqueados.
        if (!verificar(contrasenaIngresada, this.contrasenaHash)) {
            throw new AutenticacionException("ID o contraseña incorrectos.");
        }
        if (this.bloqueado) {
            throw new AutenticacionException("El usuario se encuentra bloqueado. Solicite habilitación al Administrador.");
        }
    }

    public void cambiarContrasena(String nuevaContrasena) throws ClaveInvalidaException {
        if (nuevaContrasena == null || nuevaContrasena.trim().isEmpty()) {
            throw new ClaveInvalidaException("La contraseña no puede estar vacía.");
        }
        if (nuevaContrasena.equals(this.id)) {
            throw new ClaveInvalidaException("La nueva contraseña debe ser diferente a su ID/DNI.");
        }
        if (nuevaContrasena.length() < LARGO_MINIMO_CLAVE) {
            throw new ClaveInvalidaException("La contraseña debe tener al menos " + LARGO_MINIMO_CLAVE + " caracteres.");
        }
        this.contrasenaHash = hashear(nuevaContrasena);
        this.requiereCambioClave = false;
    }

    public void blanquearClave() {
        this.contrasenaHash = hashear(this.id);
        this.requiereCambioClave = true;
    }

    // ---------------------------------------------------------------------
    //  Manejo seguro de contraseñas
    //  En la base NUNCA se guarda la contraseña, solo un "hash": una huella de un solo
    //  sentido que sirve para comprobar si una clave es correcta, pero no permite
    //  averiguar cuál era. Se usa PBKDF2 (viene incluido en Java) con una "sal" aleatoria
    //  distinta por usuario y muchas iteraciones (hace lento el ataque por fuerza bruta).
    //  Formato guardado: iteraciones:sal:hash (sal y hash en Base64).
    // ---------------------------------------------------------------------

    private static String hashear(String contrasena) {
        byte[] sal = new byte[BYTES_SAL];
        ALEATORIO.nextBytes(sal);
        byte[] hash = derivar(contrasena.toCharArray(), sal, ITERACIONES_HASH);
        return ITERACIONES_HASH + ":" + Base64.getEncoder().encodeToString(sal)
                + ":" + Base64.getEncoder().encodeToString(hash);
    }

    private static boolean verificar(String contrasena, String almacenado) {
        if (contrasena == null || almacenado == null) {
            return false;
        }
        String[] partes = almacenado.split(":");
        if (partes.length != 3) {
            return false;
        }
        try {
            int iteraciones = Integer.parseInt(partes[0]);
            byte[] sal = Base64.getDecoder().decode(partes[1]);
            byte[] esperado = Base64.getDecoder().decode(partes[2]);
            byte[] calculado = derivar(contrasena.toCharArray(), sal, iteraciones);
            // isEqual compara en tiempo constante (no revela "en qué byte falló")
            return MessageDigest.isEqual(esperado, calculado);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static byte[] derivar(char[] clave, byte[] sal, int iteraciones) {
        PBEKeySpec spec = new PBEKeySpec(clave, sal, iteraciones, BITS_HASH);
        try {
            return SecretKeyFactory.getInstance(ALGORITMO_HASH).generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo calcular el hash de la contraseña.", e);
        } finally {
            spec.clearPassword();
        }
    }

    public boolean isBloqueado() { return bloqueado; }
    public void setBloqueado(boolean bloqueado) { this.bloqueado = bloqueado; }
    public String getId() { return id; }
    public boolean isRequiereCambioClave() { return requiereCambioClave; }
    public void setRequiereCambioClave(boolean requiere) { this.requiereCambioClave = requiere; }

    /** Lo usa la capa de persistencia para guardar el hash en la base. */
    public String getContrasenaHash() { return contrasenaHash; }
}