use environment variable

- in local.properties
app_home_by_provider=true
app_key_a=the key abc

- build.gradle.kts
val app_home_by_provider = gradleLocalProperties(rootDir, providers).getProperty("app_home_by_provider", "false")
val app_key_a = gradleLocalProperties(rootDir, providers).getProperty("app_key_a", "")

In - defaultConfig { 
	resValue("string", "app_home_by_provider", app_home_by_provider)
	resValue("string", "app_key_a", app_key_a)
}

- In code
val appKeyA = getString(R.string.app_key_a)
showToast(this@Dashboard, appKeyA)
val appHomeByProvider = resources.getString(R.string.app_home_by_provider).toBoolean()
showToast(this@Dashboard, "$appHomeByProvider")


ujjwal6@intellylabs.com