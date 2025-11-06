package com.mobilectl.commands.setup

import com.mobilectl.config.Config

/**
 * Generates CI/CD workflow files for GitHub Actions and GitLab CI.
 */
class WorkflowGenerator {

    /**
     * Generates a GitHub Actions workflow file.
     */
    fun generateGitHubActions(config: Config): String {
        val hasAndroid = config.build?.android?.enabled == true
        val hasIos = config.build?.ios?.enabled == true
        val hasVersionManagement = config.version?.enabled == true
        val hasChangelog = config.changelog?.enabled == true

        return buildString {
            appendLine("name: Mobile App Deployment")
            appendLine()
            appendLine("on:")
            appendLine("  # Trigger on version tags")
            appendLine("  push:")
            appendLine("    tags:")
            appendLine("      - 'v*.*.*'")
            appendLine()
            appendLine("  # Manual trigger")
            appendLine("  workflow_dispatch:")
            appendLine("    inputs:")
            appendLine("      environment:")
            appendLine("        description: 'Deployment environment'")
            appendLine("        required: true")
            appendLine("        type: choice")
            appendLine("        options:")
            appendLine("          - production")
            appendLine("          - staging")
            appendLine("          - dev")
            appendLine("      bump_version:")
            appendLine("        description: 'Bump version (patch/minor/major)'")
            appendLine("        required: false")
            appendLine("        type: choice")
            appendLine("        options:")
            appendLine("          - none")
            appendLine("          - patch")
            appendLine("          - minor")
            appendLine("          - major")
            appendLine()
            appendLine("  # Test on pull requests")
            appendLine("  pull_request:")
            appendLine("    branches:")
            appendLine("      - main")
            appendLine("      - develop")
            appendLine()
            appendLine("jobs:")

            if (hasAndroid) {
                appendLine("  deploy-android:")
                appendLine("    runs-on: ubuntu-latest")
                appendLine("    if: github.event_name != 'pull_request'")
                appendLine()
                appendLine("    steps:")
                appendLine("      - name: Checkout code")
                appendLine("        uses: actions/checkout@v4")
                appendLine()
                appendLine("      - name: Set up JDK 17")
                appendLine("        uses: actions/setup-java@v4")
                appendLine("        with:")
                appendLine("          distribution: 'temurin'")
                appendLine("          java-version: '17'")
                appendLine("          cache: 'gradle'")
                appendLine()
                appendLine("      - name: Install mobilectl")
                appendLine("        run: |")
                appendLine("          # Add installation steps for mobilectl")
                appendLine("          # For example: curl -L <url> | bash")
                appendLine("          echo \"Install mobilectl here\"")
                appendLine()

                if (hasVersionManagement) {
                    appendLine("      - name: Bump version")
                    appendLine("        if: github.event.inputs.bump_version != 'none'")
                    appendLine("        run: |")
                    appendLine("          mobilectl version bump \${{ github.event.inputs.bump_version || 'patch' }}")
                    appendLine()
                }

                if (hasChangelog) {
                    appendLine("      - name: Generate changelog")
                    appendLine("        run: |")
                    appendLine("          mobilectl changelog generate")
                    appendLine()
                }

                appendLine("      - name: Build Android app")
                appendLine("        env:")
                appendLine("          ANDROID_KEY_PASSWORD: \${{ secrets.ANDROID_KEY_PASSWORD }}")
                appendLine("          ANDROID_STORE_PASSWORD: \${{ secrets.ANDROID_STORE_PASSWORD }}")
                appendLine("        run: |")
                appendLine("          mobilectl build")
                appendLine()
                appendLine("      - name: Deploy to Firebase")
                appendLine("        env:")
                appendLine("          FIREBASE_SERVICE_ACCOUNT: \${{ secrets.FIREBASE_SERVICE_ACCOUNT }}")
                appendLine("        run: |")
                appendLine("          # Save service account to file")
                appendLine("          echo \"\$FIREBASE_SERVICE_ACCOUNT\" > /tmp/firebase-sa.json")
                appendLine("          mobilectl deploy --platform android --destination firebase -y")
                appendLine()
            }

            if (hasIos) {
                appendLine("  deploy-ios:")
                appendLine("    runs-on: macos-latest")
                appendLine("    if: github.event_name != 'pull_request'")
                appendLine()
                appendLine("    steps:")
                appendLine("      - name: Checkout code")
                appendLine("        uses: actions/checkout@v4")
                appendLine()
                appendLine("      - name: Set up Xcode")
                appendLine("        uses: maxim-lobanov/setup-xcode@v1")
                appendLine("        with:")
                appendLine("          xcode-version: 'latest-stable'")
                appendLine()
                appendLine("      - name: Install mobilectl")
                appendLine("        run: |")
                appendLine("          # Add installation steps for mobilectl")
                appendLine("          echo \"Install mobilectl here\"")
                appendLine()
                appendLine("      - name: Build iOS app")
                appendLine("        run: |")
                appendLine("          mobilectl build --platform ios")
                appendLine()
                appendLine("      - name: Deploy to TestFlight")
                appendLine("        env:")
                appendLine("          APP_STORE_CONNECT_API_KEY: \${{ secrets.APP_STORE_CONNECT_API_KEY }}")
                appendLine("        run: |")
                appendLine("          mobilectl deploy --platform ios --destination testflight -y")
                appendLine()
            }

            appendLine("  test:")
            appendLine("    runs-on: ubuntu-latest")
            appendLine("    if: github.event_name == 'pull_request'")
            appendLine()
            appendLine("    steps:")
            appendLine("      - name: Checkout code")
            appendLine("        uses: actions/checkout@v4")
            appendLine()
            appendLine("      - name: Set up JDK 17")
            appendLine("        uses: actions/setup-java@v4")
            appendLine("        with:")
            appendLine("          distribution: 'temurin'")
            appendLine("          java-version: '17'")
            appendLine("          cache: 'gradle'")
            appendLine()
            appendLine("      - name: Run tests")
            appendLine("        run: |")
            appendLine("          ./gradlew test")
        }
    }

