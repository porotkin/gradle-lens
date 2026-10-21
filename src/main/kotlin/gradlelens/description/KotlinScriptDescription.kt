package gradlelens.description

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import gradlelens.calleeName
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*

private val SCRIPT_HEADER_BLOCKS = setOf("buildscript", "plugins")

class KotlinScriptDescriptionFile(override val psiFile: KtFile) : DescriptionFile {
    override fun declaresDescription(): Boolean =
        assignment() != null

    override fun write(description: String?) {
        val block = psiFile.script?.blockExpression ?: return
        val assignment = assignment()
        val factory = KtPsiFactory(psiFile.project)
        when {
            description == null -> assignment?.deleteWithSurroundingWhitespace()
            assignment != null -> assignment.descriptionValue()
                ?.replace(factory.createExpression(kotlinStringLiteral(description)))

            else -> block.addStatement(
                factory.createScriptStatement("$DESCRIPTION_PROPERTY = ${kotlinStringLiteral(description)}") ?: return,
                initializers().lastOrNull { (it.body as? KtCallExpression)?.calleeName() in SCRIPT_HEADER_BLOCKS },
                factory,
            )
        }
    }

    private fun initializers(): List<KtScriptInitializer> =
        psiFile.script?.blockExpression?.statements.orEmpty().filterIsInstance<KtScriptInitializer>()

    private fun assignment(): KtScriptInitializer? =
        initializers().firstOrNull { it.descriptionValue() != null }
}

private fun KtScriptInitializer.descriptionValue(): KtExpression? =
    (body as? KtBinaryExpression)
        ?.takeIf { it.operationToken == KtTokens.EQ }
        ?.takeIf { (it.left as? KtNameReferenceExpression)?.getReferencedName() == DESCRIPTION_PROPERTY }
        ?.right

private fun KtPsiFactory.createScriptStatement(text: String): KtScriptInitializer? =
    createFile("statement.gradle.kts", text).script?.blockExpression?.statements?.singleOrNull() as? KtScriptInitializer

private fun KtBlockExpression.addStatement(
    statement: KtScriptInitializer,
    header: KtScriptInitializer?,
    factory: KtPsiFactory,
) {
    val first = statements.firstOrNull()
    when {
        header != null -> {
            addAfter(statement, header)
            addAfter(factory.createNewLine(2), header)
        }

        first != null -> {
            addBefore(statement, first)
            addBefore(factory.createNewLine(2), first)
        }

        else -> add(statement)
    }
}

private fun PsiElement.deleteWithSurroundingWhitespace() {
    (prevSibling as? PsiWhiteSpace ?: nextSibling as? PsiWhiteSpace)?.delete()
    delete()
}

private fun kotlinStringLiteral(text: String): String =
    text.asIterable()
        .joinToString(
            separator = "",
            prefix = "\"",
            postfix = "\"",
            transform = Char::kotlinEscaped,
        )

private fun Char.kotlinEscaped(): String =
    when {
        this == '\\' || this == '"' || this == '$' -> "\\" + this
        isISOControl() -> unicodeEscaped()
        else -> toString()
    }
