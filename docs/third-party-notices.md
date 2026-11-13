# Third-party notices in the Android distribution

The release app packages the readable index at `app/src/main/assets/licenses/index.txt`, complete license/terms texts beside it, a coordinate inventory (`dependency-metadata.csv`), and preserved archive notices (`bundled-dependency-notices.txt`). These notices do not select a license for Penny Circle's own source.

## Evidence and scope

On 2026-11-13, the existing `release-artifact-dependencies.xml` report's **package** roots supplied 119 unique external Maven coordinates. For each coordinate, the matching resolved Gradle-cache POM and AAR/JAR were inspected; 51 license/notice entries were copied verbatim, including entries inside AAR `classes.jar`. No unrelated cache, private signing directory, inbox or device was scanned. Debug/test dependencies are excluded. Dependency upgrades require regenerating this inventory and rechecking licenses/notices. The report is generated build output, so a fresh final build should be checked for coordinate changes and asset inclusion.

The CSV records exact versions and publisher-declared license names/URLs. Two inherited license entries were resolved from authoritative Maven Central parent POMs: [Guava parent 26.0-android](https://repo.maven.apache.org/maven2/com/google/guava/guava-parent/26.0-android/guava-parent-26.0-android.pom) and [ZXing parent 3.5.3](https://repo.maven.apache.org/maven2/com/google/zxing/zxing-parent/3.5.3/zxing-parent-3.5.3.pom). Original entries from archives are retained independently of app-level `/META-INF/{AL2.0,LGPL2.1}` exclusions.

## License mapping

| Components | Evidence and packaged files |
| --- | --- |
| Geist variable 1.800 and Geist Mono variable 1.701 | Actual font `name` table IDs 0, 5, 13 and 14 identify copyright 2024 Geist Project Authors and SIL OFL 1.1. `index.txt` preserves both exact copyright statements. `Geist-OFL-1.1.txt` is the complete [official 1.8.0 OFL](https://github.com/vercel/geist-font/blob/1.8.0/OFL.txt), matching that year; no claim that both font binaries are byte-identical to that release is made. |
| AndroidX, Kotlin/Kotlinx, JetBrains annotations, JSpecify, javax.inject, Google Data Transport, Firebase components/encoders/annotations, Guava ListenableFuture | Their resolved publisher POMs declare Apache 2.0 (Guava via parent). Full `APACHE-2.0.txt` is from [Apache](https://www.apache.org/licenses/LICENSE-2.0.txt). Archive notices are retained in the combined notices file. These Firebase transitives do not establish use of Firebase analytics or a hosted application backend. |
| ZXing core 3.5.3 | Apache 2.0 per parent POM and [official license](https://github.com/zxing/zxing/blob/zxing-3.5.3/LICENSE). `ZXing-LICENSE.txt` and [project NOTICE](https://github.com/zxing/zxing/blob/zxing-3.5.3/NOTICE) (`ZXing-NOTICE.txt`) are preserved. The project-wide notice names Barcode4J/JCommander; that does not assert both are runtime dependencies here. |
| Iroh JVM and Android 1.2.0 | POMs declare MIT/Apache 2.0; complete [MIT](https://github.com/n0-computer/iroh-ffi/blob/v1.2.0/LICENSE-MIT) and [Apache](https://github.com/n0-computer/iroh-ffi/blob/v1.2.0/LICENSE-APACHE) texts from that tag are retained, including N0 copyright. |
| JNA 5.15.0 and bundled libffi | POM and [upstream choice statement](https://github.com/java-native-access/jna/blob/5.15.0/LICENSE) permit Apache 2.0 or LGPL; this distribution uses Apache 2.0. `JNA-LICENSE.txt`, full `JNA-APACHE-2.0.txt`, [libffi license](https://github.com/java-native-access/jna/blob/5.15.0/native/libffi/LICENSE) and original archive texts are retained. The alternative LGPL text is preserved without selecting it. |
| ML Kit, Google code scanner, Play services ML text-recognition components | POMs declare [ML Kit terms](https://developers.google.com/ml-kit/terms), incorporating [Google APIs terms](https://developers.google.com/terms). Full readable article snapshots are packaged in `Google-ML-Kit-terms.txt` and `Google-APIs-terms.txt`. These components are not labelled Apache. |
| Play services base/basement/tasks and ODML image | POMs declare the [Android SDK License Agreement](https://developer.android.com/studio/terms); complete article snapshot is `Android-SDK-terms.txt`. These components are not labelled Apache. |

Google terms were downloaded from official pages and converted to readable article text with navigation/HTML omitted; section headings, terms and links remain. Their inclusion is informational and does not replace the distributor's obligations under those terms. ML Kit keeps image/text inputs and outputs on-device but documents performance/utilization metrics and Google network requests for updates/model compatibility; app privacy wording must reflect that distinction. No app-specific redistribution prohibition was found in this bounded review, and this is not a legal certification.

The retained archive and upstream notices cover the resolved Maven artifacts and known JNA libffi notice. This was not an independent reconstruction of every Rust/native transitive dependency's source-license provenance. No new native source was vendored or changed. A complete supply-chain/legal audit remains outside this check.

The synthetic Indian SMS corpus stays test-only, with CC BY 4.0 attribution in `core/src/test/resources/sms-corpus/README.md`; it is not an APK runtime asset. No source license has been added for Penny Circle.

## Verification

All files named by the asset index exist and are readable UTF-8. Full texts include the OFL ending, Apache terms, MIT warranty paragraph, Google SDK final clause, and Google API final governing-law clauses. Final APK asset inclusion and the Settings reader must be verified by the release owner after rebuilding; this audit performed no build or device action.
