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

font_title = get_font(26)
font_label = get_font(20)
font_small = get_font(15)

# =============================================================
# 1. TEMPLATE 4 FOTO (Doppia Striscia Verticale 1200 x 1800 px)
# =============================================================
w_v, h_v = 1200, 1800
strip_w = 600

# Guida 4 Foto
img_4_guide = Image.new("RGBA", (w_v, h_v), (250, 250, 252, 255))
draw_4_guide = ImageDraw.Draw(img_4_guide)

p4_w = 528
p4_h = 370
p4_top = 26
p4_spacing = 12
p4_margin_x = 36

for offset_x in [0, strip_w]:
    for i in range(4):
        p_top = p4_top + i * (p4_h + p4_spacing)
        p_left = offset_x + p4_margin_x
        p_right = p_left + p4_w
        p_bottom = p_top + p4_h
        draw_4_guide.rectangle([p_left, p_top, p_right, p_bottom], fill=(235, 240, 252, 255), outline=(40, 53, 147, 255), width=2)
        draw_4_guide.text((p_left + 16, p_top + 16), f"FOTO {i+1} ({p4_w} x {p4_h} px)", fill=(40, 53, 147, 255), font=font_title)
        draw_4_guide.text((p_left + 16, p_top + 54), "Area occupata dallo scatto", fill=(100, 110, 140, 255), font=font_small)

    footer_top = p4_top + 4 * (p4_h + p4_spacing) + 12
    footer_left = offset_x + p4_margin_x
    footer_right = footer_left + p4_w
    footer_bottom = h_v - 20
    draw_4_guide.rectangle([footer_left, footer_top, footer_right, footer_bottom], fill=(255, 248, 225, 255), outline=(212, 175, 55, 255), width=2)
    draw_4_guide.text((footer_left + 16, footer_top + 20), "AREA TESTO / GRAFICA (4 FOTO)", fill=(180, 120, 0, 255), font=font_title)
    draw_4_guide.text((footer_left + 16, footer_top + 60), f"Spazio disponibile: {p4_w} x {footer_bottom - footer_top} px", fill=(150, 130, 80, 255), font=font_small)

for y in range(20, h_v - 20, 28):
    draw_4_guide.line([(strip_w, y), (strip_w, y + 14)], fill=(220, 0, 80, 255), width=2)
draw_4_guide.text((strip_w - 170, 24), "LINEA DI TAGLIO CENTRALE (2x6\")", fill=(220, 0, 80, 255), font=font_small)
img_4_guide.save(f"{OUTPUT_DIR}/template_4_foto_guida.png", dpi=(300, 300))

# Trasparente 4 Foto
img_4_trans = Image.new("RGBA", (w_v, h_v), (255, 255, 255, 255))
draw_4_trans = ImageDraw.Draw(img_4_trans)
for offset_x in [0, strip_w]:
    for i in range(4):
        p_top = p4_top + i * (p4_h + p4_spacing)
        draw_4_trans.rectangle([offset_x + p4_margin_x, p_top, offset_x + p4_margin_x + p4_w, p_top + p4_h], fill=(0, 0, 0, 0))
img_4_trans.save(f"{OUTPUT_DIR}/template_4_foto_trasparente.png", dpi=(300, 300))


# =============================================================
# 2. TEMPLATE 3 FOTO (Doppia Striscia Verticale 1200 x 1800 px)
# =============================================================
# Guida 3 Foto
img_3_guide = Image.new("RGBA", (w_v, h_v), (250, 250, 252, 255))
draw_3_guide = ImageDraw.Draw(img_3_guide)

p3_w = 528
p3_h = 440
p3_top = 36
p3_spacing = 16
p3_margin_x = 36

