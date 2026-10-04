/**
 * <h1>Cluster Orchestration, Halo Exchange &amp; Clock Barriers</h1>
 * <p>
 * Implements distributed coordination primitives for large-scale spatial simulations across compute nodes:
 * <ul>
 *   <li>{@link org.ether.society.network.cluster.ClusterClockBarrier}: Distributed barrier synchronization across simulation ticks.</li>
 *   <li>{@link org.ether.society.network.cluster.H3HaloBoundaryExchanger}: Halo cell buffer exchange across spatial partitions.</li>
 *   <li>{@link org.ether.society.network.cluster.ClusterSnapshotManager}: Distributed snapshotting and checkpointing.</li>
 *   <li>{@link org.ether.society.network.cluster.WorkerGPUOffloader}: Offloading computational kernels to dedicated worker nodes.</li>
 * </ul>
 * </p>
 */
package org.ether.society.network.cluster;
