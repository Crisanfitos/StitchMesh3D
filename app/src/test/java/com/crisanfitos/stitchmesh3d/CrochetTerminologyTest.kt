package com.crisanfitos.stitchmesh3d

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Test de invariante de terminología oficial de crochet según la Regla 13.
 * Garantiza que el vocablo "galga" queda prohibido y se sustituye por "tensión" y "muestra de tensión".
 */
class CrochetTerminologyTest {

    @Test
    fun testNoProhibitedTermsInSourceCode() {
        val srcDir = File("src/main/java")
        if (srcDir.exists()) {
            srcDir.walkTopDown()
                .filter { it.isFile && it.extension == "kt" }
                .forEach { file ->
                    val content = file.readText().lowercase()
                    assertFalse(
                        "El archivo ${file.path} contiene el término prohibido 'galga'. Se debe usar 'tensión'.",
                        content.contains("galga")
                    )
                }
        }
    }

    @Test
    fun testStringsResourceContainsCanonicalTensionTerms() {
        val stringsFile = File("src/main/res/values/strings.xml")
        assertTrue("strings.xml debe existir", stringsFile.exists())
        val content = stringsFile.readText()

        assertTrue("Debe contener 'tension_label'", content.contains("tension_label"))
        assertTrue("Debe contener 'tension_sample_10x10'", content.contains("tension_sample_10x10"))
        assertTrue("Debe contener 'tension_calibrator_title'", content.contains("tension_calibrator_title"))
        assertFalse("strings.xml no debe contener 'galga'", content.lowercase().contains("galga"))
    }
}
