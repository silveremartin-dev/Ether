# TornadoVM GPU Integration - README

## Prerequisites

### 1. Install TornadoVM

TornadoVM requires manual installation as it's not available in Maven Central.

**Quick Install (Linux/macOS):**
```bash
# Clone TornadoVM
git clone https://github.com/beehive-lab/TornadoVM.git
cd TornadoVM

# Install with default backend (OpenCL)
./bin/tornadovm-installer --jdk jdk-21 --backend opencl

# Or install with CUDA (NVIDIA only)
./bin/tornadovm-installer --jdk jdk-21 --backend ptx,opencl
```

**Windows:**
```powershell
# Install OpenCL SDK first
# Then clone and build TornadoVM
git clone https://github.com/beehive-lab/TornadoVM.git
cd TornadoVM
.\scripts\tornadoVMInstaller.cmd
```

### 2. Configure Environment

Add to your `.bashrc` or `.zshrc`:
```bash
export TORNADO_SDK=/path/to/TornadoVM/bin/sdk
source $TORNADO_SDK/etc/sources.env
```

### 3. Verify Installation

```bash
tornado --version
tornado --devices
```

You should see available GPU devices listed.

## GPU Backends Supported

- **OpenCL**: AMD, Intel, NVIDIA GPUs
- **PTX/CUDA**: NVIDIA GPUs only
- **SPIR-V**: Intel GPUs (experimental)

## Running with GPU

```bash
# Run with default GPU
mvn javafx:run

# Run with specific backend
tornado --backend opencl mvn javafx:run
tornado --backend ptx mvn javafx:run

# Run with profiling
tornado --debug mvn javafx:run
```

## Performance Tips

1. **Warm-up**: First few ticks will be slow (JIT compilation)
2. **Batch Size**: Adjust agent count for optimal GPU utilization
3. **Memory**: Minimize host-device transfers

## Fallback to CPU

If TornadoVM is not installed, the simulation automatically falls back to CPU-only Virtual Threads.

## Troubleshooting

**Issue**: `tornado: command not found`
- Solution: Source the TornadoVM environment

**Issue**: No GPU devices found
- Solution: Install GPU drivers and OpenCL/CUDA runtime

**Issue**: Out of memory errors
- Solution: Reduce world size or agent count in `config/default-config.json`

## Alternative: CPU-Only Build

To build without TornadoVM dependency:
```bash
# Comment out TornadoVM dependencies in pom.xml
# The code will automatically use CPU fallback
```
