package com.example.downloader.business.model

class YtDlpRequest {
    private val urls: List<String>
    private val mYtDlpOptions = YtDlpOptions()
    private val customCommandList: MutableList<String> = ArrayList()

    constructor(url: String) {
        urls = listOf(url)
    }

    constructor(urls: List<String>) {
        this.urls = urls
    }

    fun addOption(option: String, argument: String): YtDlpRequest {
        mYtDlpOptions.addOption(option, argument)
        return this
    }

    fun addOption(option: String, argument: Number): YtDlpRequest {
        mYtDlpOptions.addOption(option, argument)
        return this
    }

    fun addOption(option: String): YtDlpRequest {
        mYtDlpOptions.addOption(option)
        return this
    }

    fun addCommands(commands: List<String>): YtDlpRequest {
        customCommandList.addAll(commands)
        return this
    }

    fun getOption(option: String): String? {
        return mYtDlpOptions.getArgument(option)
    }

    fun getArguments(option: String): List<String?>? {
        return mYtDlpOptions.getArguments(option)
    }

    fun hasOption(option: String): Boolean {
        return mYtDlpOptions.hasOption(option)
    }

    fun buildCommand(): List<String> {
        val commandList: MutableList<String> = ArrayList()
        commandList.addAll(mYtDlpOptions.buildOptions())
        commandList.addAll(customCommandList)
        commandList.addAll(urls)
        return commandList
    }

}