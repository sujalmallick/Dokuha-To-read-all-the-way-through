/*!
 * ReactDrops Component License 1.0
 * Copyright (c) 2026 ReactDrops
 *
 * Ported and adapted from ReactDrops liquid-glass-logo-shaders.ts for
 * Compose Multiplatform (AGSL / SkSL).
 */
package ui.components.liquidglass

internal object LiquidGlassLogoShaderSource {

    /**
     * Builds the shared AGSL / SkSL fragment shader for the Liquid Glass Logo entrance.
     *
     * Uses `uniform shader uLogo;` (sampled in pixel coordinates `[0, uLogoPixelSize]`)
     * and avoids `fwidth` / `textureSize` / mutable globals for cross-backend compatibility
     * between Android 13+ AGSL (`RuntimeShader`) and iOS Skia (`RuntimeEffect`).
     */
    fun buildShaderSource(bands: Int = 6): String = """
        uniform shader uLogo;
        uniform float2 uResolution;
        uniform float2 uLogoPixelSize;
        uniform float uDpr;
        uniform float uUnit;
        uniform float2 uLogoBox;
        uniform float uTime;
        uniform float uSettle;
        uniform float uArtScale;
        uniform float3 uTile;
        uniform float uTileEnabled;
        uniform float uDepth;
        uniform float uDispersion;
        uniform float uRelief;
        uniform float uCaustics;
        uniform float uHighlights;
        uniform float uDropStrength;
        uniform float uPageLight;

        const float PI = 3.14159265359;
        const int BANDS = $bands;

        float sq(float x) { return x * x; }

        float bell(float x, float width) {
          float q = x / max(width, 0.0001);
          return exp(-q * q);
        }

        float ease(float a, float b, float x) {
          float t = clamp((x - a) / max(b - a, 0.0001), 0.0, 1.0);
          return t * t * (3.0 - 2.0 * t);
        }

        float3 spectrum(float s) {
          float wavelength = 400.0 + 300.0 * s;
          float3 rgb = float3(0.0);
          if (wavelength < 440.0) rgb = float3((440.0 - wavelength) / 120.0, 0.0, 1.0);
          else if (wavelength < 490.0) rgb = float3(0.0, (wavelength - 440.0) / 50.0, 1.0);
          else if (wavelength < 510.0) rgb = float3(0.0, 1.0, (510.0 - wavelength) / 20.0);
          else if (wavelength < 580.0) rgb = float3((wavelength - 510.0) / 70.0, 1.0, 0.0);
          else if (wavelength < 645.0) rgb = float3(1.0, (645.0 - wavelength) / 65.0, 0.0);
          else rgb = float3(1.0, 0.0, 0.0);
          return rgb * mix(0.2, 1.0, ease(400.0, 430.0, wavelength)) * mix(1.0, 0.65, ease(650.0, 700.0, wavelength));
        }

        float glassIor(float s, float spread) {
          float wavelength = 0.4 + 0.3 * s;
          return 1.38 + (0.04 / (wavelength * wavelength) - 0.12) * spread;
        }

        float4 imageAt(float2 p, float2 box) {
          float2 uv = 0.5 + p / max(box, float2(1.0));
          float2 px = uv * uLogoPixelSize;
          float4 ink = uLogo.eval(px);
          float inBounds = step(0.0, uv.x) * step(0.0, uv.y) * step(uv.x, 1.0) * step(uv.y, 1.0);
          return ink * inBounds;
        }

        float superRadius(float2 p, float n) {
          float2 a = max(abs(p), float2(0.00001));
          return pow(pow(a.x, n) + pow(a.y, n), 1.0 / n);
        }

        float2 superNormal(float2 p, float n) {
          return normalize(sign(p) * pow(max(abs(p), float2(0.00001)), float2(n - 1.0)) + float2(0.000001));
        }

        float hash1(float n) { return fract(sin(n * 127.1 + 311.7) * 43758.5453); }

        float contourNoise(float u) {
          float i = floor(u);
          float f = fract(u);
          f = f * f * (3.0 - 2.0 * f);
          return mix(hash1(i), hash1(i + 1.0), f);
        }

        float edgeWarp(float arc, float seed) {
          float broad = contourNoise(arc / max(uUnit * 0.037, 1.0) + seed) - 0.5;
          float fine = contourNoise(arc / max(uUnit * 0.008, 1.0) + seed + 29.0) - 0.5;
          return uUnit * (broad * 0.0017 + fine * 0.0008);
        }

        float4 refractedLogo(
          float2 p,
          float2 normalXY,
          float lensScale,
          float glass,
          float chromaticScale,
          float surfaceDistance,
          inout float3 outCoverage
        ) {
          float3 normal = normalize(float3(normalXY, 1.0));
          float3 color = float3(0.0);
          float3 weights = float3(0.0);
          float alpha = 0.0;
          float minCoverage = 1.0;
          float maxCoverage = 0.0;
          outCoverage = float3(0.0);

          float spread = uDispersion * chromaticScale * mix(0.85, mix(2.2, 1.5, ease(0.28, 0.9, uTime)), ease(0.25, 0.33, uTime));
          float3 neutralRay = refract(float3(0.0, 0.0, -1.0), normal, 1.0 / glassIor(0.5, spread));
          float2 neutralOffset = neutralRay.xy / max(-neutralRay.z, 0.25) * uUnit * uDepth * 0.80 * lensScale;

          for (int i = 0; i < BANDS; i++) {
            float s = float(i) / float(BANDS - 1);
            float ior = glassIor(s, spread);
            float3 ray = refract(float3(0.0, 0.0, -1.0), normal, 1.0 / ior);
            float2 offset = ray.xy / max(-ray.z, 0.25) * uUnit * uDepth * 0.80 * lensScale;

            float sphereRadius = uUnit * lensScale * 0.5;
            float sphereFocus = 1.0 - ease(0.225, 0.335, uTime);
            float bubbleQ = min(length(p) / max(sphereRadius, 1.0), 1.0);
            float openingFocus = 1.0 - ease(0.19, 0.265, uTime);
            float focusProfile = mix(0.7 + 0.6 * sq(bubbleQ), 1.0 - 0.9 * sq(bubbleQ), openingFocus);
            float2 bubbleSample = p * focusProfile + offset * mix(0.35, 0.12, openingFocus);
            bubbleSample += float2(0.025, -0.10) * uUnit * openingFocus;
            bubbleSample += (offset - neutralOffset) * openingFocus * 1.6;

            float2 sourcePoint = mix(p + offset, bubbleSample, sphereFocus);
            float4 ink = imageAt(sourcePoint, uLogoBox * uArtScale);
            ink *= mix(1.0, 1.0 - ease(0.72, 0.86, bubbleQ), openingFocus);

            float edgeShift = (s - 0.5) * uUnit * lensScale * 0.018 * uDispersion * chromaticScale;
            ink *= 1.0 - smoothstep(-0.8 / max(uDpr, 1.0), 0.8 / max(uDpr, 1.0), surfaceDistance - edgeShift);

            minCoverage = min(minCoverage, ink.a);
            maxCoverage = max(maxCoverage, ink.a);
            float3 w = spectrum(s);
            color += ink.rgb * w;
            alpha += ink.a * (w.r + w.g + w.b);
            outCoverage += ink.a * w;
            weights += w;
          }

          weights = max(weights, float3(0.0001));
          color /= weights;
          outCoverage /= weights;

          float coating = ease(-uUnit * lensScale * 0.022, 0.0, surfaceDistance);
          color.g *= 1.0 - 0.65 * coating * (maxCoverage - minCoverage);

          float youngFilm = (maxCoverage - minCoverage) * (1.0 - ease(0.19, 0.30, uTime));
          youngFilm *= 1.0 - ease(-uUnit * lensScale * 0.08, -uUnit * lensScale * 0.025, surfaceDistance);
          color.r += color.b * youngFilm * 0.80;
          color.b += color.r * youngFilm * 0.60;
          color.g *= 1.0 - youngFilm * 0.55;
          alpha /= (weights.r + weights.g + weights.b);

          float diffuse = max(dot(normal, normalize(float3(-0.4, -0.6, 1.0))), 0.0);
          float3 earlyWhite = mix(float3(0.90, 0.93, 0.95), float3(0.84, 0.86, 0.89), ease(0.21, 0.40, uTime));
          float3 whiteBalance = mix(earlyWhite, float3(0.87, 0.88, 0.885), ease(0.75, 1.22, uTime));
          color *= whiteBalance * ((0.90 + 0.10 * diffuse + glass * uCaustics * 0.04) *
            ((0.975 + 0.025 * sin(p.y / max(uUnit, 1.0) * PI)) *
            (1.0 + 0.09 * ease(0.75, 1.22, uTime) + 0.03 * (1.0 - ease(0.2, 0.5, uTime)))));

          return float4(color, alpha);
        }

        half4 main(float2 fragCoord) {
          float2 p = fragCoord - 0.5 * uResolution;
          float t = uTime;
          float settle = ease(0.0, 1.0, uSettle);
          float enter = ease(0.025, 0.16, t);
          float4 source = imageAt(p, uLogoBox * uArtScale);

          if (t > 1.58) {
            return half4(source * settle);
          }
          if (enter < 0.001) {
            return half4(0.0);
          }

          float shape = 2.0 + 3.0 * ease(0.29, 0.36, t);
          float expand = enter * (0.64 + 0.41 * (1.0 - exp(-max(t - 0.16, 0.0) * 5.8)));
          float collapse = 1.0 - ease(1.33, 1.52, t);
          float lensScale = max(0.001, mix(expand * mix(0.48, 1.0, collapse), 1.0, settle));
          float halfSize = max(uUnit * 0.5 * lensScale, 1.0);
          float radius = superRadius(p / halfSize, shape);
          float sd = (radius - 1.0) * halfSize;
          float aa = max(0.75 / max(uDpr, 1.0), 0.65);
          float mask = 1.0 - smoothstep(-aa, aa, sd);
          float glass = (1.0 - ease(1.05, 1.4, t)) * collapse * (1.0 - settle);

          if (sd > uUnit * 0.10) {
            return half4(0.0);
          }

          float2 outward = superNormal(p / halfSize, shape);
          float2 radial = normalize(p + float2(0.001));
          float diagonal = max(0.0, -2.0 * radial.x * radial.y);
          float shoulderStart = 0.6 + 0.18 * ease(0.66, 1.15, t) * (1.0 - diagonal) + 0.16 * ease(1.0, 1.22, t) * diagonal;
          float shoulder = ease(shoulderStart, 1.0, radius);
          float slope = (0.04 * radius + (0.85 + 0.2 * (1.0 - ease(0.28, 0.75, t))) * shoulder * shoulder) * glass * uDropStrength;
          slope += (1.0 - ease(0.18, 0.42, t)) * radius * 0.32 * uDropStrength;

          float2 opticalDirection = normalize(mix(outward, radial, 0.95));
          float astigmatism = 0.8 - 0.4 * radial.x * radial.y;
          float2 n = opticalDirection * slope * astigmatism * (1.0 - settle);

          float3 inkCoverage = float3(0.0);
          float4 ink = refractedLogo(p, n, lensScale, glass, 1.0, sd, inkCoverage);

          float illumination = 0.5 + 0.5 * dot(outward, normalize(float2(-0.5, -0.9)));
          float plateLight = ease(0.3, 0.8, dot(uTile, float3(0.299, 0.587, 0.114)));
          float3 body = uTile * (mix(0.55, 0.96, plateLight) + mix(0.45, 0.04, plateLight) * (1.0 - min(radius, 1.0)));
          body += float3(0.004, 0.007, 0.01) * bell(radius - 0.75, 0.5);

          float3 color = ink.rgb + body * (1.0 - inkCoverage) * glass * uTileEnabled;
          float plateFade = mix((1.0 - ease(1.08, 1.36, t)) * (1.0 - settle), glass, uPageLight);
          float alpha = max(ink.a, plateFade * uTileEnabled);

          // Spherical studio softbox highlight
          float2 spherePoint = p / halfSize;
          float3 sphereNormal = normalize(float3(spherePoint.x, -spherePoint.y, sqrt(max(0.001, 1.0 - dot(spherePoint, spherePoint)))));
          float3 sphereRay = reflect(float3(0.0, 0.0, -1.0), sphereNormal);
          float lightTravel = mix(-0.12, 0.20, ease(0.13, 0.30, t));
          float softbox = bell(sphereRay.x - lightTravel, 1.05) * bell(sphereRay.y - 0.56, 0.52);
          float sphericalLight = softbox * (1.0 - ease(0.245, 0.36, t)) * glass * uHighlights * 0.70;
          color += float3(0.50, 0.61, 0.72) * sphericalLight * (1.0 - ink.a * 0.85);
          alpha = max(alpha, sphericalLight);

          // Chromatic rim glints & caustic edge streaks
          float arc = atan(-p.y, p.x) * uUnit * 0.5;
          float grainScale = max(uUnit * 0.03, 1.0);
          float developed = ease(0.23, 0.48, t);
          float cornerExposure = ease(0.12, 0.8, 2.0 * radial.x * radial.y);
          float reliefScale = lensScale * developed * (0.2 + 0.8 * cornerExposure);
          float sharedWarp = edgeWarp(arc, 0.0) * reliefScale;
          float rimSd = sd + sharedWarp;
          float innerSd = sd + sharedWarp * 0.7 + edgeWarp(arc, 41.0) * reliefScale * 0.4;
          float inner2Sd = sd + sharedWarp * 0.45 + edgeWarp(arc, 83.0) * reliefScale * 0.6;
          float thickness = 0.75 + 0.5 * contourNoise(arc / max(uUnit * 0.013, 1.0) + 13.0);

          float around = arc / max(uUnit * 0.5, 1.0);
          float rimBob = uUnit * 0.0025 * developed * (0.6 * sin(7.0 * around + 0.4) * sin(t * 6.3 + 1.0) + 0.4 * sin(3.0 * around + 2.0) * sin(t * 4.1));
          float rimLineSd = rimSd + rimBob;
          float shimmer = mix(contourNoise(arc / grainScale), contourNoise(arc / grainScale + 211.0), 0.5 + 0.5 * sin(t * 5.0 + 6.2832 * contourNoise(arc / (grainScale * 3.0) + 7.0)));
          float rim = bell(rimLineSd + uUnit * 0.002, uUnit * 0.0018 * thickness + aa * 0.3) * (0.6 + 0.8 * shimmer);
          float centerFade = 1.0 - ease(0.62, 0.92, t);
          float key = pow(illumination, 5.0);
          float rimVisibility = 1.0 - 0.55 * ease(0.75, 1.22, t);
          float lowerRight = pow(0.5 + 0.5 * dot(float2(outward.x, -outward.y), normalize(float2(0.8, -0.6))), 5.0);
          lowerRight *= ease(-0.25, -0.05, p.x / max(uUnit, 1.0)) * (1.0 - ease(-0.22, -0.02, -p.y / max(uUnit, 1.0)));

          float drift = 0.7 + 0.6 * contourNoise(arc / max(uUnit * 0.09, 1.0) + 7.0);
          float filaments = (bell(innerSd + uUnit * 0.021, uUnit * 0.0035 * thickness) * 0.9 + bell(inner2Sd + uUnit * 0.028, uUnit * 0.004 / thickness) * 0.7) * centerFade + bell(rimSd + uUnit * 0.035, uUnit * 0.003 * thickness) * 0.5;
          float3 lowerRelief = (
            float3(0.03, 0.32, 1.0) * bell(sd + uUnit * 0.038, uUnit * 0.022 + aa * 0.2) * 0.55 +
            float3(0.22, 0.62, 1.0) * bell(sd + uUnit * 0.027, uUnit * 0.012 + aa * 0.1) * 0.7 +
            float3(0.75, 0.97, 1.0) * filaments * 1.35
          ) * drift * lowerRight * glass * uHighlights * uRelief * (1.0 - 0.85 * ease(0.6, 1.0, t)) * (1.0 - settle);

          float3 rimColor = float3(0.72, 0.86, 0.98) * rim * (0.35 + 0.65 * key) * rimVisibility * glass * uHighlights * uRelief;
          color += (rimColor + lowerRelief) * mask;
          alpha = clamp(max(alpha * mask, length(rimColor + lowerRelief) * mask * 0.5), 0.0, 1.0);

          // Blend smoothly into exact static source as settle reaches 1.0
          float4 settledSource = source * settle;
          float4 glassResult = float4(color * mask, alpha * mask);
          return half4(mix(glassResult, settledSource, settle));
        }
    """.trimIndent()
}
