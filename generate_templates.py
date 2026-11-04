import os
from PIL import Image, ImageDraw, ImageFont

OUTPUT_DIR = "/Users/mik/.gemini/antigravity/scratch/photobooth-app/templates"
os.makedirs(OUTPUT_DIR, exist_ok=True)

# Helper for font
def get_font(size):
    try:
        return ImageFont.truetype("/System/Library/Fonts/Supplemental/Arial.ttf", size)
    except:
        try:
            return ImageFont.truetype("/System/Library/Fonts/Helvetica.ttc", size)
        except:
            return ImageFont.load_default()

# -------------------------------------------------------------
# 1. TEMPLATE 4 FOTO - GUIDA (1200 x 1800 px @ 300 DPI)
# -------------------------------------------------------------
w_strip, h_strip = 1200, 1800
img_4_guide = Image.new("RGBA", (w_strip, h_strip), (250, 250, 252, 255))
draw = ImageDraw.Draw(img_4_guide)

font_title = get_font(28)
font_label = get_font(22)
font_small = get_font(16)

# Strips: 0 to 600, 600 to 1200
photo_w = 524
photo_h = 368
top_margin = 26
spacing = 12
margin_x = 38

for col_idx, offset_x in enumerate([0, 600]):
    col_name = "STRISCIA SINISTRA (2x6\")" if col_idx == 0 else "STRISCIA DESTRA (2x6\")"
    
    # 4 photos
    for i in range(4):
        p_top = top_margin + i * (photo_h + spacing)
        p_left = offset_x + margin_x
        p_right = p_left + photo_w
        p_bottom = p_top + photo_h
        
        # Photo box
        draw.rectangle([p_left, p_top, p_right, p_bottom], fill=(235, 240, 250, 255), outline=(63, 81, 181, 255), width=3)
        draw.text((p_left + 15, p_top + 15), f"FOTO {i+1} ({photo_w} x {photo_h} px)", fill=(40, 53, 147, 255), font=font_title)
        draw.text((p_left + 15, p_top + 55), "Spazio occupato dallo scatto della fotocamera", fill=(100, 110, 140, 255), font=font_small)

    # Footer
    footer_top = top_margin + 4 * (photo_h + spacing) + 12
    footer_left = offset_x + margin_x
    footer_right = footer_left + photo_w
    footer_bottom = h_strip - 20
    draw.rectangle([footer_left, footer_top, footer_right, footer_bottom], fill=(255, 248, 225, 255), outline=(212, 175, 55, 255), width=2)
    draw.text((footer_left + 20, footer_top + 20), "AREA TESTO / LOGO EVENTO", fill=(180, 120, 0, 255), font=font_title)
    draw.text((footer_left + 20, footer_top + 60), "Altezza disponibile: ~220 px", fill=(150, 130, 80, 255), font=font_small)
    draw.text((footer_left + 20, footer_top + 85), "Puoi inserire qui la tua grafica o lasciare il testo dell'app", fill=(150, 130, 80, 255), font=font_small)

# Center cut line
for y in range(20, h_strip - 20, 28):
    draw.line([(600, y), (600, y + 14)], fill=(220, 0, 80, 255), width=2)
draw.text((585, 25), "✂ GUIDA AL TAGLIO CENTRALE (Linea tratteggiata 5x15 cm)", fill=(220, 0, 80, 255), font=font_small)

img_4_guide.save(f"{OUTPUT_DIR}/template_4_foto_guida.png", dpi=(300, 300))

# -------------------------------------------------------------
# 2. TEMPLATE 4 FOTO - TRASPARENTE (1200 x 1800 px)
# (Le finestre delle 4 foto sono trasparenti per sovrapporre la grafica)
# -------------------------------------------------------------
img_4_trans = Image.new("RGBA", (w_strip, h_strip), (255, 255, 255, 255))
draw_trans = ImageDraw.Draw(img_4_trans)

for col_idx, offset_x in enumerate([0, 600]):
    for i in range(4):
        p_top = top_margin + i * (photo_h + spacing)
        p_left = offset_x + margin_x
        p_right = p_left + photo_w
        p_bottom = p_top + photo_h
        # Draw transparent holes
        draw_trans.rectangle([p_left, p_top, p_right, p_bottom], fill=(0, 0, 0, 0))

img_4_trans.save(f"{OUTPUT_DIR}/template_4_foto_trasparente.png", dpi=(300, 300))


# -------------------------------------------------------------
# 3. TEMPLATE 1 FOTO - GUIDA (1800 x 1200 px @ 300 DPI)
# -------------------------------------------------------------
w_single, h_single = 1800, 1200
img_1_guide = Image.new("RGBA", (w_single, h_single), (250, 250, 252, 255))
draw1 = ImageDraw.Draw(img_1_guide)

single_margin_x = 70
single_margin_top = 50
single_photo_w = 1660
single_photo_h = 920

# Frame mat
draw1.rectangle([single_margin_x - 12, single_margin_top - 12, single_margin_x + single_photo_w + 12, single_margin_top + single_photo_h + 12], fill=(245, 245, 248, 255), outline=(180, 180, 200, 255), width=2)
# Photo area
draw1.rectangle([single_margin_x, single_margin_top, single_margin_x + single_photo_w, single_margin_top + single_photo_h], fill=(235, 240, 250, 255), outline=(63, 81, 181, 255), width=3)
draw1.text((single_margin_x + 30, single_margin_top + 30), f"FOTO ORIZZONTALE SINGOLA ({single_photo_w} x {single_photo_h} px)", fill=(40, 53, 147, 255), font=font_title)
draw1.text((single_margin_x + 30, single_margin_top + 75), "Area coperta dallo scatto orizzontale in alta risoluzione", fill=(100, 110, 140, 255), font=font_label)

# Footer area
footer1_top = single_margin_top + single_photo_h + 20
draw1.rectangle([single_margin_x, footer1_top, single_margin_x + single_photo_w, h_single - 25], fill=(255, 248, 225, 255), outline=(212, 175, 55, 255), width=2)
draw1.text((single_margin_x + 30, footer1_top + 20), "AREA TESTO / LOGO EVENTO (1800 x ~180 px)", fill=(180, 120, 0, 255), font=font_title)
draw1.text((single_margin_x + 30, footer1_top + 60), "Puoi posizionare il nome degli sposi, data, logo aziendale o grafica personalizzata", fill=(150, 130, 80, 255), font=font_label)

img_1_guide.save(f"{OUTPUT_DIR}/template_1_foto_guida.png", dpi=(300, 300))

# -------------------------------------------------------------
# 4. TEMPLATE 1 FOTO - TRASPARENTE (1800 x 1200 px)
# -------------------------------------------------------------
img_1_trans = Image.new("RGBA", (w_single, h_single), (255, 255, 255, 255))
draw1_trans = ImageDraw.Draw(img_1_trans)
draw1_trans.rectangle([single_margin_x, single_margin_top, single_margin_x + single_photo_w, single_margin_top + single_photo_h], fill=(0, 0, 0, 0))

img_1_trans.save(f"{OUTPUT_DIR}/template_1_foto_trasparente.png", dpi=(300, 300))

print("All 4 PNG templates generated successfully!")
