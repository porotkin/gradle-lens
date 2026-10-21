# Gradle Lens

IDEA plugin: shows the Gradle `description` of a module next to its folder in the Project view.

Mode: Project view ⋮ → Appearance → Module Descriptions → All Modules / Selected Modules / None.

Edit: select a module folder and press `Alt+Shift+E` (`⌥⇧E` on macOS), or right-click it → Edit Module Description….
It updates the description where the module declares it: `description = "…"` in its `build.gradle.kts`, or
`description=…` in its own `gradle.properties`. A new description goes to `build.gradle.kts`, or to the module's
`gradle.properties` when it has no Kotlin build script. The root project's `gradle.properties` applies to every module,
so it is never used. The Project view shows the new description right away, without waiting for a Gradle sync.

Quick Documentation (`F1` / `Ctrl+Q`) on a module folder lists the Gradle modules that depend on it; click one to open
where it declares the dependency.
