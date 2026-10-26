package gradlelens.dependencylens

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.readText
import gradlelens.dependencies.AccessorNotation
import gradlelens.dependencies.CoordinatesNotation
import gradlelens.dependencies.DependencyNotation
import gradlelens.dependencies.ProjectNotation

const val BUILD_DIRECTORY = "build"

const val FINDINGS_DIRECTORY = "$BUILD_DIRECTORY/dependency-lens"

enum class Issue(
    val title: String,
    val declarationTitle: String,
    val highlightType: ProblemHighlightType,
) {
    UNUSED("Unused", "Unused dependency", ProblemHighlightType.LIKE_UNUSED_SYMBOL),
    UNUSED_API("Unused, but exported as api", "Unused api dependency", ProblemHighlightType.WEAK_WARNING),
    IMPORTS_ONLY("Used only by imports", "Used only by imports", ProblemHighlightType.GENERIC_ERROR_OR_WARNING),
    UNDECLARED("Not declared", "Undeclared dependencies come through here", ProblemHighlightType.WEAK_WARNING),
}

class Finding(
    val issue: Issue,
    val classpath: String,
    val dependency: DependencyNotation,
    val declarations: List<DependencyNotation>,
    val message: String,
)

fun readFindings(moduleDirectory: VirtualFile): List<Finding> {
    val reports = moduleDirectory.findFileByRelativePath(FINDINGS_DIRECTORY)
        ?: return emptyList()

    return VfsUtil.collectChildrenRecursively(reports)
        .filter { it.name == "findings.tsv" }
        .flatMap { it.readText().lines().drop(1) }
        .mapNotNull(::parseFinding)
}

private fun parseFinding(line: String): Finding? {
    val columns = line.split('\t')
    if (columns.size != 5) {
        return null
    }

    val issue = Issue.entries.find { it.name == columns[0] }
        ?: return null

    return Finding(
        issue,
        columns[1],
        parseNotation(columns[2]),
        columns[3].split(',').map(::parseNotation),
        columns[4],
    )
}

private fun parseNotation(spelling: String): DependencyNotation =
    when {
        spelling.startsWith(PROJECT_PREFIX) ->
            ProjectNotation(spelling.removePrefix(PROJECT_PREFIX))

        ':' in spelling -> CoordinatesNotation(spelling)
        else -> AccessorNotation(spelling.split('.'))
    }

private const val PROJECT_PREFIX = "project "
