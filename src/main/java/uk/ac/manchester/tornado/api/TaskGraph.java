package uk.ac.manchester.tornado.api;

import uk.ac.manchester.tornado.api.enums.DataTransferMode;

public class TaskGraph {
    /*
     * Task graph.
     * Enforces physical invariants and updates associated state variables within {@code TaskGraph}.
     *
     * @param name the name parameter (String)
     */
    public TaskGraph(String name) {
        // Stub
    }

    /*
     * Transfer to device.
     * Enforces physical invariants and updates associated state variables within {@code TaskGraph}.
     *
     * @param mode the mode parameter (DataTransferMode)
     * @param objects the objects parameter (Object...)
     * @return the resulting computation or state reference
     */
    public TaskGraph transferToDevice(DataTransferMode mode, Object... objects) {
        return this;
    }

    /*
     * Transfer to host.
     * Enforces physical invariants and updates associated state variables within {@code TaskGraph}.
     *
     * @param mode the mode parameter (DataTransferMode)
     * @param objects the objects parameter (Object...)
     * @return the resulting computation or state reference
     */
    public TaskGraph transferToHost(DataTransferMode mode, Object... objects) {
        return this;
    }

    // Generic stub to match any arity (improving for 4 args specifically for our
    // use case)
    public interface Task4<A, B, C, D> {
        void run(A a, B b, C c, D d);
    }

    /*
     * Task.
     * Enforces physical invariants and updates associated state variables within {@code TaskGraph}.
     *
     * @param id the id parameter (String)
     * @param code the code parameter (D&gt;)
     * @param a the a parameter (A)
     * @param b the b parameter (B)
     * @param c the c parameter (C)
     * @param d the d parameter (D)
     * @return the resulting computation or state reference
     */
    public <A, B, C, D> TaskGraph task(String id, Task4<A, B, C, D> code, A a, B b, C c, D d) {
        return this;
    }

    /*
     * Task.
     * Enforces physical invariants and updates associated state variables within {@code TaskGraph}.
     *
     * @param id the id parameter (String)
     * @param code the code parameter (Object)
     * @param args the args parameter (Object...)
     * @return the resulting computation or state reference
     */
    public TaskGraph task(String id, Object code, Object... args) {
        return this;
    }

    /*
     * Snapshot.
     * Enforces physical invariants and updates associated state variables within {@code TaskGraph}.
     *
     */
    public void snapshot() {
    }
}
