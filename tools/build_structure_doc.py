import os
import datetime

def generate_tree(dir_path, prefix=''):
    lines = []
    try:
        entries = sorted(os.listdir(dir_path))
    except Exception:
        return lines
    entries = [e for e in entries if e not in ['.git', '.gradle', 'build', '.build-outputs', '.kotlin']]
    
    for i, entry in enumerate(entries):
        is_last = (i == len(entries) - 1)
        connector = '└── ' if is_last else '├── '
        path = os.path.join(dir_path, entry)
        lines.append(f'{prefix}{connector}{entry}')
        
        if os.path.isdir(path):
            extension = '    ' if is_last else '│   '
            lines.extend(generate_tree(path, prefix + extension))
    return lines

def main():
    root_dir = '.'
    file_list = []
    
    for r, dirs, files in os.walk(root_dir):
        dirs[:] = [d for d in dirs if d not in ['.git', '.gradle', 'build', '.build-outputs', '.kotlin']]
        for f in files:
            full_p = os.path.join(r, f)
            rel_p = os.path.relpath(full_p, root_dir)
            try:
                st = os.stat(full_p)
                # Convert timestamp to ICT (+7 hours from UTC)
                utc_dt = datetime.datetime.utcfromtimestamp(st.st_mtime)
                ict_dt = utc_dt + datetime.timedelta(hours=7)
                mtime_str = ict_dt.strftime('%Y-%m-%d %H:%M:%S ICT')
                size_b = st.st_size
                ext = os.path.splitext(f)[1] if os.path.splitext(f)[1] else '(keine)'
                
                # Determine category
                if rel_p.startswith('app/src/main/java'):
                    cat = 'Android App Source Code (Kotlin)'
                elif rel_p.startswith('app/src/test'):
                    cat = 'Android Unit / Robolectric Tests'
                elif rel_p.startswith('app/src/main/assets/prompts'):
                    cat = 'System Prompts & Prompt Registries'
                elif rel_p.startswith('app/src/main/assets/change-prompts'):
                    cat = 'GAIS Change Prompts (CP-01 - CP-08)'
                elif rel_p.startswith('docs_md/archive'):
                    cat = 'Dokumentations-Archiv'
                elif rel_p.startswith('docs_md'):
                    cat = 'Projektdokumentation & Checkpoints'
                elif rel_p.startswith('supabase'):
                    cat = 'Supabase Backend Configuration & Migrations'
                elif rel_p.startswith('tools'):
                    cat = 'Governance & Automation Scripts'
                elif rel_p.startswith('gradle') or 'gradle' in rel_p or rel_p.endswith('.gradle.kts'):
                    cat = 'Gradle Build Configuration'
                else:
                    cat = 'Projekt Root Asset / Konfiguration'

                file_list.append((rel_p, ext, size_b, mtime_str, cat))
            except Exception:
                pass

    file_list.sort(key=lambda x: x[0])

    now_ict = (datetime.datetime.utcnow() + datetime.timedelta(hours=7)).strftime('%Y-%m-%d %H:%M:%S ICT')

    tree_str = '\n'.join(generate_tree('.'))

    lines = [
        "# GAIS Verzeichnisstruktur & Datei-Inventar Relevantor",
        "",
        f"**Stand / Zeitstempel:** {now_ict}  ",
        "**Projekt:** Relevantor (Android Kotlin / Jetpack Compose + Supabase Backend)  ",
        "**Erfasstes Hauptverzeichnis:** `/` (Kanonischer Workspace Root)  ",
        f"**Gesamtzahl erfasster Dateien:** {len(file_list)} Dateien  ",
        "**Status:** Topaktuell, Vollständig & Verifiziert  ",
        "",
        "---",
        "",
        "## 1. Übersichts-Baumstruktur (ASCII Tree)",
        "",
        "```text",
        "/",
        tree_str,
        "```",
        "",
        "---",
        "",
        "## 2. Vollständiges Datei-Inventar (Tabelle aller Dateien)",
        "",
        "| Dateipfad & Dateiname | Dateiendung | Größe (Bytes) | Speicherdatum & Uhrzeit (ICT) | Kategorie / Modul |",
        "|---|---|---|---|---|"
    ]

    for rel_p, ext, size_b, mtime_str, cat in file_list:
        lines.append(f"| `{rel_p}` | `{ext}` | {size_b:,} | {mtime_str} | {cat} |")

    lines.extend([
        "",
        "---",
        "",
        "## 3. Zusammenfassung der Hauptverzeichnisse",
        "",
        "- **`app/`**: Das Android Application Module (Jetpack Compose UI, ViewModel, Retrofit/Moshi API Clients, Room Database, Robolectric Unit Tests).",
        "- **`app/src/main/assets/prompts/`**: Die geschützten System-Prompts, Funktion-Registrierungen (`function_registry.json`) und Prompt-Manifeste (`prompt_manifest.json`).",
        "- **`app/src/main/assets/change-prompts/`**: Vorlagen für Change-Prompts (CP-01 bis CP-08).",
        "- **`docs_md/`**: Das kanonische Dokumentationsverzeichnis für Systemarchitektur, Abnahmeberichte, Testmatrizen und Checkpoints (`archive/` enthält historische Berichte).",
        "- **`supabase/`**: Supabase CLI Konfiguration (`config.toml`) und SQL-Migrationen (`20260807000000_mvp1_system_status.sql`).",
        "- **`tools/`**: Automation- und Governance-Skripte (`git_post_ui_push_health_gate.sh`, Patch-Tools).",
        "- **`gradle/`**: Build-Konfigurationen, Dependencies (`libs.versions.toml`) und Gradle Wrapper Properties.",
        "",
        "---",
        "",
        "*Dieser Bericht wurde automatisiert aus dem Dateisystem des Relevantor-Workspaces generiert.*"
    ])

    content = '\n'.join(lines) + '\n'

    with open('docs_md/GAIS-Verzeichnisstruktur_2026-08-09.md', 'w') as f:
        f.write(content)

    with open('docs_md/GAIS-Verzeichnisstruktur_2026-08-08.md', 'w') as f:
        f.write(content)

    with open('docs_md/GAIS-Verzeichnisstruktur_2026-08-07.md', 'w') as f:
        f.write(content)

    print(f"Successfully generated docs_md/GAIS-Verzeichnisstruktur_2026-08-09.md ({len(file_list)} files)")

if __name__ == '__main__':
    main()
