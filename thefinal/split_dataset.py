import os
import random
import shutil

# Get current working directory as base
base_dir = os.getcwd()
images_dir = os.path.join(base_dir, "images")
labels_dir = os.path.join(base_dir, "labels")

# Create train/val subfolders (if not exist)
os.makedirs(os.path.join(images_dir, "train"), exist_ok=True)
os.makedirs(os.path.join(images_dir, "val"), exist_ok=True)
os.makedirs(os.path.join(labels_dir, "train"), exist_ok=True)
os.makedirs(os.path.join(labels_dir, "val"), exist_ok=True)

# Collect all images (JPG/PNG)
image_files = [f for f in os.listdir(images_dir) if f.lower().endswith(('.jpg', '.jpeg', '.png'))]

# Randomize and split 80/20
random.shuffle(image_files)
split_index = int(0.8 * len(image_files))
train_files = image_files[:split_index]
val_files = image_files[split_index:]

def move_pairs(files, dest_img, dest_lbl):
    for img in files:
        img_path = os.path.join(images_dir, img)
        label_path = os.path.join(labels_dir, os.path.splitext(img)[0] + ".txt")
        
        if not os.path.exists(label_path):
            print(f"⚠️ Missing label for {img}, skipping.")
            continue

        shutil.move(img_path, os.path.join(dest_img, img))
        shutil.move(label_path, os.path.join(dest_lbl, os.path.basename(label_path)))

# Move image/label pairs
move_pairs(train_files, os.path.join(images_dir, "train"), os.path.join(labels_dir, "train"))
move_pairs(val_files, os.path.join(images_dir, "val"), os.path.join(labels_dir, "val"))

print("✅ Dataset successfully split into train and val sets!")
