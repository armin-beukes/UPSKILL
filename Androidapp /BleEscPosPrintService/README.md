# Trafman Print Bridge

Free Bluetooth-only Android ESC/POS bridge for Trafman web and Xprinter XP-P323B style printers.

This APK does not use RawBT and does not expose TCP/IP printing. The only printer connection path is Bluetooth Classic / SPP using UUID:

```text
00001101-0000-1000-8000-00805F9B34FB
```

Package:

```text
za.co.magnabc.trafmanprintbridge
```

## Intended Flow

1. The Android device is paired with the thermal printer in Android Bluetooth settings.
2. The user opens **Trafman Print Bridge** once, selects the paired printer, and saves it.
3. The Trafman web UI running in Chrome generates a print job.
4. A Trafman web button opens a Chrome Android intent URL.
5. Android finds **Trafman Print Bridge** and sends the job to the saved Bluetooth printer.

Chrome cannot silently launch an Android app without a user gesture. The Trafman web UI should open the bridge from a button click or similar user action.

## App Features

- Select an already-paired Bluetooth printer.
- Save selected printer locally.
- Print standalone plain text, ESC/POS receipt, QR, and feed/cut tests.
- Receive Trafman print jobs from Chrome using `intent://...`.
- Receive custom URI jobs using `trafmanprint://print?...`.
- Receive Android Intent jobs using `za.co.magnabc.trafmanprintbridge.PRINT`.
- Receive Android Share text.
- Show persistent logs for failed jobs.

## Trafman Repo Findings

Existing PDF/rendered document generation must remain the default pathway.

Useful integration points found in this workspace:

- Weigh document generation:
  - `Trafman-Weigh-BE/web/src/main/java/za/co/magnabc/web/rest/weigh/vehicleweigh/DocumentsGenerationController.java`
  - `Trafman-Weigh-BE/service/src/main/java/za/co/magnabc/service/weigh/vehicleweigh/DocumentsGenerationManagerService.java`
- Weigh rendered document endpoints:
  - `Trafman-Weigh-BE/web/src/main/java/za/co/magnabc/web/rest/weigh/conceptualdocument/renderer/RenderedWeighDocumentController.java`
- Transgression rendered documents:
  - `Trafman-Transgressions-BE/web/src/main/java/za/co/magnabc/web/rest/RenderedChargeSheetController.java`
  - `Trafman-Transgressions-BE/web/src/main/java/za/co/magnabc/web/rest/RenderedTransgressionDocumentController.java`
- Payment receipt flow:
  - `Trafman-Transgressions-BE/web/src/main/java/za/co/magnabc/web/rest/PaymentReceiptController.java`
  - `Trafman-Transgressions-BE/service/src/main/java/za/co/magnabc/service/payment/conceptualdocumentrenderer/ProvidePaymentReceiptService.java`
- Warrant printing flow:
  - `Trafman-Transgressions-BE/web/src/main/java/za/co/magnabc/web/rest/WarrantOfArrestPrintingController.java`
- Roles:
  - `Trafman-Core-BE/application/src/main/resources/roles.yml`
  - Relevant roles include document generation, rendered weigh documents, rendered charge sheet documents, transgression printing, and warrant printing.

Recommended integration: keep existing PDFs exactly as-is and add a receipt-width ESC/POS pathway next to those service boundaries. Do not try to print full A4 PDFs on a 72 mm thermal printer.

Optional config properties were added disabled-by-default in:

- `Trafman-Weigh-BE/application/src/main/resources/application.properties`
- `Trafman-Transgressions-BE/application/src/main/resources/application.properties`

```properties
trafman.mobile-escpos.enabled=false
trafman.mobile-escpos.bridge-mode=CHROME_INTENT_URI
trafman.mobile-escpos.connection-type=BLUETOOTH
trafman.mobile-escpos.android-package=za.co.magnabc.trafmanprintbridge
trafman.mobile-escpos.android-action=za.co.magnabc.trafmanprintbridge.PRINT
trafman.mobile-escpos.uri-scheme=trafmanprint
```

## Build APK

From this project directory:

