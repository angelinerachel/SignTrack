import android.content.res.AssetFileDescriptor;
import org.tensorflow.lite.Interpreter;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class TFLiteModelHelper {
    private Interpreter tfliteInterpreter;

    public TFLiteModelHelper(AssetFileDescriptor modelFile) throws Exception {
        this.tfliteInterpreter = new Interpreter(loadModelFile(modelFile));
    }

    private MappedByteBuffer loadModelFile(AssetFileDescriptor fileDescriptor) throws Exception {
        FileChannel fileChannel = new FileInputStream(fileDescriptor.getFileDescriptor()).getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    public float[][] predict(float[][][][] inputData) {
        // Input: [1, TEMPORAL_WINDOW_SIZE, NUM_NODES, FEATURES_PER_NODE]
        // Output: [1, numClasses]
        float[][] output = new float[1][LABEL_COUNT];  // Adjust LABEL_COUNT
        tfliteInterpreter.run(inputData, output);
        return output;
    }

    public void close() {
        tfliteInterpreter.close();
    }
}
