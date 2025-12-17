package uk.ac.manchester.tornado.api;

public class TornadoExecutionPlan {
    public TornadoExecutionPlan(TaskGraph... graphs) {
        // Stub - throw exception so GPUManager detects "failure" and uses fallback
        // Or if we want to simulate success but no-op, we leave empty.
        // But the goal implies verification of fallback infrastructure OR actual
        // functionality.
        // Since we don't have the real library, no-op is safer for "running" the app
        // without crashes.
        // But GPUManager catches exceptions.
        // Let's throw UnsupportedOperationException to signal "No GPU Runtime".
        throw new UnsupportedOperationException("TornadoVM implementation not present (Stub)");
    }

    public void execute() {
    }
}
