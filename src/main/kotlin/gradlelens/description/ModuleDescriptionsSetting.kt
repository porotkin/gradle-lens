package gradlelens.description

import com.intellij.ide.util.PropertiesComponent

enum class ModuleDescriptionsMode {
    ALL,
    SELECTED,
    NONE,
}

private const val MODE_KEY = "gradle-lens.module-descriptions.mode"
private const val LEGACY_ENABLED_KEY = "gradle-lens.module-descriptions.enabled"

var moduleDescriptionsMode: ModuleDescriptionsMode
    get() = PropertiesComponent.getInstance().run {
        val stored = getValue(MODE_KEY)
        ModuleDescriptionsMode.entries.find { it.name == stored }
            ?: if (getBoolean(LEGACY_ENABLED_KEY, true)) {
                ModuleDescriptionsMode.ALL
            } else {
                ModuleDescriptionsMode.NONE
            }
    }
    set(value) = PropertiesComponent.getInstance()
        .setValue(MODE_KEY, value.name)
