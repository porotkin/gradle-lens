package gradlelens.dependencies

import com.intellij.psi.PsiElement
import com.intellij.psi.SyntaxTraverser
import gradlelens.calleeName
import gradlelens.model.GradleModule
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.plainContent
import org.toml.lang.psi.TomlFile

sealed interface DependencyNotation {
    fun matches(element: PsiElement): Boolean
}

data class ProjectNotation(val gradlePath: String) : DependencyNotation {
    private val accessor = projectAccessor(gradlePath)

    override fun matches(element: PsiElement): Boolean =
        element.isProjectCall(gradlePath) || element.isAccessor(accessor)
}

data class CoordinatesNotation(val module: String) : DependencyNotation {
    override fun matches(element: PsiElement): Boolean =
        element.isCoordinates(module)
}

data class AccessorNotation(val names: List<String>) : DependencyNotation {
    override fun matches(element: PsiElement): Boolean =
        element.isAccessor(names)
}

fun findDeclaration(
    script: KtFile,
    notation: DependencyNotation,
): PsiElement? =
    SyntaxTraverser.psiTraverser(script).firstOrNull(notation::matches)

fun findDeclaration(
    script: KtFile,
    notations: List<DependencyNotation>,
): PsiElement? =
    notations.firstNotNullOfOrNull { findDeclaration(script, it) }

fun declarationNotations(
    module: GradleModule,
    dependent: GradleModule,
    catalogs: Map<String, TomlFile>,
): List<DependencyNotation> =
    if (module.buildPath == dependent.buildPath) {
        listOf(ProjectNotation(module.path))
    } else {
        module.coordinates
            ?.let { coordinates ->
                listOf(CoordinatesNotation(coordinates)) +
                        catalogs.flatMap { (name, catalog) -> libraryAccessors(name, catalog, coordinates) }
            }
            .orEmpty()
    }

internal fun String.isCoordinatesOf(module: String): Boolean =
    this == module || startsWith("$module:")

private fun PsiElement.isProjectCall(gradlePath: String): Boolean =
    this is KtCallExpression && calleeName() == "project" && firstStringArgument() == gradlePath

private fun PsiElement.isAccessor(accessor: List<String>): Boolean =
    this is KtDotQualifiedExpression && names() == accessor && !isReceiver()

private fun PsiElement.isCoordinates(module: String): Boolean =
    this is KtStringTemplateExpression && plainContent.isCoordinatesOf(module)

private fun KtCallExpression.firstStringArgument(): String? =
    (valueArguments.firstOrNull()?.getArgumentExpression() as? KtStringTemplateExpression)
        ?.plainContent

private fun KtExpression.isReceiver(): Boolean =
    (parent as? KtDotQualifiedExpression)?.receiverExpression == this

private fun KtExpression.names(): List<String>? =
    when (this) {
        is KtNameReferenceExpression -> listOf(getReferencedName())
        is KtDotQualifiedExpression -> {
            val selector = selectorExpression as? KtNameReferenceExpression
            if (selector == null) {
                null
            } else {
                receiverExpression.names()?.plus(selector.getReferencedName())
            }
        }

        else -> null
    }

private fun projectAccessor(gradlePath: String): List<String> =
    listOf("projects") + gradlePath.splitToSequence(':')
        .filter(String::isNotEmpty)
        .map(::accessorName)
        .toList()

private fun accessorName(projectName: String): String =
    projectName.split('.', '-', '_')
        .joinToString("") { it.replaceFirstChar(Char::uppercaseChar) }
        .replaceFirstChar(Char::lowercaseChar)
