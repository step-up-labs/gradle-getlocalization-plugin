A Gradle plugin to manage Android project translations through GetLocalization.

## Basic usage
Just add the plugin to the build dependencies and provide getlocalization account info, the plugin will create a task called "downloadTranslations" to download all the translations which have more than 50% of progress (this can be changed by adding the option progress = XX to the getlocalization closure).

```gradle
buildscript {
    repositories {
        jcenter()
    }
    dependencies {
        classpath 'io.stepuplabs.getlocalization.gradle:gradle-getlocalization-plugin:1.2.2'
    }
}

apply plugin: 'android-getlocalization'
getlocalization {
    user = "yourusername"
    password = "yourpassword"
    project = "projectname"
}
```

Then you can just type "gradle downloadTranslations" or add it as a dependency to your release build with:

```gradle
release.dependsOn downloadTranslations
```

## Step Up Labs fork
Based on [oprisnik's fork](https://github.com/oprisnik/gradle-getlocalization-plugin).

- supports plurals
- keeps comments
- doesn't crash when downloading
- replaces "..." with "…"

### How to release new version
- change version in build.gradle
- run `gradlew install`
- test it locally using mavenLocal() dependency
- create new version in [Bintray](https://bintray.com/stepuplabs/gradle-getlocalization-plugin)
- upload files via UI

## IANA Codes
Android does not use the same IANA code format that Get Localization uses and sometimes you might want to map a translation to more than one language. Basic mapping provided by the plugin is the following:

```gradle 
  iana_codes = [
    "bg-BG": "BG",
    "cs-CZ": "cs",
    "pt-PT": "pt",
    "pt-BR": "pt-rBR",
    "hr-HR": "hr",
    "hu-HU": "hu",
    "ja-JP": "ja",
    "no": "no,nb",
    "sk-SK": "sk",
    "tr-TR": "tr",
    "zh-TW" : "zh-rTW",
    "zh-CN" : "zh-rCN",
  ]
```

This means that for example "bg-BG" will be copied to "values-bg" and "no" will be copied to both "values-no" and "values-nb". You can provide your custom mappings in the configuration, for example, to download translations with progress > 10% and map Brazilian Portoguese to both Brazilian Portoguese and generic Portoguese you can do this:

```gradle 
getlocalization {
    user = "yourusername"
    password = "yourpassword"
    project = "projectname"
    progress = 10
    iana_codes["pt-BR"] = "pt,pt-rBR"
}
```

## Master Files
GetLocalization allows multiple master files, in case you want to download translations referring to only one of these you can do so by specifying the master file in the getlocalization closure, so:

```gradle 
getlocalization {
    user = "yourusername"
    password = "yourpassword"
    project = "projectname"
    master = "mystrings.xml" 
```
