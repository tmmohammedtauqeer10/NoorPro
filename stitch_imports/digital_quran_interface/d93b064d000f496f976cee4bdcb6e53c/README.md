# Stitch Import: Digital Quran Interface

Project: Digital Quran Interface  
Project ID: 6079309460118466300  
Screen: Improved Quran Section - Dual Language  
Screen ID: d93b064d000f496f976cee4bdcb6e53c

## Files

- `improved_quran_section_dual_language.html` - raw Stitch HTML copied from the attachment.
- `improved_quran_section_dual_language.local.html` - same HTML with remote asset links changed to local files.
- `screen.png` - screenshot/reference image provided with the request.
- `images/quran_hero.jpg` - downloaded Quran hero image from the Stitch hosted URL.
- `fonts-manrope-vietnam.css` - downloaded Google Fonts CSS referenced by the screen.
- `fonts-material-symbols.css` - downloaded Material Symbols CSS referenced by the screen.
- `tailwind-container-queries.js` - downloaded Tailwind CDN script referenced by the screen.

## Notes

- The hosted Quran hero image was served as JPEG data, so it is saved as `quran_hero.jpg`.
- The raw HTML contains some mojibake Arabic text from the pasted source. Use the screenshot and app Quran data as the source of truth when implementing this in Jetpack Compose.
