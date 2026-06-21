# LOCAL BUILD HANDOFF & ANLEITUNG FÜR ANDROID STUDIO

Dieses Dokument beschreibt, wie das Abstractor-Projekt nach dem Herunterladen aus Google AI Studio (GAIS) lokal in Android Studio eingerichtet, gebaut, signiert und auf einem physischen Android-Endgerät installiert wird.

Da die AI Studio-Download- und Export-Pipeline gelegentlich die Integrität von APK- oder ZIP-Dateien im Browser beschädigt, garantiert der lokale Build in Android Studio volle Unabhängigkeit von Cloud-Übertragungsfehlern.

---

## 1. Voraussetzungen & Import

### Android Studio installieren
Lade das aktuelle **Android Studio** (Koala, Ladybug oder neuer) herunter und installiere es:
[Android Studio offizielle Webseite](https://developer.android.com/studio)

### Projekt entpacken & öffnen
1. Exportiere das vollständige Projekt-ZIP-Archiv aus Google AI Studio (**Settings > Export project ZIP**).
2. Entpacke dieses ZIP-Archiv in ein beliebiges lokales Verzeichnis.
3. Starte **Android Studio**.
4. Wähle **File > Open** (oder *Open an Existing Project*) und navigiere zum entpackten Hauptverzeichnis des Projekts (das Verzeichnis, das `settings.gradle.kts` und das `/app`-Unterverzeichnis enthält).

---

## 2. Lokales Bauen / Compiling

### Gradle Sync
* Android Studio führt beim ersten Öffnen automatisch einen **Gradle Sync** aus, um alle Abhängigkeiten gemäß `gradle/libs.versions.toml` zu laden.
* Sollte dieser fehlschlagen oder nicht automatisch starten, kannst du ihn manuell anstoßen:
  **File > Sync Project with Gradle Files** oder über das kleine Elefanten-Symbol in der oberen Werkzeugleiste.

### APK erzeugen über die Benutzeroberfläche
1. Gehe im oberen Menü auf **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
2. Sobald der Vorgang abgeschlossen ist, wird unten rechts eine Benachrichtigung angezeigt. Klicke dort auf **locate**, um direkt zum Ordner mit der fertigen APK-Datei zu springen.
3. Der Standard-Ausgabepfad lautet:
   `app/build/outputs/apk/debug/app-debug.apk`

### APK erzeugen über das Terminal
Alternativ kannst du die APK im eingebauten Terminal von Android Studio (unten links) oder deiner Systemkonsole mit folgendem Befehl kompilieren:
```bash
./gradlew assembleDebug
```
Unter Windows (Eingabeaufforderung):
```cmd
gradlew.bat assembleDebug
```

---

## 3. Installation auf einem Android-Gerät

Um die App auf deinem physischen Smartphone oder Tablet zu installieren:

### Vorbereitung auf dem Android-Gerät
1. Deinstalliere unbedingt alle vorhandenen oder unvollständigen Versionen der **Abstractor**-App von deinem Gerät. Dies verhindert Konflikte mit Zertifikatssignaturen (da die Unterschriften von lokal gebauten und GAIS-gebauten APKs voneinander abweichen können).
2. Aktiviere die **Entwickleroptionen** und das **USB-Debugging** auf deinem Gerät:
   * Gehe zu *Einstellungen > Telefoninfo* und tippe 7-mal schnell auf die *Build-Nummer*.
   * Gehe zurück zu *Einstellungen > System > Entwickleroptionen*.
   * Aktiviere **USB-Debugging**.

### Installation direkt aus Android Studio
1. Verbinde dein Gerät per USB-Kabel mit deinem Computer. Auf dem Handy-Display erscheint eventuell eine Anfrage, ob du USB-Debugging zulassen möchtest. Bestätige diese.
2. In der oberen Symbolleiste von Android Studio sollte dein Gerät nun im Dropdown-Menü neben dem grünen Play-Button (`Run 'app'`) erscheinen.
3. Klicke auf den **grünen Play-Button**. Android Studio baut das Projekt erneut, überträgt das APK aufs Handy und startet die App direkt.

### Manuelle Installation via ADB (Alternative)
Wenn du ADB installiert hast, kannst du die zuvor erzeugte APK auch manuell per Terminal auf dein per USB verbundenes Handy übertragen:
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 4. Fehlerüberprüfung & Diagnose bei Installationsfehlern

### Was tun bei Fehler "App wurde nicht installiert" / "Fehler beim Parsen des Pakets"?
Sollte die Installation der über Android Studio lokal gebauten APK fehlschlagen, führe die folgenden Prüfungen durch:

1. **Unvollständige Deinstallation:** Häufig bleibt ein Package-Objekt unter einem anderen Benutzerprofil oder im Arbeitsbereich (Work Profile) des Telefons aktiv. Gehe in den App-Manager deines Smartphones, suche nach "Abstractor" und stelle sicher, dass sie für *alle Benutzer* vollständig entfernt wurde.
2. **Paket-Integrität prüfen:**
   Prüfe via Kommandozeile, ob das APK beschädigt ist:
   * Unter macOS / Linux: `unzip -t app/build/outputs/apk/debug/app-debug.apk`
   * Wenn `unzip` einen Fehler anzeigt, ist das Dateisystem fehlerhaft.
3. **Signatur überprüfen:**
   Führe die Signaturprüfung über das Android SDK-Tool `apksigner` oder Google JDK `jarsigner` aus:
   ```bash
   apksigner verify --print-certs app/build/outputs/apk/debug/app-debug.apk
   ```
4. **Beweis im Fehlerfall:**
   * Wenn das lokal in Android Studio kompilierte und installierte APK reibungslos startet und ausgeführt werden kann, aber das aus Google AI Studio im Browser heruntergeladene APK Fehler erzeugt:
     **Dann liegt definitiv eine Beschädigung durch den Browser-Download oder die Export-Pipeline von Google AI Studio vor (Integritätsverlust bei der Dateitransmission). Es liegt kein Problem im Quellcode der App vor.**
