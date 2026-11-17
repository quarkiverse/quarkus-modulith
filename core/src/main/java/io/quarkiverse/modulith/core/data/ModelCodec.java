package io.quarkiverse.modulith.core.data;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import io.quarkiverse.modulith.core.ModelData;

/**
 * Writes the module model to the compact binary resource the application reads when the {@code ApplicationModules} bean
 * is first used, so the recorded startup code does not grow with the application. The layout is the length of the
 * encoded model followed by the deflated encoding.
 */
public final class ModelCodec {

    /**
     * Where the build puts the model in the application.
     */
    public static final String LOCATION = "META-INF/quarkus-modulith/model.bin";

    /**
     * The largest encoded model accepted, far above the size of the model of any real application.
     */
    static final int MAX_LENGTH = 256 * 1024 * 1024;

    private ModelCodec() {
    }

    /**
     * @return the model as bytes, each distinct string stored once, compressed
     */
    public static byte[] encode(ModelData data) {
        byte[] encoded = new ModelEncoder().model(data).toByteArray();
        Deflater deflater = new Deflater();
        deflater.setInput(encoded);
        deflater.finish();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.writeBytes(ByteBuffer.allocate(Integer.BYTES).putInt(encoded.length).array());
        byte[] buffer = new byte[8192];
        while (!deflater.finished()) {
            out.write(buffer, 0, deflater.deflate(buffer));
        }
        deflater.end();
        return out.toByteArray();
    }

    /**
     * @return the model {@link #encode(ModelData)} wrote
     * @throws IllegalArgumentException if the bytes are not such a model
     */
    public static ModelData decode(byte[] bytes) {
        if (bytes.length < Integer.BYTES) {
            throw new IllegalArgumentException("The model holds no length");
        }
        int length = ByteBuffer.wrap(bytes).getInt();
        if (length < 0 || length > MAX_LENGTH) {
            throw new IllegalArgumentException("The model declares " + length + " bytes, expected 0 to " + MAX_LENGTH);
        }
        Inflater inflater = new Inflater();
        inflater.setInput(bytes, Integer.BYTES, bytes.length - Integer.BYTES);
        byte[] encoded = new byte[length];
        try {
            int inflated = inflater.inflate(encoded);
            if (inflated != length || !inflater.finished()) {
                throw new IllegalArgumentException("The model holds " + inflated + " of the " + length
                        + " bytes it declares");
            }
        } catch (DataFormatException e) {
            throw new IllegalArgumentException("The model is corrupt", e);
        } finally {
            inflater.end();
        }
        try {
            return new ModelDecoder(encoded).model();
        } catch (IndexOutOfBoundsException e) {
            throw new IllegalArgumentException("The model is corrupt", e);
        }
    }
}