for offset_x in [0, strip_w]:
    for i in range(3):
        p_top = p3_top + i * (p3_h + p3_spacing)
        p_left = offset_x + p3_margin_x
        p_right = p_left + p3_w
        p_bottom = p_top + p3_h
        draw_3_guide.rectangle([p_left, p_top, p_right, p_bottom], fill=(235, 240, 252, 255), outline=(40, 53, 147, 255), width=2)
        draw_3_guide.text((p_left + 16, p_top + 16), f"FOTO {i+1} ({p3_w} x {p3_h} px)", fill=(40, 53, 147, 255), font=font_title)
        draw_3_guide.text((p_left + 16, p_top + 54), "Area occupata dallo scatto (3 scatti)", fill=(100, 110, 140, 255), font=font_small)

    footer_top = p3_top + 3 * (p3_h + p3_spacing) + 12
    footer_left = offset_x + p3_margin_x
    footer_right = footer_left + p3_w
    footer_bottom = h_v - 20
    draw_3_guide.rectangle([footer_left, footer_top, footer_right, footer_bottom], fill=(255, 248, 225, 255), outline=(212, 175, 55, 255), width=2)
    draw_3_guide.text((footer_left + 16, footer_top + 20), "AREA TESTO / GRAFICA (3 FOTO)", fill=(180, 120, 0, 255), font=font_title)
    draw_3_guide.text((footer_left + 16, footer_top + 60), f"Spazio disponibile: {p3_w} x {footer_bottom - footer_top} px", fill=(150, 130, 80, 255), font=font_small)

for y in range(20, h_v - 20, 28):
    draw_3_guide.line([(strip_w, y), (strip_w, y + 14)], fill=(220, 0, 80, 255), width=2)
draw_3_guide.text((strip_w - 170, 24), "LINEA DI TAGLIO CENTRALE (2x6\")", fill=(220, 0, 80, 255), font=font_small)
img_3_guide.save(f"{OUTPUT_DIR}/template_3_foto_guida.png", dpi=(300, 300))

# Trasparente 3 Foto
img_3_trans = Image.new("RGBA", (w_v, h_v), (255, 255, 255, 255))
draw_3_trans = ImageDraw.Draw(img_3_trans)
for offset_x in [0, strip_w]:
    for i in range(3):
        p_top = p3_top + i * (p3_h + p3_spacing)
        draw_3_trans.rectangle([offset_x + p3_margin_x, p_top, offset_x + p3_margin_x + p3_w, p_top + p3_h], fill=(0, 0, 0, 0))
img_3_trans.save(f"{OUTPUT_DIR}/template_3_foto_trasparente.png", dpi=(300, 300))


# =============================================================
# 3. TEMPLATE 2 FOTO (Doppia Striscia Orizzontale 1800 x 1200 px)
# =============================================================
w_h, h_h = 1800, 1200
strip_h = 600

# Guida 2 Foto
img_2_guide = Image.new("RGBA", (w_h, h_h), (250, 250, 252, 255))
draw_2_guide = ImageDraw.Draw(img_2_guide)

p2_w = 640
p2_h = 480
p2_top_margin = 60
p2_left_margin = 50
p2_spacing = 30

for offset_y in [0, strip_h]:
    for i in range(2):
        p_left = p2_left_margin + i * (p2_w + p2_spacing)
        p_top = offset_y + p2_top_margin
        p_right = p_left + p2_w
        p_bottom = p_top + p2_h
        draw_2_guide.rectangle([p_left, p_top, p_right, p_bottom], fill=(235, 240, 252, 255), outline=(40, 53, 147, 255), width=2)
        draw_2_guide.text((p_left + 16, p_top + 16), f"FOTO {i+1} ({p2_w} x {p2_h} px)", fill=(40, 53, 147, 255), font=font_title)
        draw_2_guide.text((p_left + 16, p_top + 54), "Area occupata dallo scatto (rapporto 4:3)", fill=(100, 110, 140, 255), font=font_small)

    # Area branding a destra
    b_left = p2_left_margin + 2 * p2_w + p2_spacing + 10
    b_top = offset_y + p2_top_margin
    b_right = w_h - 40
    b_bottom = offset_y + p2_top_margin + p2_h
    draw_2_guide.rectangle([b_left, b_top, b_right, b_bottom], fill=(255, 248, 225, 255), outline=(212, 175, 55, 255), width=2)
    draw_2_guide.text((b_left + 16, b_top + 20), "AREA TESTO / LOGO", fill=(180, 120, 0, 255), font=font_title)
    draw_2_guide.text((b_left + 16, b_top + 60), f"Spazio: {b_right - b_left} x {b_bottom - b_top} px", fill=(150, 130, 80, 255), font=font_small)

