from huggingface_hub import snapshot_download
snapshot_download(repo_id="taobao-mnn/Qwen3.5-0.8B-MNN", local_dir=".")
print("Download complete")
