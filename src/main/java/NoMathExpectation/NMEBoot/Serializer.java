package NoMathExpectation.NMEBoot;

import java.io.*;

public class Serializer<T> {
    public byte[] serialize(T object) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (ObjectOutputStream stream = new ObjectOutputStream(buffer)) {
            stream.writeObject(object);
            return buffer.toByteArray();
        }
    }

    public T deserialize(byte[] bytes) throws Exception {
        try (ObjectInputStream stream = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            return (T) stream.readObject();
        }
    }
}
