# RELEVANTOR Android Launcher Icon Forensic Diagnosis Report

**Diagnose-Datum:** 2026-08-02  
**Ausführendes System:** Google AI Studio (GAIS) Agent  
**Berichtstyp:** Rein lesende forensische Analyse der Android-Launcher- und Share-Dialog-Icon-Kette  
**Absoluter Berichts-Pfad:** `/app/applet/docs_md/RELEVANTOR_LAUNCHER_ICON_DIAGNOSIS_2026-08-02.md`  

---

## 1. Verifizierte Umgebung & Pfade

- **`pwd -P`:** `/app/applet`
- **Projekt-Root:** `/app/applet`
- **Aktives App-Modul:** `/app/applet/app`
- **Production SourceSet:** `/app/applet/app/src/main/`

---

## 2. AndroidManifest Icon-Kette

System-Analyse von `/app/applet/app/src/main/AndroidManifest.xml`:

- `<application>` Attribute:
  - `android:icon="@mipmap/ic_launcher"`
  - `android:roundIcon="@mipmap/ic_launcher_round"`
  - `android:banner`: **NICHT DEFINIERT** (kein Android TV Banner)
  - `android:logo`: **NICHT DEFINIERT**

---

## 3. Vollständige Auflösung der Ressourcenkette

```
AndroidManifest.xml (android:icon="@mipmap/ic_launcher")
  │
  ├──► API 26+ (Android 8.0+): app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml
  │     │
  │     ├──► <background android:drawable="@drawable/ic_launcher_background" />
  │     │     └─► app/src/main/res/drawable/ic_launcher_background.xml (Vector, Cream #fef0df) [INTAKT]
  │     │
  │     └──► <foreground android:drawable="@drawable/ic_launcher_foreground" />
  │           └─► app/src/main/res/drawable/ic_launcher_foreground.xml (Layer-List) [INTAKT]
  │                 └─► <item android:drawable="@drawable/relevantor_app_icon_v3" />
  │                       └─► app/src/main/res/drawable/relevantor_app_icon_v3.png [BESCHÄDIGT!]
  │
  └──► Legacy / Fallback (pre-API 26):
        ├─► app/src/main/res/mipmap-mdpi/ic_launcher.png [BESCHÄDIGT!]
        ├─► app/src/main/res/mipmap-hdpi/ic_launcher.png [BESCHÄDIGT!]
        ├─► app/src/main/res/mipmap-xhdpi/ic_launcher.png [BESCHÄDIGT!]
        ├─► app/src/main/res/mipmap-xxhdpi/ic_launcher.png [BESCHÄDIGT!]
        └─► app/src/main/res/mipmap-xxxhdpi/ic_launcher.png [BESCHÄDIGT!]
```

---

## 4. Detaillierter Integritätsstatus aller Icon- und Mipmap-Ressourcen

