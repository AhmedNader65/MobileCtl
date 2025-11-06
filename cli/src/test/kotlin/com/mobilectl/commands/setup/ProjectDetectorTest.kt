package com.mobilectl.commands.setup

import kotlin.test.*
import java.io.File

/**
 * Unit tests for ProjectDetector
 *
 * Tests:
 * - Project type detection (Android, iOS, Flutter, React Native)
 * - App metadata detection (name, package, version)
 * - Build configuration detection (flavors, signing)
 * - Deployment configuration detection (credentials)
 * - Version file detection
 */
class ProjectDetectorTest {

    private lateinit var tempDir: File
    private lateinit var detector: ProjectDetector

    @BeforeTest
    fun setup() {
        tempDir = createTempDir("mobilectl-test")
        detector = ProjectDetector(tempDir.absolutePath)
    }

    @AfterTest
    fun cleanup() {
        tempDir.deleteRecursively()
    }

    // ═════════════════════════════════════════════════════════════
    // PROJECT TYPE DETECTION
    // ═════════════════════════════════════════════════════════════

    @Test
    fun testDetectProjectType_Flutter() {
        // Create Flutter project structure
        File(tempDir, "pubspec.yaml").writeText("name: my_app\nversion: 1.0.0")
        File(tempDir, "lib").mkdir()

        val projectType = detector.detectProjectType()

        assertEquals(ProjectType.FLUTTER, projectType, "Should detect Flutter project")
    }

    @Test
    fun testDetectProjectType_AndroidNative() {
        // Create Android project structure
        File(tempDir, "app").mkdir()
        File(tempDir, "app/build.gradle.kts").writeText("android { }")

        val projectType = detector.detectProjectType()

        assertEquals(ProjectType.ANDROID_NATIVE, projectType, "Should detect Android native project")
    }

    @Test
    fun testDetectProjectType_ReactNative() {
        // Create React Native project structure
        File(tempDir, "package.json").writeText("""
            {
                "name": "myapp",
                "dependencies": {
                    "react-native": "^0.72.0"
                }
            }
        """.trimIndent())

        val projectType = detector.detectProjectType()

        assertEquals(ProjectType.REACT_NATIVE, projectType, "Should detect React Native project")
    }

    @Test
    fun testDetectProjectType_UnknownProject() {
        // Empty directory
        val projectType = detector.detectProjectType()

        assertNull(projectType, "Should return null for unknown project type")
    }

    // ═════════════════════════════════════════════════════════════
    // APP NAME DETECTION
    // ═════════════════════════════════════════════════════════════

    @Test
    fun testDetectAppName_Flutter() {
        File(tempDir, "pubspec.yaml").writeText("name: my_flutter_app\nversion: 1.0.0")

        val appName = detector.detectAppName()

        assertEquals("my_flutter_app", appName)
    }

    @Test
    fun testDetectAppName_Android() {
        val resDir = File(tempDir, "app/src/main/res/values")
        resDir.mkdirs()
        File(resDir, "strings.xml").writeText("""
            <resources>
                <string name="app_name">My Android App</string>
            </resources>
        """.trimIndent())

        val appName = detector.detectAppName()

        assertEquals("My Android App", appName)
    }

    @Test
    fun testDetectAppName_FromDirectoryName() {
        // When no other sources are available, use directory name
        val projectDir = File(tempDir, "my-awesome-app")
        projectDir.mkdir()

        val projectDetector = ProjectDetector(projectDir.absolutePath)
        val appName = projectDetector.detectAppName()

        assertEquals("My Awesome App", appName, "Should capitalize directory name")
    }

    // ═════════════════════════════════════════════════════════════
    // PACKAGE NAME DETECTION
    // ═════════════════════════════════════════════════════════════

    @Test
    fun testDetectPackageName_AndroidKotlinDsl() {
        File(tempDir, "app").mkdir()
        File(tempDir, "app/build.gradle.kts").writeText("""
            android {
                applicationId = "com.example.myapp"
            }
        """.trimIndent())

        val packageName = detector.detectPackageName()

        assertEquals("com.example.myapp", packageName)
    }

    @Test
    fun testDetectPackageName_AndroidGroovy() {
        File(tempDir, "app").mkdir()
        File(tempDir, "app/build.gradle").writeText("""
            android {
                applicationId "com.example.app"
            }
        """.trimIndent())

        val packageName = detector.detectPackageName()

        assertEquals("com.example.app", packageName)
    }

    @Test
    fun testDetectPackageName_AndroidManifest() {
        val manifestDir = File(tempDir, "app/src/main")
        manifestDir.mkdirs()
        File(manifestDir, "AndroidManifest.xml").writeText("""
            <manifest package="com.example.manifest">
            </manifest>
        """.trimIndent())

        val packageName = detector.detectPackageName()

        assertEquals("com.example.manifest", packageName)
    }

