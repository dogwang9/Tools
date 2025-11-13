package com.example.lib.mvvm

import android.os.Bundle
import android.view.LayoutInflater
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding
import com.example.lib.utils.AndroidUtils
import java.lang.reflect.ParameterizedType

abstract class BaseActivity<VB : ViewBinding> : AppCompatActivity() {

    protected lateinit var binding: VB

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // 初始化 ViewBinding
        binding = getViewBinding()
        setContentView(binding.root)

        AndroidUtils.setNavigationBarTransparent(window, true)
    }

    /**
     * 使用反射动态解析 ViewBinding
     */
    @Suppress("UNCHECKED_CAST")
    private fun getViewBinding(): VB {
        // 获取泛型中 ViewBinding 的类型
        val type = (javaClass.genericSuperclass as ParameterizedType).actualTypeArguments[0] as Class<VB>
        // 调用静态方法 inflate(LayoutInflater)
        val inflateMethod = type.getMethod("inflate", LayoutInflater::class.java)
        return inflateMethod.invoke(null, layoutInflater) as VB
    }
}