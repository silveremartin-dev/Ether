package uk.ac.manchester.tornado.api;

import uk.ac.manchester.tornado.api.enums.DataTransferMode;

public class TaskGraph {
    public TaskGraph(String name) {
        // Stub
    }

    public TaskGraph transferToDevice(DataTransferMode mode, Object... objects) {
        return this;
    }

    public TaskGraph transferToHost(DataTransferMode mode, Object... objects) {
        return this;
    }

    // Generic stub to match any arity (improving for 4 args specifically for our
    // use case)
    public interface Task4<A, B, C, D> {
        void run(A a, B b, C c, D d);
    }

    public <A, B, C, D> TaskGraph task(String id, Task4<A, B, C, D> code, A a, B b, C c, D d) {
        return this;
    }

    public TaskGraph task(String id, Object code, Object... args) {
        return this;
    }

    public void snapshot() {
    }
}