for x in range(20, w_h - 20, 28):
    draw_2_guide.line([(x, strip_h), (x + 14, strip_h)], fill=(220, 0, 80, 255), width=2)
draw_2_guide.text((24, strip_h - 26), "LINEA DI TAGLIO CENTRALE ORIZZONTALE (6x2\")", fill=(220, 0, 80, 255), font=font_small)
img_2_guide.save(f"{OUTPUT_DIR}/template_2_foto_guida.png", dpi=(300, 300))

# Trasparente 2 Foto
img_2_trans = Image.new("RGBA", (w_h, h_h), (255, 255, 255, 255))
draw_2_trans = ImageDraw.Draw(img_2_trans)
for offset_y in [0, strip_h]:
    for i in range(2):
        p_left = p2_left_margin + i * (p2_w + p2_spacing)
        p_top = offset_y + p2_top_margin
        draw_2_trans.rectangle([p_left, p_top, p_left + p2_w, p_top + p2_h], fill=(0, 0, 0, 0))
img_2_trans.save(f"{OUTPUT_DIR}/template_2_foto_trasparente.png", dpi=(300, 300))


# =============================================================
# 4. TEMPLATE 1 FOTO (Foto Singola Orizzontale 1800 x 1200 px)
# =============================================================
# Guida 1 Foto
img_1_guide = Image.new("RGBA", (w_h, h_h), (250, 250, 252, 255))
draw_1_guide = ImageDraw.Draw(img_1_guide)

single_margin_x = 60
single_margin_top = 45
single_photo_w = 1680
single_photo_h = 980

draw_1_guide.rectangle([single_margin_x, single_margin_top, single_margin_x + single_photo_w, single_margin_top + single_photo_h], fill=(235, 240, 252, 255), outline=(40, 53, 147, 255), width=2)

font_big = get_font(34)
draw_1_guide.text((single_margin_x + 36, single_margin_top + 36), f"FOTO SINGOLA ORIZZONTALE 10x15 ({single_photo_w} x {single_photo_h} px)", fill=(40, 53, 147, 255), font=font_big)
draw_1_guide.text((single_margin_x + 36, single_margin_top + 84), "Rapporto nativo Canon SELPHY CP1500 (100 x 148 mm a 300 DPI) - 0% bordi bianchi", fill=(100, 110, 140, 255), font=font_label)

single_footer_top = single_margin_top + single_photo_h + 14
single_footer_bottom = h_h - 18
draw_1_guide.rectangle([single_margin_x, single_footer_top, single_margin_x + single_photo_w, single_footer_bottom], fill=(255, 248, 225, 255), outline=(212, 175, 55, 255), width=2)
draw_1_guide.text((single_margin_x + 28, single_footer_top + 20), "AREA TESTO / PERSONALIZZAZIONE GRAFICA INFERIORE (10x15)", fill=(180, 120, 0, 255), font=font_title)
draw_1_guide.text((single_margin_x + 28, single_footer_top + 58), f"Spazio disponibile: {single_photo_w} x {single_footer_bottom - single_footer_top} px a filo bordo.", fill=(150, 130, 80, 255), font=font_small)

img_1_guide.save(f"{OUTPUT_DIR}/template_1_foto_guida.png", dpi=(300, 300))

# Trasparente 1 Foto
img_1_trans = Image.new("RGBA", (w_h, h_h), (255, 255, 255, 255))
draw_1_trans = ImageDraw.Draw(img_1_trans)
draw_1_trans.rectangle([single_margin_x, single_margin_top, single_margin_x + single_photo_w, single_margin_top + single_photo_h], fill=(0, 0, 0, 0))
img_1_trans.save(f"{OUTPUT_DIR}/template_1_foto_trasparente.png", dpi=(300, 300))

print("Tutti i template (4, 3, 2, 1 FOTO per Canon SELPHY CP1500 a 300 DPI) generati con successo!")
