import os
from PIL import Image, ImageDraw, ImageFont

OUTPUT_DIR = "/Users/mik/.gemini/antigravity/scratch/photobooth-app/templates"
os.makedirs(OUTPUT_DIR, exist_ok=True)

def get_font(size):
    try:
        return ImageFont.truetype("/System/Library/Fonts/Supplemental/Arial.ttf", size)
    except:
        try:
            return ImageFont.truetype("/System/Library/Fonts/Helvetica.ttc", size)
        except:
            return ImageFont.load_default()

# -------------------------------------------------------------
# 1. TEMPLATE 4 FOTO - GUIDA CANON SELPHY (1200 x 1800 px @ 300 DPI)
# -------------------------------------------------------------
w_strip, h_strip = 1200, 1800
img_4_guide = Image.new("RGBA", (w_strip, h_strip), (250, 250, 252, 255))
draw = ImageDraw.Draw(img_4_guide)

font_title = get_font(26)
font_label = get_font(20)
font_small = get_font(15)

photo_w = 528
photo_h = 370
top_margin = 26
spacing = 12
margin_x = 36
strip_w = 600

for col_idx, offset_x in enumerate([0, strip_w]):
    # 4 photos
    for i in range(4):
        p_top = top_margin + i * (photo_h + spacing)
        p_left = offset_x + margin_x
        p_right = p_left + photo_w
        p_bottom = p_top + photo_h
        
        # Photo box
        draw.rectangle([p_left, p_top, p_right, p_bottom], fill=(235, 240, 252, 255), outline=(40, 53, 147, 255), width=2)
        draw.text((p_left + 16, p_top + 16), f"FOTO {i+1} ({photo_w} x {photo_h} px)", fill=(40, 53, 147, 255), font=font_title)
        draw.text((p_left + 16, p_top + 54), "Area occupata dallo scatto", fill=(100, 110, 140, 255), font=font_small)

    # Footer
    footer_top = top_margin + 4 * (photo_h + spacing) + 12
    footer_left = offset_x + margin_x
    footer_right = footer_left + photo_w
    footer_bottom = h_strip - 20
    draw.rectangle([footer_left, footer_top, footer_right, footer_bottom], fill=(255, 248, 225, 255), outline=(212, 175, 55, 255), width=2)
    draw.text((footer_left + 16, footer_top + 20), "AREA TESTO / GRAFICA (10x15)", fill=(180, 120, 0, 255), font=font_title)
    draw.text((footer_left + 16, footer_top + 60), f"Spazio disponibile: {photo_w} x {footer_bottom - footer_top} px", fill=(150, 130, 80, 255), font=font_small)
    draw.text((footer_left + 16, footer_top + 85), "I margini arrivano a filo foglio per stampa senza bordi bianchi", fill=(150, 130, 80, 255), font=font_small)

# Center cut line
for y in range(20, h_strip - 20, 28):
    draw.line([(strip_w, y), (strip_w, y + 14)], fill=(220, 0, 80, 255), width=2)
draw.text((strip_w - 170, 24), "LINEA DI TAGLIO CENTRALE (2x6\")", fill=(220, 0, 80, 255), font=font_small)

img_4_guide.save(f"{OUTPUT_DIR}/template_4_foto_guida.png", dpi=(300, 300))

# -------------------------------------------------------------
# 2. TEMPLATE 4 FOTO - TRASPARENTE CANON SELPHY (1200 x 1800 px)
# -------------------------------------------------------------
img_4_trans = Image.new("RGBA", (w_strip, h_strip), (255, 255, 255, 255))
draw_trans = ImageDraw.Draw(img_4_trans)

for offset_x in [0, strip_w]:
    for i in range(4):
        p_top = top_margin + i * (photo_h + spacing)
        p_left = offset_x + margin_x
        p_right = p_left + photo_w
        p_bottom = p_top + photo_h
        draw_trans.rectangle([p_left, p_top, p_right, p_bottom], fill=(0, 0, 0, 0))

img_4_trans.save(f"{OUTPUT_DIR}/template_4_foto_trasparente.png", dpi=(300, 300))

# -------------------------------------------------------------
# 3. TEMPLATE 1 FOTO - GUIDA CANON SELPHY (1800 x 1200 px @ 300 DPI)
# -------------------------------------------------------------
w_single, h_single = 1800, 1200
img_1_guide = Image.new("RGBA", (w_single, h_single), (250, 250, 252, 255))
draw1 = ImageDraw.Draw(img_1_guide)

single_margin_x = 60
single_margin_top = 45
single_photo_w = 1680
single_photo_h = 980

# Area foto (SENZA bordo bianco/passepartout)
draw1.rectangle([single_margin_x, single_margin_top, single_margin_x + single_photo_w, single_margin_top + single_photo_h], fill=(235, 240, 252, 255), outline=(40, 53, 147, 255), width=2)

font_big = get_font(34)
draw1.text((single_margin_x + 36, single_margin_top + 36), f"FOTO SINGOLA ORIZZONTALE 10x15 ({single_photo_w} x {single_photo_h} px)", fill=(40, 53, 147, 255), font=font_big)
draw1.text((single_margin_x + 36, single_margin_top + 84), "Rapporto esatto Canon SELPHY CP1500 (100 x 148 mm a 300 DPI) - 0% bordi bianchi", fill=(100, 110, 140, 255), font=font_label)

# Footer area
single_footer_top = single_margin_top + single_photo_h + 14
single_footer_bottom = h_single - 18
draw1.rectangle([single_margin_x, single_footer_top, single_margin_x + single_photo_w, single_footer_bottom], fill=(255, 248, 225, 255), outline=(212, 175, 55, 255), width=2)
draw1.text((single_margin_x + 28, single_footer_top + 20), "AREA TESTO / PERSONALIZZAZIONE GRAFICA INFERIORE (10x15)", fill=(180, 120, 0, 255), font=font_title)
draw1.text((single_margin_x + 28, single_footer_top + 58), f"Spazio disponibile: {single_photo_w} x {single_footer_bottom - single_footer_top} px a filo bordo.", fill=(150, 130, 80, 255), font=font_small)

img_1_guide.save(f"{OUTPUT_DIR}/template_1_foto_guida.png", dpi=(300, 300))

# -------------------------------------------------------------
# 4. TEMPLATE 1 FOTO - TRASPARENTE CANON SELPHY (1800 x 1200 px)
# -------------------------------------------------------------
img_1_trans = Image.new("RGBA", (w_single, h_single), (255, 255, 255, 255))
draw1_trans = ImageDraw.Draw(img_1_trans)
draw1_trans.rectangle([single_margin_x, single_margin_top, single_margin_x + single_photo_w, single_margin_top + single_photo_h], fill=(0, 0, 0, 0))
img_1_trans.save(f"{OUTPUT_DIR}/template_1_foto_trasparente.png", dpi=(300, 300))

print("Tutti i template per Canon SELPHY CP1500 (10x15cm a 300 DPI) sono stati generati con successo!")
