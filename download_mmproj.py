import urllib.request
import json
import os

repo_id = "unsloth/Qwen3-VL-2B-Instruct-GGUF"
api_url = f"https://huggingface.co/api/models/{repo_id}/tree/main"
download_dir = r"C:\Users\jinjin\Downloads"

print("Fetching file list from HuggingFace...")
req = urllib.request.Request(api_url)
with urllib.request.urlopen(req) as response:
    data = json.loads(response.read().decode())

mmproj_file = None
for item in data:
    if "mmproj" in item["path"] and item["path"].endswith(".gguf"):
        mmproj_file = item["path"]
        break

if not mmproj_file:
    print("Error: Could not find mmproj file in the repository.")
    exit(1)

print(f"Found mmproj file: {mmproj_file}")
download_url = f"https://huggingface.co/{repo_id}/resolve/main/{mmproj_file}?download=true"
dest_path = os.path.join(download_dir, mmproj_file)

if os.path.exists(dest_path):
    print(f"File already exists at {dest_path}. Skipping download.")
else:
    print(f"Downloading from {download_url} to {dest_path}...")
    def reporthook(blocknum, blocksize, totalsize):
        readsofar = blocknum * blocksize
        if totalsize > 0:
            percent = readsofar * 1e2 / totalsize
            print(f"\rDownloading: {percent:.1f}% ({readsofar / 1024 / 1024:.2f} MB / {totalsize / 1024 / 1024:.2f} MB)", end="")
        else: # total size is unknown
            print(f"\rRead {readsofar} bytes", end="")

    urllib.request.urlretrieve(download_url, dest_path, reporthook)
    print("\nDownload complete!")

print(f"FILE_PATH={dest_path}")
