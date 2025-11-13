package com.example.lib.mvvm

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.lang.reflect.ParameterizedType

abstract class BaseBottomSheetDialogFragment<VB : ViewBinding> :
    BottomSheetDialogFragment() {

    protected lateinit var binding: VB

    abstract fun initView()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // 初始化 ViewBinding
        binding = getViewBinding(inflater, container)
        dialog?.window?.navigationBarColor = Color.TRANSPARENT
        initView()
        return binding.root
    }

    /**
     * 通过反射获取 ViewBinding 实例
     */
    @Suppress("UNCHECKED_CAST")
    private fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?): VB {
        // 获取泛型 VB (即 ViewBinding 类) 的类型
        val type =
            (javaClass.genericSuperclass as ParameterizedType).actualTypeArguments[0] as Class<VB>
        // 调用 inflate(inflater, container, false)，生成 ViewBinding 实例
        val method = type.getMethod(
            "inflate",
            LayoutInflater::class.java,
            ViewGroup::class.java,
            Boolean::class.java
        )
        return method.invoke(null, inflater, container, false) as VB
    }
}