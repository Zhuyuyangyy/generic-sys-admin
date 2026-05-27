#!/usr/bin/env python3
"""
Fix corrupted Java files by:
1. Restore original from git (clean GBK)
2. Convert GBK -> UTF-8 (no BOM)
3. Re-apply manually edited files
"""
import os, subprocess, glob

PROJECT = r'D:\ZYY Project\generic-sys-admin'
BACKEND_SRC = os.path.join(PROJECT, 'backend', 'src', 'main', 'java')
BACKEND_TEST = os.path.join(PROJECT, 'backend', 'src', 'test', 'java')

def run(cmd, cwd=PROJECT):
    result = subprocess.run(cmd, shell=True, cwd=cwd, 
                          capture_output=True, text=True,
                          encoding='utf-8', errors='replace')
    return result.stdout + result.stderr

def is_valid_utf8(path):
    """Check if file is valid UTF-8."""
    with open(path, 'rb') as f:
        raw = f.read()
    try:
        raw.decode('utf-8')
        return True
    except:
        return False

def gbk_to_utf8(path):
    """Convert GBK file to clean UTF-8 without BOM."""
    with open(path, 'rb') as f:
        raw = f.read()
    
    # Remove BOM if present
    if raw.startswith(b'\xef\xbb\xbf'):
        raw = raw[3:]
    
    # Decode as GBK
    try:
        text = raw.decode('gbk')
    except:
        # Not GBK - might already be something else
        try:
            text = raw.decode('utf-8')
            print(f'  Already UTF8: {os.path.basename(path)}')
            return
        except:
            print(f"  Can't decode: {os.path.basename(path)}")
            return
    
    # Write as UTF-8 without BOM
    with open(path, 'w', encoding='utf-8', newline='') as f:
        f.write(text)
    print(f'  Converted GBK->UTF8: {os.path.basename(path)}')

def restore_and_convert(files):
    """Restore files from git then convert."""
    for f in files:
        rel = os.path.relpath(f, PROJECT).replace('\\', '/')
        print(f'\n--- {os.path.basename(f)} ---')
        
        # Restore from git
        run(f'git checkout HEAD -- "{rel}"')
        
        # Verify it was restored
        if not os.path.exists(f):
            print(f'  ERROR: file not found after restore')
            continue
        
        # Check encoding
        with open(f, 'rb') as fh:
            raw = fh.read()
        
        # Check for UTF-8 BOM
        has_bom = raw.startswith(b'\xef\xbb\xbf')
        has_raw = raw[:20]
        
        # Try decode
        try:
            test = raw.decode('utf-8')
            print(f'  Restored as: UTF-8 (BOM={has_bom})')
            # If has BOM, remove it
            if has_bom:
                text = raw.decode('utf-8-sig')
                with open(f, 'w', encoding='utf-8', newline='') as out:
                    out.write(text)
                print(f'  BOM removed')
        except:
            print(f'  Restored as: GBK')
            gbk_to_utf8(f)

# Step 1: Restore ALL java files from git
print('=== Restoring all Java files from git ===')
java_files = glob.glob(os.path.join(BACKEND_SRC, '**', '*.java'), recursive=True)
java_files += glob.glob(os.path.join(BACKEND_TEST, '**', '*.java'), recursive=True)
print(f'Found {len(java_files)} Java files')

for f in java_files:
    rel = os.path.relpath(f, PROJECT).replace('\\', '/')
    run(f'git checkout HEAD -- "{rel}"')

# Step 2: Check what encoding they are now
print('\n=== Checking restored files ===')
for f in sorted(java_files):
    with open(f, 'rb') as fh:
        raw = fh.read()
    
    has_bom = raw.startswith(b'\xef\xbb\xbf')
    
    try:
        text = raw.decode('utf-8')
        enc = 'UTF8' + ('+BOM' if has_bom else '')
    except:
        try:
            text = raw.decode('gbk')
            enc = 'GBK'
        except:
            enc = 'OTHER'
    
    # Check for mojibake in content
    mojibake = any(c in text for c in {'姣', '曡', '偁', '鍙', '戝', '銆'})
    status = 'MOJIBAKE!' if mojibake else 'OK'
    
    print(f'  [{enc:10}] {status} {os.path.basename(f)}')
    
    if enc == 'GBK':
        gbk_to_utf8(f)

# Step 3: Handle manually edited files
print('\n=== Re-applying manual edits ===')

# LocalFileStorageStrategy - was manually fixed
LOCAL_FILE = os.path.join(BACKEND_SRC, 'com', 'zyy', 'config', 'LocalFileStorageStrategy.java')
if os.path.exists(LOCAL_FILE):
    # Re-write with correct content (forward slashes, no unicode escapes)
    content = '''package com.zyy.config;

import com.zyy.util.MinioUtil;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.UUID;

/**
 * 本地文件存储策略
 * 文件存储在服务器本地文件系统
 */
@Component
public class LocalFileStorageStrategy implements FileStorageStrategy {

    @Override
    public String upload(MultipartFile file, String dir) throws Exception {
        // 使用 / 作为路径分隔符（兼容 Windows 和 Linux）
        String originalFilename = file.getOriginalFilename();
        String ext = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            ext = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String filename = UUID.randomUUID().toString().replace("-", "") + ext;
        
        // 存储路径: uploads/dir/filename
        String relativePath = "uploads/" + dir + "/" + filename;
        
        // 获取上传目录的绝对路径
        Path uploadPath = java.nio.file.Paths.get(
            MinioUtil.getUploadDir(), 
            dir, 
            filename
        );
        
        // 确保目录存在
        java.nio.file.Files.createDirectories(uploadPath.getParent());
        
        // 保存文件
        file.transfer/uploadPath(uploadPath);
        
        return relativePath;
    }

    @Override
    public void delete(String path) throws Exception {
        if (path == null || path.isEmpty()) {
            return;
        }
        Path fullPath = java.nio.file.Paths.get(MinioUtil.getUploadDir(), path);
        java.nio.file.Files.deleteIfExists(fullPath);
    }

    @Override
    public String getAccessUrl(String path) {
        // 本地文件返回相对路径，由前端拼接服务器地址
        return "/files/" + path;
    }
}
'''
    with open(LOCAL_FILE, 'w', encoding='utf-8', newline='') as f:
        f.write(content)
    print(f'  Re-wrote LocalFileStorageStrategy.java')

print('\n=== Done ===')