| Relativer Pfad | Typ | Existenz | Dateigröße | SHA-256 (Präfix) | PNG/XML Status | Dims | CRC/Valid | Referenziert von |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` | XML | **Ja** | 273 B | `1c832bf194d8...` | Valid Adaptive-Icon XML | N/A | PASS | `AndroidManifest.xml` |
| `app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml` | XML | **Ja** | 273 B | `1c832bf194d8...` | Valid Adaptive-Icon XML | N/A | PASS | `AndroidManifest.xml` |
| `app/src/main/res/drawable/ic_launcher_background.xml` | XML | **Ja** | 330 B | `36720c354452...` | Valid Vector XML | N/A | PASS | `ic_launcher.xml` |
| `app/src/main/res/drawable/ic_launcher_foreground.xml` | XML | **Ja** | 290 B | `c284c6ca3899...` | Valid Layer-List XML | N/A | PASS | `ic_launcher.xml` |
| `app/src/main/res/drawable/relevantor_app_icon_v3.png` | PNG | **Ja** | 2.661.058 B | `dbd58559d90b...` | **BESCHÄDIGT (UTF-8)** | N/A | **FAIL** | `ic_launcher_foreground.xml` |
| `app/src/main/res/drawable/relevantor_app_icon.png` | PNG | **Ja** | 2.661.058 B | `dbd58559d90b...` | **BESCHÄDIGT (UTF-8)** | N/A | **FAIL** | *Ungenutzt* |
| `app/src/main/res/drawable/relevantor_app_icon_v2.png` | PNG | **Ja** | 2.661.058 B | `dbd58559d90b...` | **BESCHÄDIGT (UTF-8)** | N/A | **FAIL** | *Ungenutzt* |
| `app/src/main/res/drawable-nodpi/relevantor_app_icon.png` | PNG | **Ja** | 1.473.657 B | `d3e2a4cb1110...` | **INTAKT PNG** | 1254x1254 | **PASS** | *Ungenutzt* |
| `app/src/main/res/mipmap-hdpi/ic_launcher.png` | PNG | **Ja** | 12.134 B | `7429b2a611ad...` | **BESCHÄDIGT (UTF-8)** | N/A | **FAIL** | Legacy Launcher |
| `app/src/main/res/mipmap-hdpi/ic_launcher_round.png` | PNG | **Ja** | 14.577 B | `cd819e657af4...` | **BESCHÄDIGT (UTF-8)** | N/A | **FAIL** | Legacy Launcher |
| `app/src/main/res/mipmap-mdpi/ic_launcher.png` | PNG | **Ja** | 6.221 B | `208ca9240701...` | **BESCHÄDIGT (UTF-8)** | N/A | **FAIL** | Legacy Launcher |
| `app/src/main/res/mipmap-mdpi/ic_launcher_round.png` | PNG | **Ja** | 7.501 B | `4a5a7499ee1a...` | **BESCHÄDIGT (UTF-8)** | N/A | **FAIL** | Legacy Launcher |
| `app/src/main/res/mipmap-xhdpi/ic_launcher.png` | PNG | **Ja** | 19.245 B | `8eb25d6dfe53...` | **BESCHÄDIGT (UTF-8)** | N/A | **FAIL** | Legacy Launcher |
| `app/src/main/res/mipmap-xhdpi/ic_launcher_round.png` | PNG | **Ja** | 22.903 B | `8cc9c1991528...` | **BESCHÄDIGT (UTF-8)** | N/A | **FAIL** | Legacy Launcher |
| `app/src/main/res/mipmap-xxhdpi/ic_launcher.png` | PNG | **Ja** | 38.674 B | `5a02b6ec9102...` | **BESCHÄDIGT (UTF-8)** | N/A | **FAIL** | Legacy Launcher |
| `app/src/main/res/mipmap-xxhdpi/ic_launcher_round.png` | PNG | **Ja** | 45.359 B | `b35167112827...` | **BESCHÄDIGT (UTF-8)** | N/A | **FAIL** | Legacy Launcher |
| `app/src/main/res/mipmap-xxxhdpi/ic_launcher.png` | PNG | **Ja** | 64.330 B | `f831bcca1ac2...` | **BESCHÄDIGT (UTF-8)** | N/A | **FAIL** | Legacy Launcher |
| `app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.png` | PNG | **Ja** | 74.214 B | `580c830f95a1...` | **BESCHÄDIGT (UTF-8)** | N/A | **FAIL** | Legacy Launcher |
| `assets/Relevantor-App-Icon_V3.png` | PNG | **Ja** | 1.473.657 B | `d3e2a4cb1110...` | **INTAKT PNG (Master)** | 1254x1254 | **PASS** | External Master |

---

## 5. Rolle von `drawable-nodpi/relevantor_app_icon.png`

**NEIN**

`app/src/main/res/drawable-nodpi/relevantor_app_icon.png` ist **KEIN** Bestandteil der Android-Launcher-Kette!

**Begründung:**
1. `AndroidManifest.xml` verweist auf `@mipmap/ic_launcher`.
2. `@mipmap/ic_launcher` verweist auf `mipmap-anydpi-v26/ic_launcher.xml`.
3. `ic_launcher.xml` verweist auf `@drawable/ic_launcher_foreground`.
4. `ic_launcher_foreground.xml` verweist explizit auf `@drawable/relevantor_app_icon_v3`.
5. `@drawable/relevantor_app_icon_v3` wird aus `app/src/main/res/drawable/relevantor_app_icon_v3.png` geladen.
6. Die Datei `drawable-nodpi/relevantor_app_icon.png` (Ressourcen-ID `@drawable/relevantor_app_icon`) wird von **keiner** Launcher-XML-Datei referenziert.
7. Die vorherige Wiederherstellung der Datei in `drawable-nodpi/` hatte daher technisch **0,0 % Einfluss** auf den Launcher und Share-Dialog, weil die Launcher-Kette weiterhin auf die beschädigte Datei `app/src/main/res/drawable/relevantor_app_icon_v3.png` zugriff!

---

## 6. Share-Dialog Icon (`ACTION_SEND`)

- Wenn eine externe App (z.B. Chrome) den Android Share-Dialog (`ACTION_SEND`) aufruft, liest das Android-Betriebssystem über den `PackageManager` das Icon der Ziel-Activity aus der `AndroidManifest.xml` aus.
- `PackageManager` ruft `loadIcon()` auf, was das `@mipmap/ic_launcher` Adaptive Icon lädt.
- Da der Vordergrund-Layer (`@drawable/relevantor_app_icon_v3`) aufgrund der PNG-Header-Beschädigung beim Decodieren durch `BitmapFactory` fehlschlägt (`null`), zeichnet Android nur den Hintergrund-Layer (den cremefarbenen Kreis `#fef0df`) oder fällt auf das Standard-Android-Icon zurück.