    /**
     * Generates a GitLab CI pipeline file.
     */
    fun generateGitLabCI(config: Config): String {
        val hasAndroid = config.build?.android?.enabled == true
        val hasIos = config.build?.ios?.enabled == true

        return buildString {
            appendLine("# GitLab CI Pipeline for mobilectl")
            appendLine()
            appendLine("stages:")
            appendLine("  - build")
            appendLine("  - deploy")
            appendLine()
            appendLine("variables:")
            appendLine("  GRADLE_OPTS: \"-Dorg.gradle.daemon=false\"")
            appendLine()

            if (hasAndroid) {
                appendLine("build:android:")
                appendLine("  stage: build")
                appendLine("  image: openjdk:17")
                appendLine("  script:")
                appendLine("    - echo \"Install mobilectl\"")
                appendLine("    - mobilectl build")
                appendLine("  artifacts:")
                appendLine("    paths:")
                appendLine("      - build/outputs/")
                appendLine("    expire_in: 1 week")
                appendLine("  only:")
                appendLine("    - tags")
                appendLine("    - main")
                appendLine()
                appendLine("deploy:android:")
                appendLine("  stage: deploy")
                appendLine("  image: openjdk:17")
                appendLine("  dependencies:")
                appendLine("    - build:android")
                appendLine("  script:")
                appendLine("    - mobilectl deploy --platform android -y")
                appendLine("  only:")
                appendLine("    - tags")
                appendLine()
            }

            if (hasIos) {
                appendLine("build:ios:")
                appendLine("  stage: build")
                appendLine("  tags:")
                appendLine("    - macos")
                appendLine("  script:")
                appendLine("    - echo \"Install mobilectl\"")
                appendLine("    - mobilectl build --platform ios")
                appendLine("  artifacts:")
                appendLine("    paths:")
                appendLine("      - build/outputs/")
                appendLine("    expire_in: 1 week")
                appendLine("  only:")
                appendLine("    - tags")
                appendLine("    - main")
                appendLine()
                appendLine("deploy:ios:")
                appendLine("  stage: deploy")
                appendLine("  tags:")
                appendLine("    - macos")
                appendLine("  dependencies:")
                appendLine("    - build:ios")
                appendLine("  script:")
                appendLine("    - mobilectl deploy --platform ios -y")
                appendLine("  only:")
                appendLine("    - tags")
            }
        }
    }
}
