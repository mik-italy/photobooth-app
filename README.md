# Photobooth Android - Canon SELPHY CP1500 Edition 📸🖨️

Applicazione nativa Android (Kotlin + Jetpack Compose + CameraX) progettata specificamente per postazioni Photobooth su tablet, con supporto nativo alla stampante a sublimazione **Canon SELPHY CP1500**.

---

## ✨ Funzionalità Principali

1. **Formato Doppia Striscia 2x6 pollici:**
   * Generazione automatica di **due strisce fototessera gemelle da 5x15 cm (2x6")** affiancate su un unico foglio cartolina **10x15 cm** (1200 x 1800 px a 300 DPI).
   * Linea tratteggiata centrale di guida al taglio con icona a forbice ✂.
   * 3 scatti per sessione con grafica personalizzabile in calce (titolo evento, data, monogramma).

2. **Gestione Intelligente della Coda di Stampa (Anti-Accavallamento):**
   * **Blocco automatico dell'app durante la stampa:** poiché la Canon SELPHY CP1500 impiega circa 45 secondi (ciclo termico a 4 passaggi: Giallo -> Magenta -> Ciano -> Lucido protettivo), l'app entra in una schermata di attesa che **impedisce ad altri utenti di avviare nuovi scatti prima del completamento**.
   * **Monitoraggio stato reale via protocollo IPP (Porta 631):** l'applicazione interroga la stampante per verificare quando torna in stato `IDLE` (o sfrutta il timer di sicurezza a 45 secondi).
   * Messaggio visivo a schermo con indicatori di avanzamento per rassicurare gli ospiti.

3. **Interfaccia Ospite & Modalità Kiosk:**
   * Schermo sempre acceso (`FLAG_KEEP_SCREEN_ON`).
   * Schermo intero immersivo (status bar e navigation bar nascoste).
   * Schermata iniziale "Tocca per iniziare" con badge di stato della stampante (Verde = Pronta, Giallo = Occupata, Rosso = Offline).
   * Conto alla rovescia (3.. 2.. 1.. Sorridi!) con effetto flash fotocamera.
   * Anteprima prima della stampa con opzione "Rifai" o ritorno automatico alla home dopo 60 secondi di inattività.

4. **Pannello Admin Protetto da PIN:**
   * Accessibile premendo l'icona ingranaggio in alto a destra o con pressione prolungata (PIN di default: `1234`).
   * Configurazione IP e porta della stampante.
   * Tasto di **"Test Connessione"** istantaneo.
   * Personalizzazione dei testi dell'evento (es. "Matrimonio Sara & Luca", "15 Settembre 2026").
   * Durata del conto alla rovescia (3s o 5s).
   * Attivazione/disattivazione dell'attesa bloccante di stampa.

---

## 🔌 Come collegare il Tablet alla Canon SELPHY CP1500

### Metodo A: Wi-Fi Direct (Consigliato per Eventi - Nessun router necessario)
1. Sulla Canon SELPHY CP1500:
   * Vai su **Menu** > **Impostazioni Wi-Fi** > **Connessione diretta**.
   * Sullo schermo della stampante compariranno il nome della rete (es. `CP1500-xxxxxx`) e la password.
2. Sul Tablet Android:
   * Vai nelle impostazioni Wi-Fi e connettiti alla rete generata dalla SELPHY.
3. Nell'App Photobooth:
   * Apri le impostazioni (PIN `1234`).
   * L'indirizzo IP predefinito per il Wi-Fi Direct Canon è `192.168.4.1` (porta `631`).
   * Tocca **"Testa Connessione"**: se compare *"Pronta per stampare"* sei pronto per l'evento!

### Metodo B: Wi-Fi dell'evento o Hotspot
1. Connetti sia il tablet sia la Canon SELPHY alla stessa rete Wi-Fi.
2. Sulla SELPHY: leggi l'indirizzo IP assegnato (Menu > Impostazioni Wi-Fi > Dettagli IPv4).
3. Inserisci quell'indirizzo IP nell'app Photobooth.

---

## 📦 Come Compilare e Generare il file `.apk`

Il progetto include già tutto il necessario per la compilazione automatica. Puoi scegliere il metodo più comodo:

### Opzione 1: Compilazione Cloud Automatica con GitHub Actions (Gratuita e Immediata)
Non serve installare alcuno strumento sul tuo computer:
1. Crea un nuovo repository privato o pubblico su GitHub.
2. Carica i file di questa cartella (`git init`, `git add .`, `git commit -m "Photobooth"`, `git push`).
3. La pipeline preconfigurata in `.github/workflows/build-apk.yml` si avvierà in automatico.
4. Dopo circa 2-3 minuti, vai nella scheda **Actions** di GitHub, seleziona l'ultima esecuzione e scarica l'archivio **`PhotoboothApp-Debug-APK`** contenente il file `.apk` pronto all'installazione!

### Opzione 2: Con Android Studio
1. Apri **Android Studio** su qualsiasi computer (Mac, Windows o Linux).
2. Seleziona **Open** e apri la cartella di questo progetto:
   `/Users/mik/.gemini/antigravity/scratch/photobooth-app`
3. Attendi la sincronizzazione di Gradle.
4. Vai su **Build** > **Build Bundle(s) / APK(s)** > **Build APK(s)**.
5. Il file `.apk` verrà generato nella cartella `app/build/outputs/apk/debug/app-debug.apk`.

### Opzione 3: Da Riga di Comando (se hai JDK 17 e Android SDK)
```bash
cd /Users/mik/.gemini/antigravity/scratch/photobooth-app
./gradlew assembleDebug
```

---

## 📲 Installazione sul Tablet Android

1. Copia il file `.apk` sul tablet (via cavo USB, Google Drive, WhatsApp o chiavetta USB OTG).
2. Apri il file `.apk` sul tablet.
3. Se richiesto, autorizza l'installazione da fonti sconosciute.
4. All'avvio dell'app, concedi il **permesso per la fotocamera**.
5. Configura l'IP della Canon SELPHY nelle impostazioni con PIN `1234` e inizia a scattare!