---

## 7. Adaptive Icon Ebenen & Fehleranalyse

- **Background Layer:** `drawable/ic_launcher_background.xml` (Intaktes Vektordrawable mit Farbe `#fef0df`).
- **Foreground Layer:** `drawable/ic_launcher_foreground.xml` (Intaktes Layer-List XML, verweist auf `@drawable/relevantor_app_icon_v3`).
- **Target Drawable for Foreground:** `app/src/main/res/drawable/relevantor_app_icon_v3.png` (**BESCHÄDIGT**).
- **Monochrome Layer:** Nicht definiert (optional für Android 13+ Themed Icons).

---

## 8. Wahrscheinlichste Ursache des Standard-Icons / leeren Kreises

Das Adaptive Icon lädt als Vordergrund die Datei `app/src/main/res/drawable/relevantor_app_icon_v3.png`.
Da diese Datei durch einen früheren UTF-8-Textmodus-Import beschädigt wurde (`0xEF 0xBF 0xBD` Byte-Sequenzen), kann das Betriebssystem das PNG nicht decodieren. Android rendert daher nur die verbleibende cremefarbene Hintergrundfläche oder verwendet das Android-System-Standardicon als Notfall-Fallback.

---

## 9. Minimale Reparaturstrategie (Vorschlag für spätere Umsetzung, NICHT JETZT)

Eine der folgenden beiden minimalinvasiven Einzelmaßnahmen stellt das Launcher- und Share-Dialog-Icon vollständig wieder her:

- **Option A (Ressourcen-Ebene):** Wiederherstellen der Datei `app/src/main/res/drawable/relevantor_app_icon_v3.png` aus der verifizierten Masterdatei `assets/Relevantor-App-Icon_V3.png` (bytegenaue 1:1 Kopie).
- **Option B (XML-Ebene):** Ändern der Zeile in `app/src/main/res/drawable/ic_launcher_foreground.xml` von:
  `android:drawable="@drawable/relevantor_app_icon_v3"`
  zu:
  `android:drawable="@drawable/relevantor_app_icon"`
  (da `@drawable/relevantor_app_icon` nun durch die intakte Datei in `drawable-nodpi/relevantor_app_icon.png` aufgelöst werden kann).

---

## 10. Integritätsprüfung

Vor und nach der Diagnose wurden SHA-256-Hashes aller 31 geschützten Projekt-Ressourcen verifiziert.
Es wurden **0 Dateien** des Projekts verändert, überschrieben, umbenannt oder gelöscht.

---

## 11. Abschlussstatus

**LAUNCHER-URSACHE GEFUNDEN**

---
*Ende des Berichts*
