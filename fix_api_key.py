#!/usr/bin/env python3
"""
Get the correct Roboflow API Key.

The "Publishable API Key" is for client-side only and CANNOT download models.
You need the "Private API Key" from your settings.
"""

print("="*70)
print("How to Get Your Roboflow API Key")
print("="*70)

print("""
⚠ IMPORTANT: The "Publishable API Key" shown in your workspace 
   does NOT have permission to download models!

You need the PRIVATE API KEY instead:

Step 1: Go to Roboflow Settings
   https://app.roboflow.com/settings/api

Step 2: Look for "Private API Key"
   - Click "Create New Private Key" if you don't have one
   - Copy the key (it looks like: rf_xxxxxxxxxxxxxxxxxxxxxxxx)

Step 3: Update this script with your private API key
   Open download_roboflow_auto.py and replace the API_KEY variable

The private API key has full permissions to:
   ✓ Download datasets
   ✓ Download models  
   ✓ Export to different formats
   ✓ Access your projects

The publishable API key (which you used) can only:
   ✗ Query deployed models
   ✗ Access via browser/client-side
   ✗ NOT download files

""")

# Ask user for the correct API key
print("="*70)
api_key = input("Enter your PRIVATE API Key from https://app.roboflow.com/settings/api:\n").strip()

if api_key:
    # Update the download script
    script_path = "download_roboflow_auto.py"
    
    with open(script_path, 'r') as f:
        content = f.read()
    
    # Replace the API key
    content = content.replace(
        'API_KEY = "rf_UXR3fCMuTYVvIKxKnKyRySuA0Q72"',
        f'API_KEY = "{api_key}"'
    )
    
    with open(script_path, 'w') as f:
        f.write(content)
    
    print(f"\n✓ Updated {script_path} with your new API key")
    print("\nYou can now run: python download_roboflow_auto.py")
else:
    print("\n⚠ No API key entered. Please run this script again with your private API key.")