```bash
JAVA_HOME=/tmp/rawbt-toolchain/jdk17 \
GRADLE_USER_HOME=/tmp/rawbt-gradle-home \
_JAVA_OPTIONS=-Djava.io.tmpdir=/tmp/rawbt-java-tmp \
/tmp/rawbt-toolchain/gradle/gradle-7.2/bin/gradle assembleDebug
```

Output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Convenience copy:

```text
D:\DEVOPS\TrafmanT400\TrafmanPrintBridge-debug.apk
```

## Install And Pair

1. Install `TrafmanPrintBridge-debug.apk`.
2. Open **Trafman Print Bridge**.
3. Grant Nearby devices / Bluetooth permission on Android 12+.
4. Pair the printer in Android Bluetooth settings if it is not already paired.
5. Tap **Refresh paired Bluetooth printers**.
6. Select the printer.
7. Tap **Save printer settings**.
8. Tap **Print ESC/POS receipt test**.

## Chrome Web Integration

For Trafman web running in Android Chrome, prefer an Android intent URL:

```text
intent://print?payloadType=ESC_POS_BASE64&printerMode=BLUETOOTH&documentType=NOTICE_RECEIPT&documentNumber=ABC123456&payload=<url-safe-base64>#Intent;scheme=trafmanprint;package=za.co.magnabc.trafmanprintbridge;end
```

JavaScript example:

```javascript
function sendToTrafmanPrintBridge(escposBytesBase64UrlSafe, documentType, documentNumber) {
  const params = new URLSearchParams({
    payloadType: "ESC_POS_BASE64",
    printerMode: "BLUETOOTH",
    documentType,
    documentNumber,
    payload: escposBytesBase64UrlSafe
  });

  window.location.href =
    `intent://print?${params.toString()}` +
    "#Intent;scheme=trafmanprint;package=za.co.magnabc.trafmanprintbridge;end";
}
```

The payload should be URL-safe Base64, or a normal Base64 string passed through `encodeURIComponent`.

## Custom URI Integration

The app also accepts:

```text
trafmanprint://print?payloadType=ESC_POS_BASE64&printerMode=BLUETOOTH&documentType=NOTICE_RECEIPT&documentNumber=ABC123456&payload=<base64>
```

Plain text:

```text
trafmanprint://print?payloadType=TEXT&printerMode=BLUETOOTH&text=Hello%20from%20Trafman
```

## Android Intent Integration

Action:

```text
za.co.magnabc.trafmanprintbridge.PRINT
```

String extras:

- `jobJson`: full JSON print job
- `payloadType`: `ESC_POS_BASE64` or `TEXT`
- `payload`: Base64 ESC/POS or text, depending on `payloadType`
- `printerMode`: `BLUETOOTH`
- `documentType`
- `documentNumber`

Example JSON:

```json
{
  "sourceSystem": "TRAFMAN",
  "documentType": "NOTICE_RECEIPT",
  "documentNumber": "ABC123456",
  "printerMode": "BLUETOOTH",
  "copies": 1,
  "payloadType": "ESC_POS_BASE64",
  "payload": "<base64 bytes>",
  "metadata": {
    "authority": "",
    "weighbridge": "",
    "user": "",
    "transactionId": ""
  }
}
```

## Troubleshooting

- **Permission missing**: grant Nearby devices / Bluetooth permission in Android app settings.
- **BLUETOOTH_SCAN error**: install version `0.1.1` or newer. The bridge uses paired devices and does not call Android discovery cancellation.
- **Printer not paired**: pair the printer in Android Bluetooth settings, then refresh in the app.
- **Connection failed**: confirm the printer is powered on, not connected to another phone, and in ESC/POS mode.
- **QR does not print**: the printer may not support the ESC/POS QR command or may be in another emulation/label mode.
- **Cut does not work**: some 72 mm printers do not have a cutter or ignore cut commands.
- **Trafman job received but no output**: open **View logs** in the app and check whether decoding, Bluetooth permission, or connection failed.

## Licensing Notes

This implementation currently uses only Android platform APIs and custom ESC/POS byte generation.

DantSu/ESCPOS-ThermalPrinter-Android remains a good MIT-licensed future option if richer printer support is needed. The GPLv3 open-escpos-print-service project is useful as a reference, but GPL code should not be embedded in Trafman unless the licensing impact is explicitly accepted.