    // ═════════════════════════════════════════════════════════════
    // VERSION DETECTION
    // ═════════════════════════════════════════════════════════════

    @Test
    fun testDetectVersion_Flutter() {
        File(tempDir, "pubspec.yaml").writeText("name: app\nversion: 2.1.0+5")

        val version = detector.detectVersion()

        assertEquals("2.1.0", version, "Should extract version without build number")
    }

    @Test
    fun testDetectVersion_AndroidKotlinDsl() {
        File(tempDir, "app").mkdir()
        File(tempDir, "app/build.gradle.kts").writeText("""
            android {
                versionName = "1.5.2"
                versionCode = 10
            }
        """.trimIndent())

        val version = detector.detectVersion()

        assertEquals("1.5.2", version)
    }

    @Test
    fun testDetectVersion_AndroidGroovy() {
        File(tempDir, "app").mkdir()
        File(tempDir, "app/build.gradle").writeText("""
            android {
                versionName "3.0.1"
            }
        """.trimIndent())

        val version = detector.detectVersion()

        assertEquals("3.0.1", version)
    }

    // ═════════════════════════════════════════════════════════════
    // ANDROID FLAVORS DETECTION
    // ═════════════════════════════════════════════════════════════

    @Test
    fun testDetectAndroidFlavors_KotlinDsl() {
        File(tempDir, "app").mkdir()
        File(tempDir, "app/build.gradle.kts").writeText("""
            android {
                productFlavors {
                    create("free") { }
                    create("paid") { }
                    create("premium") { }
                }
            }
        """.trimIndent())

        val flavors = detector.detectAndroidFlavors()

        assertEquals(3, flavors.size)
        assertTrue("free" in flavors)
        assertTrue("paid" in flavors)
        assertTrue("premium" in flavors)
    }

    @Test
    fun testDetectAndroidFlavors_Groovy() {
        File(tempDir, "app").mkdir()
        File(tempDir, "app/build.gradle").writeText("""
            android {
                productFlavors {
                    free {
                        dimension "tier"
                    }
                    paid {
                        dimension "tier"
                    }
                }
            }
        """.trimIndent())

        val flavors = detector.detectAndroidFlavors()

        assertEquals(2, flavors.size)
        assertTrue("free" in flavors)
        assertTrue("paid" in flavors)
    }

    @Test
    fun testDetectAndroidFlavors_NoFlavors() {
        File(tempDir, "app").mkdir()
        File(tempDir, "app/build.gradle.kts").writeText("android { }")

        val flavors = detector.detectAndroidFlavors()

        assertTrue(flavors.isEmpty(), "Should return empty list when no flavors")
    }

    // ═════════════════════════════════════════════════════════════
    // IOS PROJECT DETECTION
    // ═════════════════════════════════════════════════════════════

    @Test
    fun testDetectIosProjectPath_Workspace() {
        File(tempDir, "MyApp.xcworkspace").mkdir()

        val projectPath = detector.detectIosProjectPath()

        assertEquals("MyApp.xcworkspace", projectPath)
    }

    @Test
    fun testDetectIosProjectPath_XcodeProject() {
        File(tempDir, "MyApp.xcodeproj").mkdir()

        val projectPath = detector.detectIosProjectPath()

        assertEquals("MyApp.xcodeproj", projectPath)
    }

    @Test
    fun testDetectIosProjectPath_InIosSubdir() {
        val iosDir = File(tempDir, "ios")
        iosDir.mkdir()
        File(iosDir, "Runner.xcworkspace").mkdir()

        val projectPath = detector.detectIosProjectPath()

        assertEquals("ios/Runner.xcworkspace", projectPath)
    }

    @Test
    fun testDetectIosProjectPath_PrefersWorkspace() {
        // When both exist, prefer workspace
        File(tempDir, "MyApp.xcworkspace").mkdir()
        File(tempDir, "MyApp.xcodeproj").mkdir()

        val projectPath = detector.detectIosProjectPath()

        assertEquals("MyApp.xcworkspace", projectPath, "Should prefer .xcworkspace over .xcodeproj")
    }

    // ═════════════════════════════════════════════════════════════
    // CREDENTIALS DETECTION
    // ═════════════════════════════════════════════════════════════

    @Test
    fun testDetectFirebaseCredentials() {
        val credentialsDir = File(tempDir, "credentials")
        credentialsDir.mkdir()
        File(credentialsDir, "firebase-service-account.json").writeText("{}")

        val credentialsPath = detector.detectFirebaseCredentials()

        assertNotNull(credentialsPath)
        assertTrue(credentialsPath.contains("firebase-service-account.json"))
    }

    @Test
    fun testDetectFirebaseCredentials_AlternativeName() {
        val credentialsDir = File(tempDir, "credentials")
        credentialsDir.mkdir()
        File(credentialsDir, "firebase-adminsdk.json").writeText("{}")

        val credentialsPath = detector.detectFirebaseCredentials()

        assertNotNull(credentialsPath)
        assertTrue(credentialsPath.contains("firebase-adminsdk.json"))
    }

