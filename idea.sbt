lazy val mainRunner = project.in(file("mainRunner")).dependsOn(RootProject(file("."))).settings(
    libraryDependencies := (libraryDependencies in RootProject(file("."))).value.map {
        module =>
            if (module.configurations.equals(Some("provided"))) {
                module.copy(configurations = None)
            } else {
                module
            }
    }
)