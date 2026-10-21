package gradlelens.description

import com.intellij.lang.properties.psi.PropertiesElementFactory
import com.intellij.lang.properties.psi.PropertiesFile
import com.intellij.lang.properties.psi.PropertyKeyValueFormat
import com.intellij.psi.PsiFile

class GradlePropertiesDescriptionFile(private val properties: PropertiesFile) : DescriptionFile {
    override val psiFile: PsiFile
        get() = properties.containingFile

    override fun declaresDescription(): Boolean =
        properties.findPropertyByKey(DESCRIPTION_PROPERTY) != null

    override fun write(description: String?) {
        val property = properties.findPropertyByKey(DESCRIPTION_PROPERTY)
        when {
            description == null
                -> property?.psiElement?.delete()

            property != null
                -> property.setValue(asciiPropertyValue(description), PropertyKeyValueFormat.FILE)

            else
                -> properties.addProperty(
                DESCRIPTION_PROPERTY,
                asciiPropertyValue(description),
                PropertyKeyValueFormat.FILE,
            )
        }
    }
}

private fun asciiPropertyValue(value: String): String =
    PropertiesElementFactory.escapeValue(value, '=', PropertyKeyValueFormat.MEMORY)
        .asIterable()
        .joinToString(separator = "") { if (it.code < 128) it.toString() else it.unicodeEscaped() }
