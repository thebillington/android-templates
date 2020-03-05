<?xml version="1.0"?>
<#import "root://activities/common/kotlin_macros.ftl" as kt>
<recipe>
    <@kt.addAllKotlinDependencies />
    <dependency mavenUrl="com.android.support:support-v4:${buildApi}.+"/>
    <merge from="root/res/values/strings.xml" to="${escapeXmlAttribute(resOut)}/values/strings.xml" />

    <instantiate from="res/layout/fragment_data_binding_base.xml.ftl"
                    to="${escapeXmlAttribute(resOut)}/layout/${escapeXmlAttribute(fragmentName)}.xml" />

    <open file="${escapeXmlAttribute(resOut)}/layout/${escapeXmlAttribute(fragmentName)}.xml" />

    <instantiate from="src/app_package/DataBindingBaseFragment.kt.ftl"
                   to="${escapeXmlAttribute(srcOut)}/${className}.kt" />

    <open file="${escapeXmlAttribute(srcOut)}/${className}.kt" />
</recipe>
