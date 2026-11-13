<?xml version="1.0" encoding="UTF-8"?>
<!--
  Turns the Android launcher icon (an adaptive icon's foreground, as a
  VectorDrawable) into an SVG for the iOS app icon. Run by
  iosApp/scripts/make-icon.sh; see docs/ios.md.

  Handles the subset of VectorDrawable the Sniddle icon uses: <group> with
  pivot/scale/translate, and <path> with fill and stroke attributes. Any other
  element or attribute (fillAlpha, fillType, trimPath*, rotation, a gradient...)
  stops the conversion, so nothing can silently drop out of the icon: teach the
  converter the new feature, then add it to the attribute lists below.

  Parameters:
    bg   background colour as #RRGGBB (the adaptive icon's background)

  The SVG's viewBox is the middle 72 of the drawable's 108 units: the part an
  Android launcher shows inside its mask. iOS shows almost the whole square,
  so cropping to the same region makes the snail the same size on both.
-->
<xsl:stylesheet version="1.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:android="http://schemas.android.com/apk/res/android"
    exclude-result-prefixes="android">

    <xsl:output method="xml" indent="yes" encoding="UTF-8"/>
    <xsl:param name="bg"/>

    <xsl:template match="/vector">
        <xsl:if test="not(starts-with($bg, '#')) or string-length($bg) != 7">
            <xsl:message terminate="yes">vector-to-svg: bg must be #RRGGBB, got '<xsl:value-of select="$bg"/>'</xsl:message>
        </xsl:if>
        <xsl:call-template name="known-attributes">
            <xsl:with-param name="known" select="'|name|width|height|viewportWidth|viewportHeight|'"/>
        </xsl:call-template>
        <xsl:if test="@android:viewportWidth != 108 or @android:viewportHeight != 108">
            <xsl:message terminate="yes">vector-to-svg: expected a 108x108 adaptive-icon viewport</xsl:message>
        </xsl:if>
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="18 18 72 72" width="1024" height="1024">
            <rect x="0" y="0" width="108" height="108" fill="{$bg}"/>
            <xsl:apply-templates select="*"/>
        </svg>
    </xsl:template>

    <!--
      Android applies a group's transform as: scale about the pivot, then
      translate. As an SVG transform list (applied right to left) that is
      translate(t) translate(p) scale(s) translate(-p).
    -->
    <xsl:template match="group">
        <xsl:variable name="px" select="sum(@android:pivotX)"/>
        <xsl:variable name="py" select="sum(@android:pivotY)"/>
        <xsl:variable name="tx" select="sum(@android:translateX)"/>
        <xsl:variable name="ty" select="sum(@android:translateY)"/>
        <xsl:variable name="sx" select="sum(@android:scaleX) + number(not(@android:scaleX))"/>
        <xsl:variable name="sy" select="sum(@android:scaleY) + number(not(@android:scaleY))"/>
        <xsl:call-template name="known-attributes">
            <xsl:with-param name="known" select="'|name|pivotX|pivotY|scaleX|scaleY|translateX|translateY|'"/>
        </xsl:call-template>
        <g xmlns="http://www.w3.org/2000/svg">
            <xsl:attribute name="transform">
                <xsl:value-of select="concat(
                    'translate(', $tx, ' ', $ty, ') ',
                    'translate(', $px, ' ', $py, ') ',
                    'scale(', $sx, ' ', $sy, ') ',
                    'translate(', -$px, ' ', -$py, ')')"/>
            </xsl:attribute>
            <xsl:apply-templates select="*"/>
        </g>
    </xsl:template>

    <xsl:template match="path">
        <xsl:call-template name="known-attributes">
            <xsl:with-param name="known"
                select="'|name|pathData|fillColor|strokeColor|strokeWidth|strokeLineCap|strokeLineJoin|'"/>
        </xsl:call-template>
        <path xmlns="http://www.w3.org/2000/svg" d="{@android:pathData}">
            <xsl:choose>
                <xsl:when test="@android:fillColor">
                    <xsl:call-template name="paint">
                        <xsl:with-param name="what" select="'fill'"/>
                        <xsl:with-param name="argb" select="@android:fillColor"/>
                    </xsl:call-template>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:attribute name="fill">none</xsl:attribute>
                </xsl:otherwise>
            </xsl:choose>
            <xsl:if test="@android:strokeColor">
                <xsl:call-template name="paint">
                    <xsl:with-param name="what" select="'stroke'"/>
                    <xsl:with-param name="argb" select="@android:strokeColor"/>
                </xsl:call-template>
                <xsl:attribute name="stroke-width">
                    <xsl:value-of select="@android:strokeWidth"/>
                </xsl:attribute>
            </xsl:if>
            <xsl:if test="@android:strokeLineCap">
                <xsl:attribute name="stroke-linecap">
                    <xsl:value-of select="@android:strokeLineCap"/>
                </xsl:attribute>
            </xsl:if>
            <xsl:if test="@android:strokeLineJoin">
                <xsl:attribute name="stroke-linejoin">
                    <xsl:value-of select="@android:strokeLineJoin"/>
                </xsl:attribute>
            </xsl:if>
        </path>
    </xsl:template>

    <!-- #AARRGGBB (or #RRGGBB) -> fill="#RRGGBB" fill-opacity="0..1" -->
    <xsl:template name="paint">
        <xsl:param name="what"/>
        <xsl:param name="argb"/>
        <xsl:variable name="hex" select="translate(substring($argb, 2), 'abcdef', 'ABCDEF')"/>
        <xsl:choose>
            <xsl:when test="string-length($hex) = 8">
                <xsl:attribute name="{$what}">
                    <xsl:value-of select="concat('#', substring($hex, 3))"/>
                </xsl:attribute>
                <xsl:variable name="d" select="'0123456789ABCDEF'"/>
                <xsl:variable name="hi" select="string-length(substring-before($d, substring($hex, 1, 1)))"/>
                <xsl:variable name="lo" select="string-length(substring-before($d, substring($hex, 2, 1)))"/>
                <xsl:attribute name="{$what}-opacity">
                    <xsl:value-of select="format-number(($hi * 16 + $lo) div 255, '0.###')"/>
                </xsl:attribute>
            </xsl:when>
            <xsl:when test="string-length($hex) = 6">
                <xsl:attribute name="{$what}">
                    <xsl:value-of select="concat('#', $hex)"/>
                </xsl:attribute>
            </xsl:when>
            <xsl:otherwise>
                <xsl:message terminate="yes">vector-to-svg: unsupported colour '<xsl:value-of select="$argb"/>'</xsl:message>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <!-- Stops on any attribute of the current element that isn't android:<one of known>. -->
    <xsl:template name="known-attributes">
        <xsl:param name="known"/>
        <xsl:for-each select="@*[namespace-uri() != 'http://schemas.android.com/apk/res/android'
                or not(contains($known, concat('|', local-name(), '|')))]">
            <xsl:message terminate="yes">vector-to-svg: unsupported attribute <xsl:value-of select="name()"/> on &lt;<xsl:value-of select="name(..)"/>&gt;</xsl:message>
        </xsl:for-each>
    </xsl:template>

    <!-- Fail loudly on any element this converter does not understand. -->
    <xsl:template match="*">
        <xsl:message terminate="yes">vector-to-svg: unsupported element &lt;<xsl:value-of select="name()"/>&gt;</xsl:message>
    </xsl:template>
    <xsl:template match="text()"/>
    <xsl:template match="comment()"/>
</xsl:stylesheet>
