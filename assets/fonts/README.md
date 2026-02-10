# Fonts

Here are the TTF fonts used by Gaia Sky. The actual fonts are in `assets/fonts`. The directory `assets/fonts/src` contains the original fonts (not in VCS). The directory `assets/fonts/chars` contains files with the characters used in Gaia Sky. From these, we create a subset for each font:

```bash
pyftsubset src/SarasaUiSC-Regular.ttf \
--text="$(cat chars/common.txt chars/chinese3500.txt)" \
--unicodes="U+0000-007F,U+00A0-00FF" \
--output-file=SarasaUiSC-Regular-Subset.ttf \
--layout-features='kern,liga,clig,calt,locl,subs,sups' \
--layout-features='*' \
--glyph-names \
--symbol-cmap \
--notdef-glyph \
--notdef-outline \
--recommended-glyphs
```

This enables us to reduce the size of each file from 10MB to ~1.5MB.
