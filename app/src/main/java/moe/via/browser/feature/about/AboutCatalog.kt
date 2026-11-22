package moe.via.browser.feature.about

data class ReferenceOpenSourceProject(val title: String, val license: String, val url: String)

/** Acknowledgements listed by Via 7.3.3, preserved as reference project credits. */
object AboutCatalog {
    val referenceProjects: List<ReferenceOpenSourceProject> = listOf(
        project("AndroidX - AndroidX", "androidx/androidx"),
        project("Dagger - Google", "google/dagger"),
        project("RxJava - ReactiveX", "ReactiveX/RxJava"),
        project("RxAndroid - ReactiveX", "ReactiveX/RxAndroid"),
        project("AutoDispose - Uber", "uber/AutoDispose"),
        project("OkHttp - Square", "square/okhttp"),
        project("LeakCanary - Square", "square/leakcanary/"),
        project("AndResGuard - shwenzhang", "shwenzhang/AndResGuard"),
        project("VasDolly - Tencent", "Tencent/VasDolly", "BSD 3-Clause License"),
        project("Timber - Jake Wharton", "JakeWharton/timber"),
        project("TinyPinyin - promeG", "promeG/TinyPinyin"),
        project("ZXing - ZXing", "zxing/zxing"),
        project("okhttp-digest - rburgst", "rburgst/okhttp-digest"),
        project("sardine-android - The Grizzly Labs", "thegrizzlylabs/sardine-android"),
        project("AutoUnfold - AirBashX", "AirBashX/UserScript"),
        project("readability - mozilla", "mozilla/readability"),
        project("drag-select-recyclerview - afollestad", "afollestad/drag-select-recyclerview"),
        project("Viewer.js - Chen Fengyuan", "fengyuanchen/viewerjs", "MIT License"),
        project("translate - xnx3", "xnx3/translate", "MIT License"),
        project("okhttp-eventsource - LaunchDarkly", "launchdarkly/okhttp-eventsource"),
        project("Markwon - Dimitry", "noties/Markwon")
    )

    private fun project(title: String, repository: String, license: String = "Apache License 2.0") =
        ReferenceOpenSourceProject(title, license, "https://github.com/$repository")
}
