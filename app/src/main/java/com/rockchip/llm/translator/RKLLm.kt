package com.rockchip.llm.translator

interface LLmCallback {
    enum class State {
        ERROR, NORMAL, FINISH
    }
    fun onCallback(data: String, state: State)
}

open class RKllm(modelPath: String, callback: LLmCallback) {
    companion object {
        init {
            System.loadLibrary("rkllm")
        }
    }

    private var mInstance : Long
    private var mCallback : LLmCallback

    init {
        mInstance = initLLm(modelPath)
        mCallback = callback
    }

    fun destroy() {
        deinitLLm(mInstance)
        mInstance = 0
    }

    protected fun say(text: String) {
        infer(mInstance, text)
    }

    fun callbackFromNative(data: String, state: Int) {
        var s = LLmCallback.State.ERROR
        s = if (state == 0) LLmCallback.State.FINISH
        else if (state < 0) LLmCallback.State.ERROR
        else LLmCallback.State.NORMAL
        mCallback.onCallback(data, s)
    }

    private external fun initLLm(modelPath: String) : Long
    private external fun deinitLLm(handle: Long)
    private external fun infer(handle: Long, text: String)
}