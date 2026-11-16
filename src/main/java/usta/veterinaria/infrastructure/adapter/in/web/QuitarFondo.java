package usta.veterinaria.infrastructure.adapter.in.web;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.util.Map;

@ApplicationScoped
public class QuitarFondo {

    private static final int TAM = 320;               // entrada del modelo
    private static final int LADO_MAX = 1600;         // evita decodificar imágenes enormes
    private static final float[] MEDIA = {0.485f, 0.456f, 0.406f};
    private static final float[] STD = {0.229f, 0.224f, 0.225f};

    @ConfigProperty(name = "yazkas.modelo.fondo", defaultValue = "models/u2net.onnx")
    String rutaModelo;

    private OrtEnvironment env;
    private OrtSession sesion;
    private String nombreEntrada;

    @PostConstruct
    void iniciar() throws OrtException {
        env = OrtEnvironment.getEnvironment();
        sesion = env.createSession(rutaModelo, new OrtSession.SessionOptions());
        nombreEntrada = sesion.getInputNames().iterator().next();
    }

    @PreDestroy
    void cerrar() throws OrtException {
        if (sesion != null) sesion.close();
    }

    public byte[] aPngSinFondo(InputStream in) throws IOException {
        BufferedImage src = ImageIO.read(in);
        if (src == null) throw new IOException("Imagen no válida");
        src = limitar(src);
        int w = src.getWidth(), h = src.getHeight();

        // 1) Reducir a 320x320 y preparar el tensor
        BufferedImage peq = escalar(src, TAM, TAM, BufferedImage.TYPE_INT_RGB);
        float[] datos = new float[3 * TAM * TAM];
        float max = 1e-6f;
        for (int y = 0; y < TAM; y++) {
            for (int x = 0; x < TAM; x++) {
                int p = peq.getRGB(x, y);
                max = Math.max(max, Math.max((p >> 16) & 255, Math.max((p >> 8) & 255, p & 255)));
            }
        }
        for (int y = 0; y < TAM; y++) {
            for (int x = 0; x < TAM; x++) {
                int p = peq.getRGB(x, y);
                int i = y * TAM + x;
                datos[i] = (((p >> 16) & 255) / max - MEDIA[0]) / STD[0];
                datos[TAM * TAM + i] = (((p >> 8) & 255) / max - MEDIA[1]) / STD[1];
                datos[2 * TAM * TAM + i] = ((p & 255) / max - MEDIA[2]) / STD[2];
            }
        }

        // 2) Ejecutar el modelo
        float[][] mapa;
        try (OnnxTensor tensor = OnnxTensor.createTensor(env, FloatBuffer.wrap(datos), new long[]{1, 3, TAM, TAM});
             OrtSession.Result r = sesion.run(Map.of(nombreEntrada, tensor))) {
            mapa = ((float[][][][]) r.get(0).getValue())[0][0];
        } catch (OrtException e) {
            throw new IOException("Falló el modelo de recorte", e);
        }

        // 3) Normalizar la máscara a 0..255
        float mn = Float.MAX_VALUE, mx = -Float.MAX_VALUE;
        for (float[] fila : mapa) for (float v : fila) { mn = Math.min(mn, v); mx = Math.max(mx, v); }
        float rango = Math.max(mx - mn, 1e-6f);
        BufferedImage mascPeq = new BufferedImage(TAM, TAM, BufferedImage.TYPE_BYTE_GRAY);
        for (int y = 0; y < TAM; y++) {
            for (int x = 0; x < TAM; x++) {
                int a = Math.round((mapa[y][x] - mn) / rango * 255f);
                mascPeq.getRaster().setSample(x, y, 0, a);
            }
        }

        // 4) Escalar la máscara al tamaño original y usarla como transparencia
        BufferedImage masc = escalar(mascPeq, w, h, BufferedImage.TYPE_BYTE_GRAY);
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = src.getRGB(x, y) & 0x00FFFFFF;
                int alfa = masc.getRaster().getSample(x, y, 0);
                out.setRGB(x, y, (alfa << 24) | rgb);
            }
        }

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ImageIO.write(out, "png", bos);
        return bos.toByteArray();
    }

    private static BufferedImage limitar(BufferedImage src) {
        int mayor = Math.max(src.getWidth(), src.getHeight());
        if (mayor <= LADO_MAX) return src;
        double k = (double) LADO_MAX / mayor;
        return escalar(src, (int) (src.getWidth() * k), (int) (src.getHeight() * k), BufferedImage.TYPE_INT_RGB);
    }

    private static BufferedImage escalar(BufferedImage src, int w, int h, int tipo) {
        BufferedImage dst = new BufferedImage(w, h, tipo);
        Graphics2D g = dst.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(src, 0, 0, w, h, null);
        g.dispose();
        return dst;
    }
}