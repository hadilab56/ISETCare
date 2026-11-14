# ISETCare - application Android (Kotlin + Java)

Gestion intelligente du matériel et des réclamations techniques de l'ISET Radès.
Projet d'intégration L3 DSI — DidouSoft.

## Ouvrir le projet

1. Décompresser le zip.
2. Android Studio : **File > Open** et choisir le dossier `ISETCare`.
3. Attendre la fin du *Gradle Sync* (connexion Internet nécessaire la première fois).
4. Lancer sur un émulateur ou un téléphone (Android 8.0 / API 26 minimum).

Si Android Studio propose de mettre à jour le plugin Gradle (AGP), vous pouvez accepter.

## Comptes de démonstration

| Rôle | E-mail | Mot de passe |
|---|---|---|
| Professeur | sami.benali@isetrades.tn | prof123 |
| Personnel | leila.trabelsi@isetrades.tn | pers123 |
| Technicien | amine.gharbi@isetrades.tn | tech123 |
| Technicien | karim.mansour@isetrades.tn | tech123 |
| Administrateur | admin@isetrades.tn | admin123 |

Sur l'écran de connexion, toucher un compte remplit les champs.

## Scénario de démonstration

1. **Professeur** : Scanner > saisir `PC-031` (ou scanner son QR Code) > Ouvrir la fiche >
   Signaler une panne > l'IA propose la catégorie et la priorité.
2. **Administrateur** : Suivi > ouvrir la réclamation > Affecter un technicien.
3. **Technicien (Amine)** : Suivi > ouvrir la mission > Saisir mon intervention > Enregistrer.
4. **Professeur** : la cloche affiche les notifications, la réclamation est « Résolu ».
5. Ouvrir la fiche `PRN-001` : l'IA recommande de **remplacer** l'imprimante
   (850 DT de réparations pour un équipement de 700 DT).

Sur un émulateur sans caméra, utilisez la saisie manuelle du code. Chaque fiche affiche
le QR Code de l'équipement : affichez-le sur un autre écran pour tester le scan.

## Écrans (maquettes Figma M01 à M08)

| Maquette | Fichier |
|---|---|
| M01 Connexion | `ui/LoginActivity.kt` |
| M02 Scanner un QR Code | `ui/ScanFragment.kt` |
| M03 Fiche équipement | `ui/EquipementActivity.kt` |
| M04 Signaler une panne | `ui/SignalerActivity.kt` |
| M05 Ma réclamation | `ui/ReclamationActivity.kt`, `ui/SuiviFragment.kt` |
| M06 Notifications | `ui/NotificationsActivity.kt` |
| M07 Mon intervention | `ui/InterventionActivity.kt` |
| M08 Mon agenda | `ui/AgendaFragment.kt` |

## Organisation du code

- **Kotlin** (`ui/`) : activités, fragments, listes.
- **Java** (`model/`, `data/`, `ai/`, `network/`) : classes métier du diagramme de classes,
  règles métier, IA et contrat de l'API.
- `data/DataStore.java` : données et règles métier. Les données sont stockées sur le
  téléphone, l'application fonctionne donc sans serveur.
- `ai/IncidentClassifier.java` : catégorie + priorité à partir de la description (mots-clés).
- `ai/ReplacementAdvisor.java` : recommandation Réparer / Surveiller / Remplacer.
- `network/IsetCareApi.java` : endpoints de l'API REST Spring Boot (Retrofit).

## Brancher le backend Spring Boot

L'application n'appelle pas encore l'API. Quand le backend sera prêt :

1. Régler `BASE_URL` dans `network/ApiClient.java` (`10.0.2.2` = votre PC vu de l'émulateur).
2. Remplacer le corps des méthodes de `DataStore` par les appels `ApiClient.get()...` ;
   chaque méthode indique en commentaire l'endpoint correspondant.
3. Pour une URL en `http://`, ajouter `android:usesCleartextTraffic="true"` dans le manifest.
4. Remplacer `IncidentClassifier` par l'appel à `POST /api/ia/analyser` (service Python).

Pour remettre les données de démonstration à zéro : Paramètres Android > Applications >
ISETCare > Stockage > Effacer les données.
