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
# 1. TEMPLATE 4 FOTO - GUIDA A5 (1748 x 2480 px @ 300 DPI)
# -------------------------------------------------------------
w_strip, h_strip = 1748, 2480
img_4_guide = Image.new("RGBA", (w_strip, h_strip), (250, 250, 252, 255))
draw = ImageDraw.Draw(img_4_guide)

font_title = get_font(36)
font_label = get_font(28)
font_small = get_font(20)

photo_w = 766
photo_h = 506
top_margin = 38
spacing = 16
margin_x = 54
strip_w = 874

for col_idx, offset_x in enumerate([0, strip_w]):
    col_name = "STRISCIA 1 (SX)" if col_idx == 0 else "STRISCIA 2 (DX)"
    
    # 4 photos
    for i in range(4):
        p_top = top_margin + i * (photo_h + spacing)
        p_left = offset_x + margin_x
        p_right = p_left + photo_w
        p_bottom = p_top + photo_h
        
        # Photo box
        draw.rectangle([p_left, p_top, p_right, p_bottom], fill=(235, 240, 252, 255), outline=(40, 53, 147, 255), width=3)
        draw.text((p_left + 24, p_top + 24), f"FOTO {i+1} ({photo_w} x {photo_h} px)", fill=(40, 53, 147, 255), font=font_title)
        draw.text((p_left + 24, p_top + 74), "Spazio occupato dallo scatto della fotocamera", fill=(100, 110, 140, 255), font=font_small)

    # Footer
    footer_top = top_margin + 4 * (photo_h + spacing) + 16
    footer_left = offset_x + margin_x
    footer_right = footer_left + photo_w
    footer_bottom = h_strip - 30
    draw.rectangle([footer_left, footer_top, footer_right, footer_bottom], fill=(255, 248, 225, 255), outline=(212, 175, 55, 255), width=3)
    draw.text((footer_left + 24, footer_top + 30), "AREA TESTO / LOGO EVENTO (A5)", fill=(180, 120, 0, 255), font=font_title)
    draw.text((footer_left + 24, footer_top + 80), f"Larghezza: {photo_w} px  |  Altezza disponibile: ~{footer_bottom - footer_top} px", fill=(150, 130, 80, 255), font=font_small)
    draw.text((footer_left + 24, footer_top + 115), "Personalizza questa zona con nomi, data, cornici grafiche o loghi", fill=(150, 130, 80, 255), font=font_small)

# Center cut line
for y in range(30, h_strip - 30, 36):
    draw.line([(strip_w, y), (strip_w, y + 18)], fill=(220, 0, 80, 255), width=3)
draw.text((strip_w - 200, 35), "LINEA DI TAGLIO CENTRALE (A5)", fill=(220, 0, 80, 255), font=font_small)

img_4_guide.save(f"{OUTPUT_DIR}/template_4_foto_guida.png", dpi=(300, 300))

# -------------------------------------------------------------
# 2. TEMPLATE 4 FOTO - TRASPARENTE A5 (1748 x 2480 px)
# (Finestre trasparenti per le 4 foto per sovrapporre la grafica)
# -------------------------------------------------------------
img_4_trans = Image.new("RGBA", (w_strip, h_strip), (255, 255, 255, 255))
draw_trans = ImageDraw.Draw(img_4_trans)

for offset_x in [0, strip_w]:
    for i in range(4):
        p_top = top_margin + i * (photo_h + spacing)
        p_left = offset_x + margin_x
        p_right = p_left + photo_w
        p_bottom = p_top + photo_h
        # Finestre trasparenti
        draw_trans.rectangle([p_left, p_top, p_right, p_bottom], fill=(0, 0, 0, 0))

img_4_trans.save(f"{OUTPUT_DIR}/template_4_foto_trasparente.png", dpi=(300, 300))

# -------------------------------------------------------------
# 3. TEMPLATE 1 FOTO - GUIDA A5 (2480 x 1748 px @ 300 DPI)
# -------------------------------------------------------------
w_single, h_single = 2480, 1748
img_1_guide = Image.new("RGBA", (w_single, h_single), (250, 250, 252, 255))
draw1 = ImageDraw.Draw(img_1_guide)

single_margin_x = 80
single_margin_top = 60
single_photo_w = 2320
single_photo_h = 1360

# Area foto (SENZA bordo bianco/passepartout)
draw1.rectangle([single_margin_x, single_margin_top, single_margin_x + single_photo_w, single_margin_top + single_photo_h], fill=(235, 240, 252, 255), outline=(40, 53, 147, 255), width=3)

font_big = get_font(44)
draw1.text((single_margin_x + 50, single_margin_top + 50), f"FOTO ORIZZONTALE SINGOLA A5 ({single_photo_w} x {single_photo_h} px)", fill=(40, 53, 147, 255), font=font_big)
draw1.text((single_margin_x + 50, single_margin_top + 110), "Risoluzione consigliata 300 DPI  |  Formato carta: A5 Orizzontale (210 x 148 mm)", fill=(100, 110, 140, 255), font=font_label)

# Footer area
single_footer_top = single_margin_top + single_photo_h + 20
single_footer_bottom = h_single - 24
draw1.rectangle([single_margin_x, single_footer_top, single_margin_x + single_photo_w, single_footer_bottom], fill=(255, 248, 225, 255), outline=(212, 175, 55, 255), width=3)
draw1.text((single_margin_x + 40, single_footer_top + 28), "AREA TESTO / PERSONALIZZAZIONE GRAFICA INFERIORE (A5)", fill=(180, 120, 0, 255), font=font_title)
draw1.text((single_margin_x + 40, single_footer_top + 80), f"Spazio disponibile: {single_photo_w} x {single_footer_bottom - single_footer_top} px. Ideale per loghi, data, monogramma o dedica.", fill=(150, 130, 80, 255), font=font_small)

img_1_guide.save(f"{OUTPUT_DIR}/template_1_foto_guida.png", dpi=(300, 300))

# -------------------------------------------------------------
# 4. TEMPLATE 1 FOTO - TRASPARENTE A5 (2480 x 1748 px)
# -------------------------------------------------------------
img_1_trans = Image.new("RGBA", (w_single, h_single), (255, 255, 255, 255))
draw1_trans = ImageDraw.Draw(img_1_trans)
draw1_trans.rectangle([single_margin_x, single_margin_top, single_margin_x + single_photo_w, single_margin_top + single_photo_h], fill=(0, 0, 0, 0))
img_1_trans.save(f"{OUTPUT_DIR}/template_1_foto_trasparente.png", dpi=(300, 300))

print("Tutti i template A5 a 300 DPI sono stati generati con successo!")
