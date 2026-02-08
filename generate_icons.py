import os
from PIL import Image, ImageOps, ImageDraw

def generate_icons(source_path, res_dir):
    if not os.path.exists(source_path):
        print(f"Error: Source file not found at {source_path}")
        return

    # Android Icon Sizes
    densities = {
        'mipmap-mdpi': 48,
        'mipmap-hdpi': 72,
        'mipmap-xhdpi': 96,
        'mipmap-xxhdpi': 144,
        'mipmap-xxxhdpi': 192
    }

    try:
        img = Image.open(source_path).convert("RGBA")
        print(f"Loaded image: {img.size}")
        
        # CENTER CROP STRATEGY
        # Assuming the "Q" is in the center. We want a crop that is square.
        width, height = img.size
        min_dim = min(width, height)
        
        # Calculate crop box (Center)
        left = (width - min_dim) / 2
        top = (height - min_dim) / 2
        right = (width + min_dim) / 2
        bottom = (height + min_dim) / 2
        
        # Crop to square
        img_square = img.crop((left, top, right, bottom))
        
        # Optional: Zoom in slightly (10%) to emphasize the "Q" if ample whitespace
        # zoom_factor = 0.9 
        # crop_dim = min_dim * zoom_factor
        # z_left = (min_dim - crop_dim) / 2
        # z_top = (min_dim - crop_dim) / 2
        # z_right = (min_dim + crop_dim) / 2
        # z_bottom = (min_dim + crop_dim) / 2
        # img_square = img_square.crop((z_left, z_top, z_right, z_bottom))

        for folder, size in densities.items():
            target_dir = os.path.join(res_dir, folder)
            if not os.path.exists(target_dir):
                os.makedirs(target_dir)

            # 1. Standard Icon (Square/Full)
            # Resize
            icon = img_square.resize((size, size), Image.Resampling.LANCZOS)
            icon_path = os.path.join(target_dir, "ic_launcher.png")
            icon.save(icon_path)
            print(f"Saved {folder}/ic_launcher.png")

            # 2. Round Icon (Circle Mask)
            mask = Image.new('L', (size, size), 0)
            draw = ImageDraw.Draw(mask) 
            draw.ellipse((0, 0, size, size), fill=255)
            
            icon_round = ImageOps.fit(img_square, (size, size), centering=(0.5, 0.5))
            icon_round.putalpha(mask)
            
            round_path = os.path.join(target_dir, "ic_launcher_round.png")
            icon_round.save(round_path)
            print(f"Saved {folder}/ic_launcher_round.png")

        print("Icon generation complete!")

    except Exception as e:
        print(f"An error occurred: {e}")

if __name__ == "__main__":
    # PATHS
    source = r"c:\WIDI\KASIR\Tes-Prototype-2\assets\aplikasi_icon.png"
    res_folder = r"c:\WIDI\KASIR\app\src\main\res"
    
    generate_icons(source, res_folder)
