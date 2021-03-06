import chopper.Chopper
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

/**
 *
 * @author Konstantin Krivopustov
 */
class CreateMultiPageDoc extends DefaultTask {

    String docName
    String docLang
    String docTitle
    Map<String, String> properties

    @InputDirectory
    File getSrcDir() {
        return new File("${project.buildDir}/$docName/$docLang/html-single")
    }

    @InputDirectory
    File getChopperDir() {
        return new File("tools/chopper")
    }

    @InputDirectory
    File getCssDir() {
        return new File("styles")
    }

    @OutputDirectory
    File getDstDir() {
        return new File("${project.buildDir}/$docName/$docLang/html")
    }

    @TaskAction
    def createMultiPageDoc() {

        def props = ['docName': docName, 'gitBranch': getGitBranch()]
        if (project.hasProperty('canonicalVer')) {
            def url = "${project.docHome}/${docName}-${project.canonicalVer}"
            if (docLang != 'en') {
                url += "-${docLang}"
            }
            props.put('canonicalUrl', url)
        }
        if (properties)
            props.putAll(properties)

        def chopper = new Chopper(
                "${srcDir}/${docName}.html",
                dstDir.absolutePath,
                "${project.rootDir}/tools/chopper",
                docLang == 'en' ? '' : docLang,
                props
        )
        chopper.process()

        project.copy {
            from "${project.rootDir}/buildSrc/build/classes/java/main/chopper/server"
            into "${dstDir}/WEB-INF/classes/chopper/server"
        }

        project.configurations.chopper.files.each { dep ->
            project.copy {
                from dep.absolutePath
                into "${dstDir}/WEB-INF/lib"
            }
        }
    }

    private getGitBranch() {
        def branch = ""
        def proc = "git rev-parse --abbrev-ref HEAD".execute()
        proc.in.eachLine { line -> branch = line }
        proc.err.eachLine { line -> println line }
        proc.waitFor()
        branch
    }
}
