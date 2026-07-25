# Installing OpenCL for Intel GPU (TornadoVM)

**System:** Windows with Intel GPU  
**Purpose:** GPU acceleration for TornadoVM

---

## Option 1: Intel Graphics Drivers (Recommended)

### 1. Download Intel Graphics Driver
- Visit: https://www.intel.com/content/www/us/en/download-center/home.html
- Search for your Intel GPU model (e.g., "Intel UHD Graphics 630")
- Download the latest driver package
- **OR** use Intel Driver & Support Assistant: https://www.intel.com/content/www/us/en/support/detect.html

### 2. Install Driver
```powershell
# Run the installer (DCH drivers include OpenCL)
# Make sure to select "Clean Install" option
```

### 3. Verify OpenCL Installation
```powershell
# Check if OpenCL is installed
clinfo

# If clinfo is not found, install it:
# Download from: https://github.com/Oblomov/clinfo/releases
```

---

## Option 2: Intel OpenCL Runtime (Standalone)

If drivers don't include OpenCL:

### 1. Download Intel OpenCL Runtime
- Visit: https://www.intel.com/content/www/us/en/developer/articles/tool/opencl-drivers.html
- Download "Intel® Processor Graphics – Windows®" package
- **Direct link**: https://registrationcenter-download.intel.com/akdlm/IRC_NAS/

### 2. Install
```powershell
# Run installer
.\intel-opencl-runtime-windows-*.exe

# Follow prompts
```

### 3. Add to PATH
```powershell
# Check installation directory
# Typical: C:\Program Files (x)\Intel\OpenCL\SDK

# Add to PATH (PowerShell as Admin):
$env:PATH += ";C:\Program Files (x86)\Intel\OpenCL\SDK\bin"
[System.Environment]::SetEnvironmentVariable("PATH", $env:PATH, [System.EnvironmentVariableTarget]::Machine)
```

---

## Verify Installation

### 1. Check OpenCL Availability
```powershell
# Download and run clinfo
clinfo

# Expected output should show:
# Platform Name: Intel(R) OpenCL HD Graphics
# Device Name: Intel(R) UHD Graphics XXX
```

### 2. Check from Java
```java
// Test OpenCL from Java (create TestOpenCL.java):
import org.jocl.*;

public class TestOpenCL {
    public static void main(String[] args) {
        int[] numPlatforms = new int[1];
        CL.clGetPlatformIDs(0, null, numPlatforms);
        System.out.println("OpenCL Platforms: " + numPlatforms[0]);
        
        if (numPlatforms[0] > 0) {
            cl_platform_id[] platforms = new cl_platform_id[numPlatforms[0]];
            CL.clGetPlatformIDs(platforms.length, platforms, null);
            
            for (cl_platform_id platform : platforms) {
                System.out.println("Platform: " + getString(platform, CL.CL_PLATFORM_NAME));
            }
        }
    }
}
```

---

## TornadoVM Setup (After OpenCL)

### 1. Prerequisites
```powershell
# Java 21 (already installed)
# Maven (already installed)
# Git
# CMake (for building TornadoVM)
```

### 2. Download CMake
```powershell
# https://cmake.org/download/
winget install Kitware.CMake
```

### 3. Build TornadoVM
```powershell
# Clone TornadoVM
git clone https://github.com/beehive-lab/TornadoVM
cd TornadoVM

# Configure for Intel GPU (OpenCL)
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
.\bin\tornadovm-installer --backend=opencl --jdk=%JAVA_HOME%

# Build
.\bin\compile.cmd
```

### 4. Add to Project
```xml
<!-- In your pom.xml -->
<dependency>
    <groupId>tornado</groupId>
    <artifactId>tornado-api</artifactId>
    <version>1.0.1</version>
    <scope>system</scope>
    <systemPath>${TORNADO_SDK}/share/java/tornado/tornado-api-1.0.1.jar</systemPath>
</dependency>
```

---

## Troubleshooting

### Issue: "No OpenCL platforms found"
**Solution:**
1. Update Intel graphics drivers
2. Restart computer after install
3. Check Device Manager → Display adapters → Intel GPU is enabled

### Issue: "Access denied to GPU"
**Solution:**
```powershell
# Run as Administrator
# Check if integrated GPU is active (not disabled in BIOS)
```

### Issue: "clinfo shows CPU only"
**Solution:**
- Intel drivers might only install CPU runtime
- Download GPU-specific package from Intel website
- Check GPU is not disabled in BIOS/UEFI

---

## Expected Output (Success)

```
> clinfo

Number of platforms: 1
  Platform Name: Intel(R) OpenCL HD Graphics
  Platform Vendor: Intel(R) Corporation
  Platform Version: OpenCL 3.0

  Device Name: Intel(R) UHD Graphics 630
  Device Type: GPU
  Device Vendor: Intel(R) Corporation
  Max Compute Units: 24
  Max Work Group Size: 256
```

---

## Next Steps

Once OpenCL is working:
1. Test TornadoVM basic examples
2. Create GPU kernels for Ether simulation
3. Profile performance (CPU vs GPU)

**Estimated speedup with Intel integrated GPU:**
- 5-10x for large parallel workloads
- Best for: matrix operations, parallel array processing
- For full 100x speedup, dedicated NVIDIA/AMD GPU recommended

---

**Need help?** 
- Intel OpenCL Forum: https://community.intel.com/t5/OpenCL/bd-p/opencl
- TornadoVM Docs: https://tornadovm.readthedocs.io/
