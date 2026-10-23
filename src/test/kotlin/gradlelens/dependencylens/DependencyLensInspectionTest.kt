package gradlelens.dependencylens

import gradlelens.BuildFileTestCase

class DependencyLensInspectionTest : BuildFileTestCase() {
    fun testMarksReportedDeclarations() {
        myFixture.addFileToProject(
            "build/dependency-lens/js/main/findings.tsv",
            """
            issue	classpath	dependency	declaration	message
            UNUSED	jsCompileClasspath	project :unused	project :unused	Not needed
            UNDECLARED	jsCompileClasspath	project :exposed	project :facade	Comes through
            IMPORTS_ONLY	jsCompileClasspath	org.example:lib	org.example:lib,libs.example.lib	Only imports
            UNUSED	jsCompileClasspath	project :gone	project :gone	Not declared here
            ANALYSIS_FAILED	jsCompileClasspath	project :unused	project :unused	Unknown issue
            """.trimIndent(),
        )
        val script = buildScript(
            """
            dependencies {
                jsMainImplementation(project(":unused"))
                jsMainImplementation(project(":facade"))
                jsMainImplementation(libs.example.lib)
            }
            """.trimIndent(),
        )

        assertEquals(
            listOf(
                """project(":unused") Not needed LIKE_UNUSED_SYMBOL""",
                """project(":facade") Comes through WEAK_WARNING""",
                "libs.example.lib Only imports GENERIC_ERROR_OR_WARNING",
            ),
            readFindings(script.virtualFile.parent).mapNotNull { finding ->
                finding.declarationIn(script)?.let { "${it.text} ${finding.message} ${finding.issue.highlightType}" }
            },
        )
    }
}