    @Test
    fun testDetectGoogleServicesJson() {
        val appDir = File(tempDir, "app")
        appDir.mkdir()
        File(appDir, "google-services.json").writeText("{}")

        val googleServicesPath = detector.detectGoogleServicesJson()

        assertNotNull(googleServicesPath)
        assertTrue(googleServicesPath.contains("google-services.json"))
    }

    @Test
    fun testDetectPlayConsoleCredentials() {
        val credentialsDir = File(tempDir, "credentials")
        credentialsDir.mkdir()
        File(credentialsDir, "play-console.json").writeText("{}")

        val credentialsPath = detector.detectPlayConsoleCredentials()

        assertNotNull(credentialsPath)
        assertTrue(credentialsPath.contains("play-console.json"))
    }

    @Test
    fun testDetectAppStoreConnectApiKey() {
        val credentialsDir = File(tempDir, "credentials")
        credentialsDir.mkdir()
        File(credentialsDir, "app-store-connect-api-key.json").writeText("{}")

        val apiKeyPath = detector.detectAppStoreConnectApiKey()

        assertNotNull(apiKeyPath)
        assertTrue(apiKeyPath.contains("app-store-connect-api-key.json"))
    }

    // ═════════════════════════════════════════════════════════════
    // VERSION FILES DETECTION
    // ═════════════════════════════════════════════════════════════

    @Test
    fun testDetectVersionFiles_Flutter() {
        File(tempDir, "pubspec.yaml").writeText("name: app\nversion: 1.0.0")

        val versionFiles = detector.detectVersionFiles()

        assertTrue(versionFiles.contains("pubspec.yaml"))
    }

    @Test
    fun testDetectVersionFiles_Android() {
        File(tempDir, "app").mkdir()
        File(tempDir, "app/build.gradle.kts").writeText("android { }")

        val versionFiles = detector.detectVersionFiles()

        assertTrue(versionFiles.contains("app/build.gradle.kts"))
    }

    @Test
    fun testDetectVersionFiles_ReactNative() {
        File(tempDir, "package.json").writeText("{}")

        val versionFiles = detector.detectVersionFiles()

        assertTrue(versionFiles.contains("package.json"))
    }

    @Test
    fun testDetectVersionFiles_MultipleFiles() {
        // Flutter project with both pubspec and Android build
        File(tempDir, "pubspec.yaml").writeText("name: app")
        File(tempDir, "app").mkdir()
        File(tempDir, "app/build.gradle.kts").writeText("android { }")

        val versionFiles = detector.detectVersionFiles()

        assertTrue(versionFiles.size >= 2, "Should detect multiple version files")
        assertTrue(versionFiles.contains("pubspec.yaml"))
        assertTrue(versionFiles.contains("app/build.gradle.kts"))
    }

    // ═════════════════════════════════════════════════════════════
    // EDGE CASES & ERROR HANDLING
    // ═════════════════════════════════════════════════════════════

    @Test
    fun testDetector_EmptyDirectory() {
        val emptyDir = createTempDir("empty-project")
        val emptyDetector = ProjectDetector(emptyDir.absolutePath)

        // Should not crash, return nulls/empty lists
        assertNull(emptyDetector.detectProjectType())
        assertNull(emptyDetector.detectPackageName())
        assertTrue(emptyDetector.detectAndroidFlavors().isEmpty())

        emptyDir.deleteRecursively()
    }

    @Test
    fun testDetector_InvalidDirectory() {
        val invalidDetector = ProjectDetector("/invalid/nonexistent/path")

        // Should not crash
        assertNull(invalidDetector.detectProjectType())
        assertNull(invalidDetector.detectAppName())
    }

    @Test
    fun testDetector_MalformedBuildFile() {
        File(tempDir, "app").mkdir()
        File(tempDir, "app/build.gradle.kts").writeText("this is not valid kotlin code {{{")

        // Should not crash, return empty results
        val flavors = detector.detectAndroidFlavors()
        assertTrue(flavors.isEmpty())
    }

    // ═════════════════════════════════════════════════════════════
    // CONSISTENCY TESTS
    // ═════════════════════════════════════════════════════════════

    @Test
    fun testMultipleCallsReturnSameResults() {
        // Create test project
        File(tempDir, "pubspec.yaml").writeText("name: my_app\nversion: 1.0.0")
        File(tempDir, "lib").mkdir()

        val type1 = detector.detectProjectType()
        val type2 = detector.detectProjectType()

        assertEquals(type1, type2, "Multiple calls should return consistent results")
    }

    @Test
    fun testDetectorIsStateless() {
        // Detector should not maintain state between calls
        val appName1 = detector.detectAppName()
        val appName2 = detector.detectAppName()

        assertEquals(appName1, appName2)
    }
}
