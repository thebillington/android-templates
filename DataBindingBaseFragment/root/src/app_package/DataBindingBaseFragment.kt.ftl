package ${escapeKotlinIdentifiers(packageName)}

<#if applicationPackage??>
import ${applicationPackage}.components.BaseFragment
import ${applicationPackage}.R
import ${applicationPackage}.databinding.${fragmentBindingName}
</#if>

class ${className} : BaseFragment() {

    private lateinit var mBinding: ${fragmentBindingName}

    init {
        layoutID = R.layout.${fragmentName}
    }

    override fun initBinding() {
        mBinding = mBindingRoot as ${fragmentBindingName}
    }

    override fun initViewModels() {

    }

    override fun initObservers() {

    }

    override fun fragmentTag(): String {
        return "${className}"
    }

}
