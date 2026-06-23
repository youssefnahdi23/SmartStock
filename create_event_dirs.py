#!/usr/bin/env python3
"""Create event directories for SmartStock"""
import os
import sys

base_path = r"c:\Users\Youssef\Documents\Projects\SmartStock\docs"

directories = [
    "events",
    "events\\identity",
    "events\\product",
    "events\\inventory",
    "events\\warehouse",
    "events\\supplier",
    "events\\customer",
    "events\\purchase-order",
    "events\\sales-order"
]

print("Creating directories...")
print("=" * 70)

for dir_path in directories:
    full_path = os.path.join(base_path, dir_path)
    try:
        os.makedirs(full_path, exist_ok=True)
        print(f"✓ Created: {full_path}")
    except Exception as e:
        print(f"✗ Failed: {full_path}")
        print(f"  Error: {e}")
        sys.exit(1)

print("\n" + "=" * 70)
print("Verifying all directories exist...")
print("=" * 70 + "\n")

all_exist = True
for dir_path in directories:
    full_path = os.path.join(base_path, dir_path)
    exists = os.path.isdir(full_path)
    status = "✓" if exists else "✗"
    print(f"{status} {full_path}")
    if not exists:
        all_exist = False

print("\n" + "=" * 70)
if all_exist:
    print("SUCCESS: All directories created successfully!")
else:
    print("ERROR: Some directories are missing!")
    sys.exit(1)

# List the directory structure
print("\nDirectory structure:")
print("=" * 70)
for root, dirs, files in os.walk(os.path.join(base_path, "events")):
    level = root.replace(os.path.join(base_path, "events"), "").count(os.sep)
    indent = " " * 2 * level
    print(f"{indent}{os.path.basename(root)}/")
    subindent = " " * 2 * (level + 1)
    for d in dirs:
        print(f"{subindent}{d}/")
