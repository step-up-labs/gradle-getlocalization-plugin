package org.zooper.gradle

import groovy.json.JsonSlurper
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

import java.nio.file.Files
import java.nio.file.StandardCopyOption

class GetLocalizationDownloadTask extends DefaultTask {
    def URL_BASE = "https://api.getlocalization.com/%s/api/translations/" // %s = Project
    def URL_DOWN = URL_BASE + "file/%s/%s/" // %s = Master File Name, Language Tag
    def URL_LIST = URL_BASE + "list/json/"

    GetLocalizationDownloadTask() {
        super()
        this.description = "Download latest translations from GetLocalization into project"
    }

    @TaskAction
    def dowloadTranslations() throws IOException {
        checkConfig()
        getTranslations().each { t ->
            logger.lifecycle(String.format("Downloading '%s', progress %s%%", t.iana_code, t.progress))
            downloadTranslation(t)
            if (project.getlocalization.fastlane == true) {
                downloadFastlaneFiles(t)
            }
        }
    }

    def downloadTranslation(t) {
        logger.lifecycle(String.format("Downloading '%s'", t.master_file))
        def prj = project.getlocalization.project
        def url = String.format(URL_DOWN, prj, t.master_file, t.iana_code)
        def File resDir = getResDir()
        def Map ianaCodes = project.getlocalization.iana_codes

        // Create temporary target file
        def File tmpFile = File.createTempFile("getlocalization.", ".xml")

        // Maps codes
        def codes = [t.iana_code]
        if (ianaCodes.containsKey(t.iana_code)) {
            codes = ianaCodes[t.iana_code].split(",")
        }

        // Dump translation
        def br = fetchURI(url)

        tmpFile.withWriter("UTF-8") { out ->
            String line
            while ((line = br.readLine()) != null) {
                out.println line.replace("...", "…")
            }
            out.print String.format("<!-- Translation '%s', progress %s%%, master '%s' -->\n",
                    t.iana_code, t.progress, t.master_file)
        }

        // For each code check target dir and copy file over
        codes.each { code ->
            def File targetDir = new File(resDir, String.format("values-%s", code.trim()))
            if (!targetDir.exists()) targetDir.mkdir()
            if (!targetDir.exists() || !targetDir.isDirectory()) {
                throw new IOException(
                        "Target translation directory cannot be created"
                )
            }

            // Copy (JAVA 7 or newer only)
            File targetFile = new File(targetDir, t.filename.replaceAll(".*/", ""))
            Files.copy(tmpFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }

        // All done
        tmpFile.delete()
    }

    def downloadFastlaneFiles(t) {
        downloadFastlaneFile(t, "full_description.txt")
        downloadFastlaneFile(t, "short_description.txt")
        downloadFastlaneFile(t, "title.txt")
    }

    def downloadFastlaneFile(t, filename) {
        logger.lifecycle(String.format("Downloading '%s'", filename))
        def prj = project.getlocalization.project
        def url = String.format(URL_DOWN, prj, filename, t.iana_code)
        def File resDir = getFastlaneDir()

        // Create temporary target file
        def File tmpFile = File.createTempFile("getlocalization.", ".txt")

        // Maps codes
        def ianaCodes = project.getlocalization.fastlane_codes
        def codes = [t.iana_code]
        if (ianaCodes.containsKey(t.iana_code)) {
            codes = ianaCodes[t.iana_code].split(",")
        }

        // Dump translation
        def br = fetchURI(url)

        tmpFile.withWriter("UTF-8") { out ->
            String line
            while ((line = br.readLine()) != null) {
                out.println line
            }
        }

        // For each code check target dir and copy file over
        codes.each { code ->
            def File targetDir = new File(resDir, code)
            if (!targetDir.exists()) {
                targetDir.mkdir()
            }
            if (!targetDir.exists() || !targetDir.isDirectory()) {
                throw new IOException(
                        "Target translation directory '" + targetDir.absolutePath + "' cannot be created"
                )
            }

            // Copy (JAVA 7 or newer only)
            File targetFile = new File(targetDir, filename)
            Files.copy(tmpFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }

        // All done
        tmpFile.delete()
    }

    def List getTranslations() {
        def result = []
        def url = String.format(URL_LIST, project.getlocalization.project)
        def json = fetchURI(url)
        def translations = new JsonSlurper().parse(json)
        //logger.lifecycle(translations.toString())
        def master = project.getlocalization.master
        translations.each { translation ->
            if (translation.progress.toInteger() > project.getlocalization.progress && (translation.master_file == master || master == null)) {
                result.add(translation)
            }
        }
        return result
    }

    def checkConfig() {
        def File resDir = getResDir()
        if (project.getlocalization.user == null
                || project.getlocalization.password == null
                || project.getlocalization.project == null) {
            throw new IllegalArgumentException(
                    "Cannot download because either user or password or project are missing"
            )
        }
    }

    def File getResDir() {
        def File resDir = (File) project.android.sourceSets.main.res.srcDirs.iterator().next()
        if (!resDir.exists() || !resDir.isDirectory()) {
            throw new IllegalArgumentException(
                    "Cannot find res directory in project"
            )
        }
        return resDir;
    }

    def File getFastlaneDir() {
        def File resDir = (File) project.file("/../fastlane/metadata/android")
        if (!resDir.exists() || !resDir.isDirectory()) {
            throw new IllegalArgumentException(
                    "Cannot find " + resDir.absolutePath + " directory"
            )
        }
        return resDir;
    }

    def fetchURI(String uri) {
        def uc = new URL(uri).openConnection()
        def String pw = project.getlocalization.user + ":" + project.getlocalization.password
        uc.setRequestProperty("Authorization", "Basic " + pw.getBytes().encodeBase64().toString());
        return new BufferedReader(new InputStreamReader((InputStream) uc.getInputStream(), "UTF-8"))
    }
}
