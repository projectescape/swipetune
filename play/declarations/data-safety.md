# Google Play Data Safety draft

Review these answers against the final Play Console wording before submission.

## Collection and sharing

- Does the app collect or share required user data types? **No**.
- Is all user data encrypted in transit? **Not applicable; the app does not transmit user data to the developer or third parties.**
- Can users request deletion? **Not applicable to developer-held data; no account or developer-held data exists. Users can clear app storage, uninstall, or delete exported backups themselves.**

## Local access that is not developer collection

- Audio files and media metadata are read locally after Android permission is granted.
- Playlists, favorites, history, Review decisions, and preferences are stored in private local app storage.
- User-initiated portable backups are written to a destination selected through Android's system file picker.
- Android Auto Backup may transfer private app data through the backup provider configured by the user. The developer does not receive or control that data.

## SDK audit

- No advertising SDK.
- No analytics SDK.
- No crash-reporting SDK.
- No account or authentication SDK.
- No `INTERNET` permission.
