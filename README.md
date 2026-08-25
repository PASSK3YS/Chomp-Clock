# Chomp Clock (v1.1.3)

### An offline personal fasting, food calorie, and weight tracker app.

- **Fasting Tracker**: 7-stage metabolic biomarker telemetry, custom fasting duration picker, confirmation dialog before ending fast, and live persistent silent Android notification.
- **UK Food Database & Barcode Scanner**: Resilient barcode lookups powered by Open Food Facts and an extensive UK supermarket offline catalog.
- **Weight Telemetry**: Multi-unit tracker supporting Stone & Pounds (st & lbs), Pounds (lbs), and Kilograms (kg).
- **Metabolic Projections**: Dynamic weekly weight trajectory estimation using Mifflin-St Jeor equation and waist-to-height ratio (WHtR).
- **Data Portability**: Full .JSON and .CSV export/import and backup management.

---

### Automated GitHub Actions Release & APK Builds

This repository is equipped with fully automated GitHub Actions workflows:

- **Automatic Release Builds**: When a new GitHub Release is created/published or a version tag (e.g. `v1.1.3`) is pushed, `.github/workflows/release.yml` automatically compiles both Release and Debug APKs, generates SHA256 checksums, and attaches them directly to the GitHub Release.
- **Manual Builds**: You can also trigger a build manually anytime from the **Actions** tab by selecting the *Build and Release Android APK* workflow.
- **Continuous Integration**: Every pull request and push to the `main`/`master` branch is tested and verified via `.github/workflows/ci.yml`.
