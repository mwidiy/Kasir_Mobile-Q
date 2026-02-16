from PIL import Image
import os

DRAWABLE_DIR = r"c:\WIDI\KASIR\app\src\main\res\drawable"

def verify_images():
    files = [f for f in os.listdir(DRAWABLE_DIR) if f.endswith('.png')]
    bad_files = []
    
    for f in files:
        path = os.path.join(DRAWABLE_DIR, f)
        try:
            img = Image.open(path)
            img.verify() # Verify file integrity
            # Re-open to check dimensions (verify closes the file)
            img = Image.open(path) 
            width, height = img.size
            if width < 10 or height < 10:
                print(f"WARNING: {f} is too small ({width}x{height})")
                bad_files.append(f)
            # print(f"OK: {f}")
        except Exception as e:
            print(f"ERROR: {f} is corrupt or invalid: {e}")
            bad_files.append(f)

    if bad_files:
        print(f"\nFound {len(bad_files)} bad files:")
        for f in bad_files:
            print(f"- {f}")
    else:
        print("\nAll PNG files verified successfully!")

if __name__ == "__main__":
    verify_images()
