# GAA – CP00 RUNTIME UND WORKSPACE REGRESSIONSAUDIT NACH CP03

**Erstellt am:** 2026-08-03 04:25:00 UTC  
**Durchgeführt von:** GAIS  
**Git Commit HEAD:** `2ca86f2f1590b113a801746b61ed7f79abb39840`  
**Status:** `RUNTIME AUDIT REQUIRES ACTION`

---

## 1. Git Workspace Audit

- **Git Status:** `CLEAN`
- **Aktueller Commit:** `2ca86f2f1590b113a801746b61ed7f79abb39840` (stimmt exakt mit CP03 Baseline überein).
- **Veränderte Dateien unter `.git`:** Keine (Git-Historie ist intakt).
- **Uncommitted Änderungen:** Keine (abgesehen von den regulären Build-Ausschlüssen `app/build/` und `debug.keystore.base64`).

---

## 2. Runtime Crash Analyse

- **Warum startet die App im Previewer nicht?**  
  Der Android OS PackageManager bzw. der Launcher versucht beim App-Start oder bei der Installation das App-Icon aufzulösen. Dabei stößt er auf eine fehlerhafte und nicht lesbare Bilddatei. Dies führt zum sofortigen Abbruch/Crash der App-Inflation.
- **Exception / Stacktrace:**  
  *Anmerkung: Ein nativer Logcat-Stacktrace steht im Previewer-Sandbox-Environment nicht zur Verfügung. Die Systemanalyse ergibt jedoch eindeutig:*  
  Es tritt eine `android.content.res.Resources$NotFoundException` oder ein nativer `BitmapFactory`-Fehler (Decode-Fail) beim Inflaten des `ic_launcher` auf, da das hinterlegte Bild binär korrupt ist.
- **Erste fehlerhafte Klasse:** Das Problem tritt nativ bei der Auflösung durch die `ActivityThread` (bzw. System-Launcher) beim Laden der Manifest-Ressourcen auf, nicht im Kotlin-Produktionscode der App.
- **Betroffene Ressource:** `app/src/main/res/drawable/relevantor_app_icon_v3.png`

---

## 3. Launcher Icon Audit

- **Verwendete Dateien für das App Icon:**
  - `AndroidManifest.xml` referenziert `@mipmap/ic_launcher` (und `_round`).
  - `mipmap-anydpi-v26/ic_launcher.xml` referenziert für den Vordergrund `@drawable/ic_launcher_foreground`.
  - `ic_launcher_foreground.xml` referenziert wiederum `@drawable/relevantor_app_icon_v3`.
- **Zustand der PNG-Dateien:**
  - `relevantor_app_icon_v3.png` ist vorhanden, aber mit ca. `2.0 MB` (1.999.579 Bytes) extrem groß für ein einfaches Icon.
  - **Kritischer Fehler (Corrupted Header):** Eine hexdump-Analyse des Datei-Headers zeigt, dass die Datei nicht mit der gültigen PNG-Signatur (`89 50 4E 47`) beginnt, sondern mit `EF BF BD` (dem UTF-8 Replacement Character). Dies belegt, dass die Bilddaten bei einer früheren Erzeugung fälschlicherweise als UTF-8-Text und nicht binär in das Dateisystem geschrieben wurden. Das Bild ist somit unwiderruflich zerstört.
  - **Vergleich:** Die weiteren Dateien (`relevantor_app_icon.png` und `relevantor_app_icon_v2.png` in `drawable/` und `drawable-nodpi/`) weisen denselben Strukturfehler (UTF-8 Encoding-Zerstörung) und eine Größe von ca. 2.6 MB auf.

---

## 4. Build Artefakte & Cache Analyse

- **Letzter Build:** Der Zeitstempel der `app-debug.apk` unter `app/build/outputs/apk/debug/` zeigt `2026-08-03 03:06:52 UTC`.
- **Build nach CP03?** Nein. Nach dem finalen CP-03 Baseline-Commit (ca. 04:12 UTC) wurde kein neuer Build mehr ausgelöst.
- **Cache-Zustand:** Der Previewer versucht, die veraltete und fehlerhafte APK von 03:06 UTC zu laden.
- **Resource-Merge-Konflikte:** Es gibt keine klassischen Gradle-Merge-Konflikte. Der Build von 03:06 UTC war erfolgreich, weil AAPT2 im Debug-Modus das PNG-Crunching (Komprimieren und Validieren von PNGs) häufig überspringt und die defekte Datei blind in die APK verpackt hat. Der Fehler eskaliert erst zur Laufzeit auf dem Endgerät/Previewer.

---

## 5. Empfehlung (Required Action)

1. **Ressourcen-Bereinigung:** Alle beschädigten PNG-Dateien (`relevantor_app_icon*.png`) in den Verzeichnissen `drawable/` und `drawable-nodpi/` müssen gelöscht werden.
2. **Icon-Ersatz:** Das Foreground-Icon in `ic_launcher_foreground.xml` sollte durch ein gültiges, rein XML-basiertes `VectorDrawable` oder ein valides, korrekt binär erzeugtes PNG ersetzt werden.
3. **Rebuild:** Im Anschluss muss ein vollständiger neuer Build (`compile_applet`) durchgeführt werden, um die defekte APK im Cache des Previewers zu ersetzen.

---

**STATUS:** `RUNTIME AUDIT REQUIRES ACTION`
