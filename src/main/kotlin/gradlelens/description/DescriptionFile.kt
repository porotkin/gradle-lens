package gradlelens.description

import com.intellij.lang.properties.psi.PropertiesFile
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.psi.KtFile

const val DESCRIPTION_PROPERTY = "description"

sealed interface DescriptionFile {
    val psiFile: PsiFile

    fun declaresDescription(): Boolean

    fun write(description: String?)
}

fun descriptionFileOf(file: PsiFile): DescriptionFile? =
    when (file) {
        is KtFile -> KotlinScriptDescriptionFile(file)
        is PropertiesFile -> GradlePropertiesDescriptionFile(file)
        else -> null
    }

fun chooseDescriptionFile(filesByPrecedence: List<DescriptionFile>): DescriptionFile? =
    filesByPrecedence.firstOrNull(DescriptionFile::declaresDescription) ?: filesByPrecedence.firstOrNull()

fun Char.unicodeEscaped(): String =
    "\\u%04x".format(code)
