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
                """jsMainImplementation(project(":unused")) Not needed LIKE_UNUSED_SYMBOL [Remove dependency]""",
                """jsMainImplementation(project(":facade")) Comes through WEAK_WARNING []""",
                "jsMainImplementation(libs.example.lib) Only imports GENERIC_ERROR_OR_WARNING []",
                """jsMainImplementation(project(":facade")) Declare :exposed instead LIKE_UNUSED_SYMBOL []""",
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
