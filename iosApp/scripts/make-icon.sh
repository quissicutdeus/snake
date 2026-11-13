#!/bin/sh
# Makes the iOS app icon from the Android launcher icon, so the two never
# drift apart: the adaptive icon's foreground (a VectorDrawable) becomes an
# SVG (iosApp/icon/vector-to-svg.xsl), which is drawn at 1024x1024 on the
# adaptive icon's background colour, with no transparency (iOS shows
# transparent icon pixels as black).
#
#   iosApp/scripts/make-icon.sh
#
# Needs xsltproc (libxslt), rsvg-convert (librsvg) and magick (ImageMagick 7).
# Writes iosApp/Sniddle/Assets.xcassets/AppIcon.appiconset/AppIcon.png, which
# is committed: CI's macOS runners don't have librsvg, and the icon only
# changes when the Android one does. Run it again after changing
# app/src/main/res/drawable/ic_launcher_foreground.xml or the background colour.
set -eu

cd "$(git rev-parse --show-toplevel)"

for tool in xsltproc rsvg-convert magick; do
    if ! command -v "$tool" >/dev/null 2>&1; then
        echo "make-icon: $tool not found (Arch: paru -S libxslt librsvg imagemagick)" >&2
        exit 1
    fi
done

foreground=app/src/main/res/drawable/ic_launcher_foreground.xml
colors=app/src/main/res/values/colors.xml
out=iosApp/Sniddle/Assets.xcassets/AppIcon.appiconset/AppIcon.png

# <color name="ic_launcher_background">#FF1F2547</color>  ->  #1F2547
bg=$(sed -n 's#.*<color name="ic_launcher_background">\#[0-9A-Fa-f]\{2\}\([0-9A-Fa-f]\{6\}\)</color>.*#\#\1#p' "$colors")
if [ -z "$bg" ]; then
    echo "make-icon: no #AARRGGBB ic_launcher_background colour in $colors" >&2
    exit 1
fi

work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT

xsltproc --stringparam bg "$bg" iosApp/icon/vector-to-svg.xsl "$foreground" >"$work/icon.svg"
rsvg-convert --width 1024 --height 1024 "$work/icon.svg" --output "$work/icon.png"
magick "$work/icon.png" -background "$bg" -alpha remove -alpha off -strip "PNG24:$out"

size=$(magick identify -format '%wx%h %[colorspace] alpha=%A' "$out")
case $size in
    "1024x1024 sRGB alpha=Undefined" | "1024x1024 sRGB alpha=False") ;;
    *)
        echo "make-icon: expected a 1024x1024 sRGB image without alpha, got '$size'" >&2
        exit 1
        ;;
esac
echo "make-icon: wrote $out ($size, background $bg)"
