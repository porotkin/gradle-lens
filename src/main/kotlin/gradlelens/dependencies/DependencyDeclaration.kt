package gradlelens.dependencies

import com.intellij.psi.PsiElement
import com.intellij.psi.SyntaxTraverser
import gradlelens.calleeName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.plainContent

fun findDependencyDeclaration(
    script: KtFile,
    gradlePath: String,
): PsiElement? {
    val accessor = projectAccessor(gradlePath)
    return SyntaxTraverser.psiTraverser(script)
        .firstOrNull { it.isProjectCall(gradlePath) || it.isProjectAccessor(accessor) }
}

private fun PsiElement.isProjectCall(gradlePath: String): Boolean =
    this is KtCallExpression && calleeName() == "project" && firstStringArgument() == gradlePath

private fun PsiElement.isProjectAccessor(accessor: List<String>): Boolean =
    this is KtDotQualifiedExpression && names() == accessor && !isReceiver()

private fun KtCallExpression.firstStringArgument(): String? =
    (valueArguments.firstOrNull()?.getArgumentExpression() as? KtStringTemplateExpression)?.plainContent

private fun KtExpression.isReceiver(): Boolean =
    (parent as? KtDotQualifiedExpression)?.receiverExpression == this

private fun KtExpression.names(): List<String>? =
    when (this) {
        is KtNameReferenceExpression -> listOf(getReferencedName())
        is KtDotQualifiedExpression -> {
            val selector = selectorExpression as? KtNameReferenceExpression
            if (selector == null) null else receiverExpression.names()?.plus(selector.getReferencedName())
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
