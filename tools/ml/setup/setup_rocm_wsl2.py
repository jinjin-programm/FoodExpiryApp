#!/usr/bin/env python3
"""
AMD ROCm Setup Guide for WSL2 - YOLO Training

This guide helps you set up AMD GPU acceleration for YOLO training.

HARDWARE: AMD RX 9070 XT (RDNA 3.5 architecture)
OS: Windows 11 + WSL2 Ubuntu 22.04

================================================================================
STEP 1: CHECK WSL2 INSTALLATION
================================================================================
"""

SETUP_GUIDE = """
================================================================================
AMD ROCm SETUP GUIDE FOR YOLO TRAINING (WSL2)
================================================================================

PREREQUISITES:
- AMD RX 9070 XT GPU (RDNA 3.5 / gfx1100)
- Windows 11 with WSL2 installed
- At least 16GB RAM
- 50GB+ free disk space

================================================================================
STEP 1: INSTALL/UPDATE WSL2
================================================================================

Open PowerShell as Administrator:

    # Check WSL version
    wsl --version
    
    # If not installed, run:
    wsl --install -d Ubuntu-22.04
    
    # Update WSL kernel
    wsl --update
    
    # Set WSL2 as default
    wsl --set-default-version 2

================================================================================
STEP 2: CONFIGURE UBUNTU IN WSL2
================================================================================

Open Ubuntu terminal:

    # Update system
    sudo apt update && sudo apt upgrade -y
    
    # Install build tools
    sudo apt install -y build-essential git wget curl
    
    # Add user to render and video groups
    sudo usermod -aG render,video $USER
    
    # Logout and login again for groups to take effect
    exit

================================================================================
STEP 3: INSTALL AMD ROCm 6.0 (CRITICAL FOR RX 9070 XT)
================================================================================

ROCm 6.0+ supports RDNA 3.5 (gfx1100) architecture:

    # Download and install ROCm
    wget https://repo.radeon.com/amdgpu-install/6.0/ubuntu/jammy/amdgpu-install_6.0.60000-1_all.deb
    
    sudo apt install ./amdgpu-install_6.0.60000-1_all.deb
    
    # Install ROCm with graphics support
    sudo amdgpu-install --usecase=rocm,graphics --no-dkms
    
    # Add to PATH
    echo 'export PATH=$PATH:/opt/rocm/bin' >> ~/.bashrc
    echo 'export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:/opt/rocm/lib' >> ~/.bashrc
    source ~/.bashrc
    
    # Verify installation
    rocminfo
    
    # Check if GPU is detected
    rocm-smi

IF rocminfo SHOWS YOUR GPU, PROCEED TO NEXT STEP!

================================================================================
STEP 4: INSTALL PYTHON AND PYTORCH WITH ROCm
================================================================================

    # Install Python 3.10
    sudo apt install -y python3.10 python3.10-venv python3-pip
    
    # Create virtual environment
    python3.10 -m venv ~/yolo_env
    source ~/yolo_env/bin/activate
    
    # Upgrade pip
    pip install --upgrade pip
    
    # Install PyTorch with ROCm 6.0 support
    pip install torch torchvision --index-url https://download.pytorch.org/whl/rocm6.0
    
    # Verify GPU detection
    python -c "import torch; print(f'CUDA available: {torch.cuda.is_available()}'); print(f'Device: {torch.cuda.get_device_name(0) if torch.cuda.is_available() else \"CPU\"}')"

EXPECTED OUTPUT:
    CUDA available: True
    Device: AMD Radeon RX 9070 XT (or similar)

================================================================================
STEP 5: INSTALL ULTRALYTICS (YOLO)
================================================================================

    # With venv activated
    pip install ultralytics
    
    # Verify installation
    python -c "from ultralytics import YOLO; print('YOLO ready!')"

================================================================================
STEP 6: ACCESS WINDOWS FILES IN WSL2
================================================================================

Your Windows drives are mounted under /mnt/:

    # Access your dataset
    cd /mnt/d/FoodExpiryDatasets/crawled_grocery_yolo
    
    # Or create a symlink
    ln -s /mnt/d/FoodExpiryDatasets ~/datasets

================================================================================
STEP 7: TRAIN YOUR MODEL
================================================================================

Copy the training script to WSL2:

    # In Windows, copy the script to WSL accessible location
    # Then in WSL2:
    cd /mnt/c/Users/jinjin/AndroidStudioProjects/FoodExpiryAPP
    
    # Run training
    python train_yolo11_amd.py --epochs 100 --batch 16

Or run directly:

    from ultralytics import YOLO
    
    model = YOLO("yolo11n.pt")
    model.train(data="/mnt/d/FoodExpiryDatasets/crawled_grocery_yolo/data.yaml", 
                epochs=100, imgsz=640, batch=16, device=0)

================================================================================
TROUBLESHOOTING
================================================================================

Issue: "CUDA not available"
Solution:
    - Check rocminfo shows GPU
    - Reinstall ROCm
    - Verify PATH includes /opt/rocm/bin

Issue: "HIP error: out of memory"
Solution:
    - Reduce batch size: --batch 8
    - Reduce image size: --imgsz 416
    - Close other GPU applications

Issue: "Permission denied" for /dev/kfd
Solution:
    sudo usermod -aG render,video $USER
    # Logout and login again

Issue: Training is slow
Solution:
    - Verify GPU is being used (check nvidia-smi equivalent: rocm-smi)
    - Reduce workers: --workers 4
    - Use smaller model: yolo11s instead of yolo11m

================================================================================
ALTERNATIVE: CPU TRAINING (if ROCm doesn't work)
================================================================================

If ROCm setup fails, use CPU training (slower but reliable):

    # Install CPU-only PyTorch
    pip install torch torchvision --index-url https://download.pytorch.org/whl/cpu
    
    # Train with CPU
    python train_yolo11_amd.py --device cpu

Note: CPU training will be 5-10x slower than GPU

================================================================================
NEXT STEPS AFTER TRAINING
================================================================================

1. Export model to TFLite
2. Copy to Android project
3. Update labels file
4. Test on device

Run: python export_to_android.py

================================================================================
"""

def main():
    print(SETUP_GUIDE)
    
    print("\n" + "="*80)
    print("QUICK START COMMANDS")
    print("="*80)
    print("""
# In WSL2 Ubuntu:

# 1. Install ROCm
wget https://repo.radeon.com/amdgpu-install/6.0/ubuntu/jammy/amdgpu-install_6.0.60000-1_all.deb
sudo apt install ./amdgpu-install_6.0.60000-1_all.deb
sudo amdgpu-install --usecase=rocm,graphics --no-dkms
sudo usermod -aG render,video $USER

# 2. Logout and login, then:
echo 'export PATH=$PATH:/opt/rocm/bin' >> ~/.bashrc
source ~/.bashrc
rocminfo

# 3. Setup Python and PyTorch
python3 -m venv ~/yolo_env
source ~/yolo_env/bin/activate
pip install torch torchvision --index-url https://download.pytorch.org/whl/rocm6.0
pip install ultralytics

# 4. Verify
python -c "import torch; print(torch.cuda.is_available())"

# 5. Train
cd /mnt/c/Users/jinjin/AndroidStudioProjects/FoodExpiryAPP
python train_yolo11_amd.py
""")

if __name__ == "__main__":
    main()
