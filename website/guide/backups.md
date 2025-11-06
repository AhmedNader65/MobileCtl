# Backups & Recovery

MobileCtl automatically creates backups for safe operations.

## Overview

Every version bump and changelog generation creates automatic backups.

## List Backups

```bash
mobilectl version restore
mobilectl changelog restore
```

## Restore from Backup

```bash
mobilectl version restore version-backup-2024-01-15-14-30-00
mobilectl changelog restore changelog-backup-2024-01-15-14-30-00
```

## Backup Location

Backups are stored in `.mobilectl/backups/`:

```
.mobilectl/
  backups/
    version/
    changelog/
```

## Best Practices

- Keep backups in .gitignore
- Review backups before major changes
- Clean old backups periodically

See [Version Command](/reference/version) and [Changelog Command](/reference/changelog) for details.
