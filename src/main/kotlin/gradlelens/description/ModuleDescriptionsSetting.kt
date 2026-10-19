package gradlelens.description

import com.intellij.ide.util.PropertiesComponent

private const val KEY = "gradle-lens.module-descriptions.enabled"

var moduleDescriptionsEnabled: Boolean
    get() = PropertiesComponent.getInstance()
        .getBoolean(KEY, true)
    set(value) = PropertiesComponent.getInstance()
        .setValue(KEY, value, true)
