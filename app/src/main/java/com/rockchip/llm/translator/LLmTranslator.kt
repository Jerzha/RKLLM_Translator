package com.rockchip.llm.translator

interface LLmTranslatorCallback {
    fun onThinking(msg: String, finished: Boolean)
    fun onResult(msg: String, finished: Boolean)
}

class LLmTranslator(modelPath: String, callback: LLmTranslatorCallback) :
    RKllm(modelPath, object : LLmCallback {
        var inThinking = false

        override fun onCallback(data: String, state: LLmCallback.State) {
            if (state == LLmCallback.State.NORMAL) {
                if (data == "<think>") {
                    inThinking = true
                    return
                } else if (data == "</think>") {
                    inThinking = false
                    callback.onThinking("", true)
                    return
                }

                if (inThinking) {
                    callback.onThinking(data, false)
                } else {
                    if (data == "\n") return
                    callback.onResult(data, false)
                }
            } else {
                callback.onThinking("", true)
                callback.onResult("", true)
            }
        }
    })

{
    fun translate(str: String, lang: String) {
        val msg = "<｜User｜>请将下一行的句子翻译成${lang},并仅将翻译结果输出。\n${str}<｜Assistant｜>"
        say(msg)
    }
}