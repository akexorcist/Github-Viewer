package dev.akexorcist.githubviewer.test

import java.io.File

object TestEnvironment {
    private val env: Map<String, String> by lazy { loadEnv() }

    val testUser: String get() = env["GITHUB_TEST_USER"] ?: "akexorcist"
    val testRepo: String get() = env["GITHUB_TEST_REPO"] ?: "Github-Viewer"
    val testRepoOwner: String get() = env["GITHUB_TEST_REPO_OWNER"] ?: "akexorcist"

    // Optional GitHub personal access token. When set, increases rate limits from
    // 60 req/hour (unauthenticated) to 5000 req/hour, and search from 10 req/min
    // to 30 req/min. Set via GITHUB_TOKEN env var or integration-test/.env file.
    val githubToken: String? get() = env["GITHUB_TOKEN"]?.takeIf { it.isNotBlank() }

    private val trackedKeys = setOf(
        "GITHUB_TEST_USER",
        "GITHUB_TEST_REPO",
        "GITHUB_TEST_REPO_OWNER",
        "GITHUB_TOKEN",
    )

    private fun loadEnv(): Map<String, String> {
        val envFile = File("integration-test/.env")
        val fromFile = if (envFile.exists()) {
            envFile.readLines()
                .filter { it.contains("=") && !it.startsWith("#") }
                .associate { line ->
                    val (key, value) = line.split("=", limit = 2)
                    key.trim() to value.trim()
                }
        } else emptyMap()

        // System env variables override .env file
        return fromFile + System.getenv().filterKeys { it in trackedKeys }
    }
}
