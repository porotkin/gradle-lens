package gradlelens.dependencylens

import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ex.QuickFixWrapper
import gradlelens.BuildFileTestCase

class DependencyLensInspectionTest : BuildFileTestCase() {
    fun testMarksReportedDeclarations() {
        assertEquals(
            listOf(
                """jsMainImplementation(project(":unused")) <html><b>Unused dependency</b>: <ul><li>Not needed</li></ul></html> LIKE_UNUSED_SYMBOL [Remove dependency]""",
                """jsMainImplementation(project(":facade")) <html><b>Undeclared dependencies come through here</b>: <ul><li>Comes through</li> <li>Used in &lt;opt-in&gt;</li></ul></html> WEAK_WARNING []""",
                "jsMainImplementation(libs.example.lib) <html><b>Used only by imports</b>: <ul><li>Only imports</li></ul></html> GENERIC_ERROR_OR_WARNING []",
                """jsMainImplementation(project(":facade")) <html><b>Unused dependency</b>: <ul><li>Declare :exposed instead</li></ul></html> LIKE_UNUSED_SYMBOL []""",
            ),
            reportedProblems().map { problem ->
                val fixes = problem.fixes.orEmpty().map { it.familyName }
                "${problem.psiElement.text} ${problem.descriptionTemplate} ${problem.highlightType} $fixes"
            },
        )
    }

    fun testRemovesUnusedDependency() {
        val unused = reportedProblems().first()

        myFixture.launchAction(QuickFixWrapper.wrap(unused, unused.fixes.orEmpty().single() as LocalQuickFix))

        myFixture.checkResult(
            """
            dependencies {
                jsMainImplementation(project(":facade"))
                jsMainImplementation(libs.example.lib)
            }
            """.trimIndent(),
        )
    }

    private fun reportedProblems(): List<ProblemDescriptor> {
        myFixture.addFileToProject(
            "build/dependency-lens/js/main/findings.tsv",
            """
            issue	classpath	dependency	declaration	message
            UNUSED	jsCompileClasspath	project :unused	project :unused	Not needed
            UNDECLARED	jsCompileClasspath	project :exposed	project :facade	Comes through
            IMPORTS_ONLY	jsCompileClasspath	org.example:lib	org.example:lib,libs.example.lib	Only imports
            UNUSED	jsCompileClasspath	project :facade	project :facade	Declare :exposed instead
            UNDECLARED	jsCompileClasspath	project :hidden	project :facade	Used in <opt-in>
            UNDECLARED	jsTestCompileClasspath	project :exposed	project :facade	Comes through
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
        val manager = InspectionManager.getInstance(project)

        return readFindings(script.virtualFile.parent)
            .problemsIn(script, manager, true)
    }
}
