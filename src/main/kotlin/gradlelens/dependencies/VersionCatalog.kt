package gradlelens.dependencies

import org.toml.lang.psi.*
import org.toml.lang.psi.ext.TomlLiteralKind
import org.toml.lang.psi.ext.kind

fun libraryAccessors(
    catalogName: String,
    catalog: TomlFile,
    module: String,
): List<AccessorNotation> =
    catalog.children
        .asSequence()
        .filterIsInstance<TomlTable>()
        .filter { it.header.key?.text == "libraries" }
        .flatMap(TomlTable::entries)
        .filter { it.value?.libraryModule()?.isCoordinatesOf(module) == true }
        .map { AccessorNotation(listOf(catalogName) + it.key.text.split('-', '_', '.')) }
        .toList()

private fun TomlValue.libraryModule(): String? =
    when (this) {
        is TomlLiteral -> stringValue()
        is TomlInlineTable -> stringEntry("module")
            ?: listOfNotNull(stringEntry("group"), stringEntry("name"))
                .takeIf { it.size == 2 }
                ?.joinToString(":")

        else -> null
    }

private fun TomlInlineTable.stringEntry(key: String): String? =
    (entries.find { it.key.text == key }?.value as? TomlLiteral)?.stringValue()

private fun TomlLiteral.stringValue(): String? =
    (kind as? TomlLiteralKind.String)?.value
