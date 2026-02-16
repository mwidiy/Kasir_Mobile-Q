from PIL import Image, ImageDraw, ImageFont
import os

# Configuration
LOGOS = {
    "ewallet_dana.png": {"color": "#118EEA", "text": "DANA"},
    "bank_jago.png": {"color": "#5E27C9", "text": "Jago"},
    "bank_seabank.png": {"color": "#EE4D2D", "text": "Sea"},
    "bank_superbank.png": {"color": "#000000", "text": "Super"},
    "bank_bni.png": {"color": "#F15A23", "text": "BNI"},
    "bank_cimb.png": {"color": "#DA291C", "text": "CIMB"},
    "bank_permata.png": {"color": "#00A258", "text": "Permata"},
    "bank_danamon.png": {"color": "#F37021", "text": "Danamon"}
}

OUTPUT_DIR = r"c:\WIDI\KASIR\app\src\main\res\drawable"

def create_logo(filename, config):
    try:
        # Create a 500x500 image with valid background color
        img = Image.new('RGB', (500, 500), color=config["color"])
        d = ImageDraw.Draw(img)
        
        # Draw text in center (simplified since we might not have custom fonts)
        # Using default font, scaled up effectively by drawing big? 
        # Actually default font is tiny. Let's try to load a system font or just draw a big rectangle/circle.
        # Better: Just a clean colored box is better than broken image. 
        # Even better: Draw text if possible.
        
        # Try to load arial
        try:
            font = ImageFont.truetype("arial.ttf", 150)
        except:
            font = ImageFont.load_default()

        # Calculate text position (rough centering)
        text = config["text"]
        left, top, right, bottom = d.textbbox((0, 0), text, font=font)
        text_width = right - left
        text_height = bottom - top
        position = ((500 - text_width) / 2, (500 - text_height) / 2)
        
        d.text(position, text, fill="white", font=font)
        
        # Save
        path = os.path.join(OUTPUT_DIR, filename)
        img.save(path)
        print(f"Generated {path}")
        
    except Exception as e:
        print(f"Failed to generate {filename}: {e}")

if __name__ == "__main__":
    for filename, config in LOGOS.items():
        create_logo(filename, config)
