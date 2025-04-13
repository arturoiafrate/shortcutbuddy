@echo off
setlocal

REM ===========================================================
REM  Script di Avvio (con GOTO) per ShortcutBuddy (Windows)
REM ===========================================================
REM Descrizione:
REM   Avvia l'applicazione modulare JavaFX ShortcutBuddy, impostando
REM   correttamente il module-path e il percorso librerie native.
REM   Usa GOTO per gestire il controllo dell'esistenza di Java.
REM
REM Prerequisiti Utente:
REM   - Java (JRE o JDK) versione 21 (o compatibile) installato.
REM   - Il comando 'java' accessibile dal PATH di sistema.
REM
REM Struttura Cartelle Attesa:
REM   ./ShortcutBuddy-0.3.0.jar  (JAR Principale)
REM   ./libs/                    (JAR delle dipendenze)
REM   ./native/                  (DLL native - JNA, JNativeHook)
REM   ./run_shortcutbuddy.bat    (Questo script)
REM ===========================================================

REM --- Configurazione (Modifica SOLO se necessario) ---
set APP_JAR=ShortcutBuddy.jar
set LIBS_DIR=libs
set NATIVE_DIR=native
set APP_MODULE_NAME=it.arturoiafrate.shortcutbuddy
set MAIN_CLASS=it.arturoiafrate.shortcutbuddy.ShortcutBuddyApp
REM !! Aggiorna questa lista basandoti sui 'requires' del tuo module-info.java !!
set REQUIRED_MODULES=javafx.controls,javafx.fxml,javafx.graphics,java.desktop,com.github.kwhat.jnativehook,com.sun.jna,com.sun.jna.platform,org.apache.commons.io,com.google.gson,org.apache.commons.lang3,atlantafx.base,java.desktop,lombok,org.slf4j,org.kordamp.ikonli.javafx,org.kordamp.ikonli.feather,java.net.http,%APP_MODULE_NAME%
REM --- Fine Configurazione ---

set BASE_DIR=%~dp0
set NATIVE_LIB_PATH=%BASE_DIR%%NATIVE_DIR%

REM Definisci il Module Path (JAR dell'app + cartella libs)
REM Le virgolette gestiscono spazi nei percorsi
set MODULE_PATH="%BASE_DIR%%APP_JAR%";"%BASE_DIR%%LIBS_DIR%"

REM Verifica se java ? nel PATH eseguendo 'java -version' e controllando l'errore
java -version >nul 2>&1
if %errorlevel% neq 0 goto JavaNotFound

REM Se siamo qui, Java ? stato trovato, procedi con l'avvio
goto StartApplication

REM --- Sezione Gestione Errore Java Non Trovato ---
:JavaNotFound
echo [ERRORE] Comando 'java' non eseguibile o non trovato nel PATH. (Codice: %errorlevel%)
echo          Assicurati che Java (JRE/JDK 21+) sia installato correttamente e nel PATH.
goto EndScript

REM --- Sezione Avvio Applicazione ---
:StartApplication
echo Avvio %APP_JAR% (come modulo)...
echo Percorso librerie native: "%NATIVE_LIB_PATH%"
REM echo Module Path: %MODULE_PATH%
REM echo Add Modules: %REQUIRED_MODULES%
REM echo Main Module/Class: %APP_MODULE_NAME%/%MAIN_CLASS%
echo.

start "ShortcutBuddy" javaw -Djava.library.path="%NATIVE_LIB_PATH%" ^
     --module-path %MODULE_PATH% ^
     --add-modules %REQUIRED_MODULES% ^
     -m %APP_MODULE_NAME%/%MAIN_CLASS%

REM Controlla l'esito dell'applicazione principale
if %errorlevel% neq 0 (
    echo.
    echo [ATTENZIONE] L'applicazione si e' chiusa con un codice di errore: %errorlevel%.
    echo             Controlla eventuali messaggi di errore sopra.
    echo Premi un tasto per chiudere questa finestra...
    pause > nul
) else (
    echo.
    echo ShortcutBuddy avviato.
)

REM --- Sezione Finale Script ---
:EndScript
'echo.
'echo Premi un tasto per chiudere questa finestra...
'pause > nul
endlocal
exit /b 0